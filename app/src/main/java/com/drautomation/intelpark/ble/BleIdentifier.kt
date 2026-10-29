package com.drautomation.intelpark.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.drautomation.intelpark.data.AppJson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

class BleException(val code: String) : Exception(code)

/**
 * Identifies the driver to the nearest Intel Park cabinet:
 * scan (service UUID) -> connect -> MTU -> read INFO -> read CHALLENGE -> sign -> write IDENTIFY frames
 * -> wait for RESULT. No pairing: nothing secret travels, the proof only works for this challenge.
 */
class BleIdentifier(private val context: Context) {

    data class Cabinet(val device: BluetoothDevice, val rssi: Int, val name: String?)
    data class Outcome(val result: ResultMsg, val controller: String, val site: String?, val rssi: Int)

    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    val isSupported: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    /** Runtime permissions this Android version needs for scanning and connecting. */
    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Full identification. [bayId]: the bay the driver is on, when the app knows it (QR code, booking). */
    suspend fun identify(token: String, bayId: String?, sign: (ByteArray) -> String): Outcome = withContext(Dispatchers.IO) {
        if (!isSupported) throw BleException("ble_unsupported")
        if (!isEnabled) throw BleException("bluetooth_off")
        if (!hasPermissions()) throw BleException("permission_denied")
        val cabinet = scan().firstOrNull() ?: throw BleException("no_cabinet")
        withTimeout(30_000) { exchange(cabinet, token, bayId, sign) }
    }

    @SuppressLint("MissingPermission")
    suspend fun scan(timeoutMs: Long = 8_000): List<Cabinet> {
        val scanner = adapter?.bluetoothLeScanner ?: throw BleException("bluetooth_off")
        val found = ConcurrentHashMap<String, Cabinet>()
        var failure: Int? = null
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                found[result.device.address] = Cabinet(result.device, result.rssi, result.scanRecord?.deviceName)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                failure = errorCode
            }
        }
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BleProtocol.SERVICE)).build())
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(filters, settings, callback)
        try {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < timeoutMs) {
                delay(250)
                if (failure != null) throw BleException("scan_failed")
                // a cabinet found: listen 1.5 s more to compare signal strengths, then stop
                if (found.isNotEmpty() && System.currentTimeMillis() - start > 1_500) break
            }
        } finally {
            runCatching { scanner.stopScan(callback) }
        }
        return found.values.sortedByDescending { it.rssi }
    }

    @SuppressLint("MissingPermission")
    private suspend fun exchange(cabinet: Cabinet, token: String, bayId: String?, sign: (ByteArray) -> String): Outcome {
        val session = GattSession()
        val gatt = cabinet.device.connectGatt(context, false, session, BluetoothDevice.TRANSPORT_LE)
            ?: throw BleException("connect_failed")
        try {
            session.connected.await()
            val mtu = session.requestMtu(gatt, BleProtocol.WANTED_MTU)
            if (!session.discover(gatt)) throw BleException("not_intelpark")
            val service = gatt.getService(BleProtocol.SERVICE) ?: throw BleException("not_intelpark")
            fun chrc(uuid: java.util.UUID) = service.getCharacteristic(uuid) ?: throw BleException("not_intelpark")

            val info = AppJson.decodeFromString(InfoMsg.serializer(), session.read(gatt, chrc(BleProtocol.INFO)).decodeToString())
            val resultChrc = chrc(BleProtocol.RESULT)
            session.enableNotifications(gatt, resultChrc)
            val challenge = AppJson.decodeFromString(
                ChallengeMsg.serializer(), session.read(gatt, chrc(BleProtocol.CHALLENGE)).decodeToString(),
            )
            val proof = sign(BleProtocol.proofMessage(challenge.ctrl, challenge.ch, bayId))
            val message = AppJson.encodeToString(IdentifyMsg.serializer(), IdentifyMsg(token, proof, bayId.orEmpty(), cabinet.rssi))
            val identify = chrc(BleProtocol.IDENTIFY)
            for (frame in BleProtocol.frames(message.encodeToByteArray(), mtu)) {
                session.write(gatt, identify, frame)
            }
            // The cabinet notifies when the decision is ready (it may ask the server first); read it.
            repeat(15) {
                withTimeoutOrNull(1_000) { session.notifications.receive() }
                val raw = session.read(gatt, resultChrc).decodeToString()
                val result = runCatching { AppJson.decodeFromString(ResultMsg.serializer(), raw) }.getOrNull()
                if (result != null && result.status != "waiting" && (result.ch == null || result.ch == challenge.ch)) {
                    return Outcome(result, info.ctrl.ifEmpty { challenge.ctrl }, info.site, cabinet.rssi)
                }
            }
            throw BleException("no_answer")
        } finally {
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }
    }

    /** BluetoothGattCallback -> coroutines. One GATT operation at a time (Android requirement). */
    @SuppressLint("MissingPermission")
    private class GattSession : BluetoothGattCallback() {
        val connected = CompletableDeferred<Unit>()
        val notifications = Channel<ByteArray>(Channel.CONFLATED)
        private var services: CompletableDeferred<Boolean>? = null
        private var mtu: CompletableDeferred<Int>? = null
        private var operation: CompletableDeferred<Pair<Int, ByteArray?>>? = null

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connected.complete(Unit)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                val error = BleException("disconnected")
                connected.completeExceptionally(error)
                services?.completeExceptionally(error)
                mtu?.completeExceptionally(error)
                operation?.completeExceptionally(error)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            services?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            this.mtu?.complete(if (status == BluetoothGatt.GATT_SUCCESS) mtu else 23)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            operation?.complete(status to value)
        }

        @Deprecated("Android 12 and older")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) operation?.complete(status to characteristic.value)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            operation?.complete(status to null)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            operation?.complete(status to null)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            notifications.trySend(value)
        }

        @Deprecated("Android 12 and older")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) notifications.trySend(characteristic.value ?: ByteArray(0))
        }

        suspend fun requestMtu(gatt: BluetoothGatt, wanted: Int): Int {
            val deferred = CompletableDeferred<Int>().also { mtu = it }
            if (!gatt.requestMtu(wanted)) return 23
            return withTimeoutOrNull(5_000) { deferred.await() } ?: 23
        }

        suspend fun discover(gatt: BluetoothGatt): Boolean {
            val deferred = CompletableDeferred<Boolean>().also { services = it }
            if (!gatt.discoverServices()) return false
            return withTimeout(10_000) { deferred.await() }
        }

        private suspend fun run(start: () -> Boolean): ByteArray? {
            val deferred = CompletableDeferred<Pair<Int, ByteArray?>>().also { operation = it }
            if (!start()) throw BleException("gatt_busy")
            val (status, value) = withTimeout(8_000) { deferred.await() }
            if (status != BluetoothGatt.GATT_SUCCESS) throw BleException("gatt_error_$status")
            return value
        }

        suspend fun read(gatt: BluetoothGatt, chrc: BluetoothGattCharacteristic): ByteArray =
            run { gatt.readCharacteristic(chrc) } ?: ByteArray(0)

        @Suppress("DEPRECATION")
        suspend fun write(gatt: BluetoothGatt, chrc: BluetoothGattCharacteristic, value: ByteArray) {
            run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(chrc, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
                } else {
                    chrc.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    chrc.value = value
                    gatt.writeCharacteristic(chrc)
                }
            }
        }

        @Suppress("DEPRECATION")
        suspend fun enableNotifications(gatt: BluetoothGatt, chrc: BluetoothGattCharacteristic) {
            gatt.setCharacteristicNotification(chrc, true)
            val cccd = chrc.getDescriptor(BleProtocol.CCCD) ?: return  // BlueZ adds it for "notify"
            val enable = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(cccd, enable) == BluetoothGatt.GATT_SUCCESS
                } else {
                    cccd.value = enable
                    gatt.writeDescriptor(cccd)
                }
            }
        }
    }
}

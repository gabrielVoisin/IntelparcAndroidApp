package com.drautomation.intelpark.ble

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Bluetooth protocol between the phone and the cabinet's Raspberry Pi.
 * Same values as raspberry-agent/agent/identity.py; full description in docs/BLUETOOTH.md.
 */
object BleProtocol {
    val SERVICE: UUID = UUID.fromString("6e1f0001-5a2b-4c8e-9a7d-1f2e3d4c5b6a")
    val INFO: UUID = UUID.fromString("6e1f0002-5a2b-4c8e-9a7d-1f2e3d4c5b6a")
    val CHALLENGE: UUID = UUID.fromString("6e1f0003-5a2b-4c8e-9a7d-1f2e3d4c5b6a")
    val IDENTIFY: UUID = UUID.fromString("6e1f0004-5a2b-4c8e-9a7d-1f2e3d4c5b6a")
    val RESULT: UUID = UUID.fromString("6e1f0005-5a2b-4c8e-9a7d-1f2e3d4c5b6a")
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val WANTED_MTU = 247
    private const val PROOF_PREFIX = "intelpark-ble-v1"
    private const val LAST_FRAME = 0x80

    /** Signed by the phone key: "intelpark-ble-v1|<controller>|<challenge>|<bay id or empty>". */
    fun proofMessage(controller: String, challenge: String, bayId: String?): ByteArray =
        "$PROOF_PREFIX|$controller|$challenge|${bayId.orEmpty()}".toByteArray(Charsets.UTF_8)

    /** Cut a message into frames of at most mtu - 3 bytes: [index | 0x80 on the last frame][chunk]. */
    fun frames(message: ByteArray, mtu: Int): List<ByteArray> {
        val size = maxOf(1, mtu - 3 - 1)
        val count = maxOf(1, (message.size + size - 1) / size)
        require(count <= 128) { "message too long for MTU $mtu" }
        return (0 until count).map { i ->
            val chunk = message.copyOfRange(i * size, minOf(message.size, (i + 1) * size))
            val header = (i or if (i == count - 1) LAST_FRAME else 0).toByte()
            byteArrayOf(header) + chunk
        }
    }
}

@Serializable
data class InfoMsg(val v: Int = 1, val ctrl: String = "", val site: String? = null, val online: Boolean = false)

@Serializable
data class ChallengeMsg(val v: Int = 1, val ctrl: String, val ch: String)

@Serializable
data class IdentifyMsg(val token: String, val proof: String, val bay: String, val rssi: Int? = null)

/** status: authorised, payment_required, reserved_by_other, wrong_bay, pending (server unreachable),
 *  invalid_token, expired_token, bad_proof, bad_challenge, bad_message, bad_frame, not_configured, error, waiting. */
@Serializable
data class ResultMsg(
    val v: Int = 1,
    val ch: String? = null,
    val ok: Boolean = false,
    val status: String = "waiting",
    val bay: String? = null,
    val bayId: String? = null,
    val online: Boolean? = null,
)

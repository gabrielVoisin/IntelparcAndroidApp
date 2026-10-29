package com.drautomation.intelpark.ble

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * The phone's own key (EC P-256) in the Android Keystore. The private key never leaves the phone:
 * the server writes its public key into the identity token, and the cabinet checks that the phone
 * signed its challenge with the matching private key (a copied token is useless).
 */
object DeviceKey {
    private const val ALIAS = "intelpark_ble_identity"

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized
    private fun ensure(): KeyStore {
        val ks = keyStore()
        if (!ks.containsAlias(ALIAS)) {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            generator.initialize(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            generator.generateKeyPair()
        }
        return ks
    }

    /** X.509 SubjectPublicKeyInfo, base64: what POST /api/v1/app/ble/token expects. */
    fun publicKeyBase64(): String {
        val certificate = ensure().getCertificate(ALIAS)
        return Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    /** SHA256withECDSA signature (DER), base64. */
    fun sign(data: ByteArray): String {
        val entry = ensure().getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        signature.update(data)
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }
}

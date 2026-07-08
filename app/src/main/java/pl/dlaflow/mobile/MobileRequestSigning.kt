package pl.dlaflow.mobile

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

interface MobileRequestSigner {
    fun publicKeySpkiBase64(): String
    fun sign(canonical: String): String
}

internal fun mobileRequestBodySha256(bytes: ByteArray): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }
}

internal fun mobileRequestCanonicalString(
    method: String,
    pathWithQuery: String,
    bodySha256: String,
    timestamp: String,
    nonce: String,
    deviceId: String,
): String {
    return listOf(
        "v1",
        method.uppercase(),
        pathWithQuery,
        bodySha256,
        timestamp,
        nonce,
        deviceId,
    ).joinToString("\n")
}

class AndroidKeystoreMobileRequestSigner : MobileRequestSigner {
    private val alias = "dlaflow_mobile_request_signing_v1"

    override fun publicKeySpkiBase64(): String {
        val publicKey = getOrCreateKeyPairPublicKey()

        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    override fun sign(canonical: String): String {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(getPrivateKey())
        signature.update(canonical.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(
            signature.sign(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }

    private fun getOrCreateKeyPairPublicKey(): java.security.PublicKey {
        val keyStore = keyStore()
        val certificate = keyStore.getCertificate(alias)
        if (certificate != null) {
            return certificate.publicKey
        }

        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

        keyPairGenerator.initialize(parameterSpec)

        return keyPairGenerator.generateKeyPair().public
    }

    private fun getPrivateKey(): PrivateKey {
        getOrCreateKeyPairPublicKey()

        return keyStore().getKey(alias, null) as PrivateKey
    }

    private fun keyStore(): KeyStore {
        return KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }
}

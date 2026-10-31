package com.synfusion.snipnote.data.security

import java.util.Base64
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * AES/GCM/NoPadding with a 12-byte random IV and a 128-bit authentication tag.
 *
 * Payload layout: `snipnote:v1:` + Base64(IV ‖ ciphertext‖tag). The version prefix lets future
 * releases rotate algorithms without guessing what old rows contain.
 */
class KeystoreCipherBox(
    private val keys: SecretKeyProvider = AndroidKeystoreSecretKeyProvider(),
) : CipherBox {

    override fun encrypt(plaintext: String): CryptoResult<String> = try {
        val key = keys.getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        iv.copyInto(combined, 0)
        ciphertext.copyInto(combined, iv.size)
        CryptoResult.Success(PREFIX + Base64.getEncoder().withoutPadding().encodeToString(combined))
    } catch (e: GeneralSecurityException) {
        CryptoResult.KeyUnavailable
    } catch (e: IllegalStateException) {
        CryptoResult.KeyUnavailable
    } catch (e: RuntimeException) {
        CryptoResult.Failure("encrypt-failed:${e.javaClass.simpleName}")
    }

    override fun decrypt(payload: String): CryptoResult<String> {
        if (!payload.startsWith(PREFIX)) {
            // Rows written before encryption, or a corrupted column.
            return CryptoResult.Failure("unsupported-format")
        }
        val raw = try {
            Base64.getDecoder().decode(payload.removePrefix(PREFIX))
        } catch (e: IllegalArgumentException) {
            return CryptoResult.Failure("invalid-base64")
        }
        if (raw.size <= IV_LENGTH) return CryptoResult.Failure("truncated-payload")

        val key = keys.existingKey() ?: return CryptoResult.KeyUnavailable
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = raw.copyOfRange(0, IV_LENGTH)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            val plaintext = cipher.doFinal(raw, IV_LENGTH, raw.size - IV_LENGTH)
            CryptoResult.Success(String(plaintext, Charsets.UTF_8))
        } catch (e: AEADBadTagException) {
            CryptoResult.Failure("authentication-failed")
        } catch (e: GeneralSecurityException) {
            CryptoResult.Failure("decrypt-failed:${e.javaClass.simpleName}")
        }
    }

    companion object {
        const val PREFIX = "snipnote:v1:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
    }
}

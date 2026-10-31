package com.synfusion.snipnote.data.security

/**
 * Outcome of a crypto operation. Failures are modelled explicitly so that callers can
 * distinguish "the Keystore key is gone" (data may be recoverable with a restored key)
 * from "this ciphertext is damaged" (AEAD tag mismatch).
 */
sealed interface CryptoResult<out T> {
    data class Success<T>(val value: T) : CryptoResult<T>

    /** The encryption key is not available on this device. Data must be preserved, never dropped. */
    data object KeyUnavailable : CryptoResult<Nothing>

    /** The payload is malformed, truncated or failed authentication. */
    data class Failure(val reason: String) : CryptoResult<Nothing>
}

/** Symmetric authenticated encryption used for the Safe workspace. */
interface CipherBox {
    /** Encrypts [plaintext] and returns an opaque, self-describing payload. */
    fun encrypt(plaintext: String): CryptoResult<String>

    /** Decrypts a payload previously produced by [encrypt]. */
    fun decrypt(payload: String): CryptoResult<String>
}

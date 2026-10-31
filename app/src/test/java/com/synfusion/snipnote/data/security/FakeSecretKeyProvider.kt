package com.synfusion.snipnote.data.security

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Test double for the Android Keystore. Uses a plain AES key so the real encryption and
 * authentication code paths in [KeystoreCipherBox] run on the JVM.
 */
class FakeSecretKeyProvider(
    private val keyBytes: ByteArray? = ByteArray(32) { (it * 7 + 1).toByte() },
) : SecretKeyProvider {

    override fun existingKey(): SecretKey? = keyBytes?.let { SecretKeySpec(it, "AES") }

    override fun getOrCreateKey(): SecretKey =
        existingKey() ?: throw IllegalStateException("No key available in this test")
}

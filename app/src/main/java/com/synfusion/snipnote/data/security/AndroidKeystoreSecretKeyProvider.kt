package com.synfusion.snipnote.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Supplies the app's single AES-256 key. Implemented separately so tests can inject one. */
interface SecretKeyProvider {
    /** Returns the existing key or null when it is not (or no longer) available. */
    fun existingKey(): SecretKey?

    /** Returns the existing key, generating it on first use. */
    fun getOrCreateKey(): SecretKey
}

/**
 * Android Keystore backed AES-256 key.
 *
 * * The key material never leaves the secure hardware / Keystore daemon.
 * * No key is ever hard-coded, logged, exported or included in backups.
 * * Losing the Keystore entry (factory reset, "clear credentials", backup restore onto a new
 *   device) makes existing Safe ciphertext permanently unreadable: there is no recovery path
 *   by design. The UI reports this state instead of deleting rows.
 */
class AndroidKeystoreSecretKeyProvider(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) },
) : SecretKeyProvider {

    override fun existingKey(): SecretKey? = runCatching {
        keyStore.getKey(keyAlias, null) as? SecretKey
    }.getOrNull()

    override fun getOrCreateKey(): SecretKey {
        existingKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Randomized encryption: a fresh IV per operation, no user-auth binding here because
            // the app enforces its own unlock gate (see SafeSession) and must be able to
            // re-encrypt during an import while unlocked.
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(false)
            builder.setIsStrongBoxBacked(false)
        }
        generator.init(builder.build())
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "snipnote_safe_aes_v1"
    }
}

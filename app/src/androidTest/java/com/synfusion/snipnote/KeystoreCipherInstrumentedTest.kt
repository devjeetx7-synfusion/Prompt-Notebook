package com.synfusion.snipnote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synfusion.snipnote.data.security.CryptoResult
import com.synfusion.snipnote.data.security.KeystoreCipherBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Android Keystore path. Requires a device/emulator with a working
 * keystore (standard on API 26+ AOSP images).
 */
@RunWith(AndroidJUnit4::class)
class KeystoreCipherInstrumentedTest {

    private val box = KeystoreCipherBox()

    @Test
    fun keystoreBackedRoundTripPreservesContent() {
        val plaintext = "Private prompt:  \n\tUse AES-GCM\n\n日本語 🚀\n"
        val encrypted = box.encrypt(plaintext)
        assertTrue("encryption failed: $encrypted", encrypted is CryptoResult.Success)
        val payload = (encrypted as CryptoResult.Success).value
        assertTrue(payload.startsWith(KeystoreCipherBox.PREFIX))
        val decrypted = box.decrypt(payload)
        assertEquals(plaintext, (decrypted as CryptoResult.Success).value)
    }

    @Test
    fun twoEncryptionsOfTheSamePlaintextDiffer() {
        val a = (box.encrypt("same") as CryptoResult.Success).value
        val b = (box.encrypt("same") as CryptoResult.Success).value
        assertNotEquals(a, b)
    }

    @Test
    fun corruptedPayloadDoesNotDecrypt() {
        val payload = (box.encrypt("payload") as CryptoResult.Success).value
        val corrupted = payload.dropLast(4) + "AAAA"
        assertTrue(box.decrypt(corrupted) is CryptoResult.Failure)
    }
}

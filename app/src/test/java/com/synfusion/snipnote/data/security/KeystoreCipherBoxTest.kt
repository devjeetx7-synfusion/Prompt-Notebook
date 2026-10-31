package com.synfusion.snipnote.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class KeystoreCipherBoxTest {

    private val box = KeystoreCipherBox(FakeSecretKeyProvider())

    @Test
    fun `round trip preserves unicode newlines indentation and whitespace`() {
        val plaintext = "  Line one\n\tTabbed\n\nUnicode: 日本語 🚀\nTrailing spaces   \n"
        val encrypted = box.encrypt(plaintext)
        assertTrue(encrypted is CryptoResult.Success)
        val decrypted = box.decrypt((encrypted as CryptoResult.Success).value)
        assertTrue(decrypted is CryptoResult.Success)
        assertTrue(plaintext == (decrypted as CryptoResult.Success).value)
    }

    @Test
    fun `encryption is randomised and never returns plaintext`() {
        val plaintext = "secret value"
        val first = (box.encrypt(plaintext) as CryptoResult.Success).value
        val second = (box.encrypt(plaintext) as CryptoResult.Success).value
        assertNotEquals(first, second)
        assertFalse(first.contains(plaintext))
        assertTrue(first.startsWith(KeystoreCipherBox.PREFIX))
    }

    @Test
    fun `empty string round trips`() {
        val encrypted = (box.encrypt("") as CryptoResult.Success).value
        assertEquals("", (box.decrypt(encrypted) as CryptoResult.Success).value)
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val encrypted = (box.encrypt("important content") as CryptoResult.Success).value
        val raw = Base64.getDecoder().decode(encrypted.removePrefix(KeystoreCipherBox.PREFIX))
        val tampered = raw.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1] + 1).toByte()
        val payload = KeystoreCipherBox.PREFIX + Base64.getEncoder().withoutPadding().encodeToString(tampered)
        val result = box.decrypt(payload)
        assertTrue(result is CryptoResult.Failure)
        assertFalse(result is CryptoResult.Success)
    }

    @Test
    fun `a different key cannot decrypt ciphertext`() {
        val encrypted = (box.encrypt("bound to one key") as CryptoResult.Success).value
        val otherBox = KeystoreCipherBox(FakeSecretKeyProvider(ByteArray(32) { (it * 3 + 5).toByte() }))
        val result = otherBox.decrypt(encrypted)
        assertTrue(result is CryptoResult.Failure)
    }

    @Test
    fun `missing key is reported distinctly from corruption`() {
        val encrypted = (box.encrypt("key is gone") as CryptoResult.Success).value
        val noKeyBox = KeystoreCipherBox(FakeSecretKeyProvider(keyBytes = null))
        assertTrue(noKeyBox.decrypt(encrypted) is CryptoResult.KeyUnavailable)
        assertTrue(noKeyBox.encrypt("new content") is CryptoResult.KeyUnavailable)
    }

    @Test
    fun `malformed payloads are rejected without throwing`() {
        listOf(
            "",
            "not-a-payload",
            "snipnote:v1:",
            "snipnote:v1:!!!!notbase64!!!!",
            KeystoreCipherBox.PREFIX + "AAAA",
        ).forEach { payload ->
            val result = box.decrypt(payload)
            assertTrue("expected failure for payload '$payload'", result is CryptoResult.Failure)
        }
    }
}

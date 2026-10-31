package com.synfusion.snipnote.util

import android.content.Context
import androidx.room.Room
import com.synfusion.snipnote.data.local.SnipNoteDatabase
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.RoomNoteRepository
import com.synfusion.snipnote.data.security.CipherBox
import com.synfusion.snipnote.data.security.CryptoResult
import com.synfusion.snipnote.data.security.SafeSession
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Deterministic AES-GCM cipher for instrumentation tests: same algorithm and payload layout as
 * the Keystore implementation, but with a fixed in-memory key so ciphertext can be inspected.
 * (The real Keystore path is covered separately in [KeystoreCipherInstrumentedTest].)
 */
class TestCipherBox(
    private val keyBytes: ByteArray = ByteArray(32) { (it * 11 + 3).toByte() },
) : CipherBox {
    override fun encrypt(plaintext: String): CryptoResult<String> = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"))
        val iv = cipher.iv
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        CryptoResult.Success(PREFIX + Base64.getEncoder().withoutPadding().encodeToString(iv + ct))
    }.getOrElse { CryptoResult.KeyUnavailable }

    override fun decrypt(payload: String): CryptoResult<String> {
        if (!payload.startsWith(PREFIX)) return CryptoResult.Failure("unsupported-format")
        val raw = runCatching { Base64.getDecoder().decode(payload.removePrefix(PREFIX)) }
            .getOrElse { return CryptoResult.Failure("invalid-base64") }
        if (raw.size <= 12) return CryptoResult.Failure("truncated-payload")
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(128, raw.copyOfRange(0, 12)),
            )
            CryptoResult.Success(String(cipher.doFinal(raw, 12, raw.size - 12), Charsets.UTF_8))
        }.getOrElse { CryptoResult.Failure("authentication-failed") }
    }

    companion object {
        const val PREFIX = "snipnote:v1:"
    }
}

class TestGraph(context: Context, private val fileName: String? = null) {
    // Explicit executors keep behaviour deterministic under runTest and make the
    // persistence test's close/reopen cycle unambiguous.
    private val queryExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val transactionExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    val database: SnipNoteDatabase = if (fileName == null) {
        Room.inMemoryDatabaseBuilder(context, SnipNoteDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(queryExecutor)
            .setTransactionExecutor(transactionExecutor)
            .build()
    } else {
        Room.databaseBuilder(context, SnipNoteDatabase::class.java, fileName)
            .allowMainThreadQueries()
            .setQueryExecutor(queryExecutor)
            .setTransactionExecutor(transactionExecutor)
            .build()
    }

    val session = SafeSession()
    val cipher: CipherBox = TestCipherBox()
    val notes = RoomNoteRepository(database, cipher, session, clock = { FIXED_NOW })
    val categories = CategoryRepository(database, clock = { FIXED_NOW })

    fun close() {
        database.close()
        queryExecutor.shutdown()
        transactionExecutor.shutdown()
    }

    companion object {
        var FIXED_NOW: Long = 1_760_000_000_000
    }
}

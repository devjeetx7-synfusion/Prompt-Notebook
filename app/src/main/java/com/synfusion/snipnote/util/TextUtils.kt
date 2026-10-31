package com.synfusion.snipnote.util

import java.security.MessageDigest

object TextUtils {

    /**
     * Escapes `%`, `_` and the escape character for use inside a SQL `LIKE` pattern that
     * declares `ESCAPE '\'`. Prevents a user typing "%" from matching every row.
     */
    fun escapeLike(raw: String): String {
        val builder = StringBuilder(raw.length + 8)
        for (char in raw) {
            when (char) {
                '\\' -> builder.append("\\\\")
                '%' -> builder.append("\\%")
                '_' -> builder.append("\\_")
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    /** Wraps a search term in wildcards for a "contains" match. */
    fun likeContains(raw: String): String = "%" + escapeLike(raw) + "%"

    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    /**
     * Stable fingerprint for import de-duplication. Only ever computed for plaintext that
     * already lives on this device; it is never written for ciphertext.
     */
    fun contentFingerprint(title: String, content: String): String =
        sha256(title.trim() + "\u0000" + content.trim())

    /** Short one-line preview that keeps the original whitespace when short enough. */
    fun preview(content: String, maxChars: Int = 180): String {
        val collapsed = content.replace("\r\n", "\n").replace("\n", " ⏎ ").trim()
        return if (collapsed.length <= maxChars) collapsed else collapsed.take(maxChars).trimEnd() + "…"
    }

    fun charCount(text: String): Int = text.codePointCount(0, text.length)
}

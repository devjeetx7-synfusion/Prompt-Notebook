package com.synfusion.snipnote.domain.detect

import com.synfusion.snipnote.domain.model.NoteType

data class TypeDetection(
    val suggestedType: NoteType,
    /** 0f..1f. Values below [ContentTypeDetector.CONFIDENCE_THRESHOLD] mean "show the hint, don't switch". */
    val confidence: Float,
    val reason: String,
) {
    val isConfident: Boolean get() = confidence >= ContentTypeDetector.CONFIDENCE_THRESHOLD
}

/**
 * Heuristic, deliberately conservative classifier.
 *
 * Detection is a *suggestion* surfaced to the user; it never overwrites a type the user picked
 * manually. All checks look for structural evidence (URL scheme, code keywords, indentation)
 * instead of guessing from a single word.
 */
object ContentTypeDetector {

    const val CONFIDENCE_THRESHOLD = 0.75f

    private val urlRegex = Regex(
        """^(https?://|www\.)[\w\-._~:/?#\[\]@!$&'()*+,;=%]+$""",
        RegexOption.IGNORE_CASE,
    )

    private val codeKeywords = listOf(
        "fun ", "val ", "var ", "class ", "object ", "interface ", "def ", "import ",
        "public ", "private ", "protected ", "return ", "const ", "let ", "function ",
        "#include", "package ", "=>", "-> {", "});", "};", "</", "/>", "SELECT ", "FROM ",
    )

    private val promptPhrases = listOf(
        "act as", "you are a", "write a", "create a", "explain", "summarize", "summarise",
        "translate", "generate", "prompt:", "please ", "as an expert", "step by step",
    )

    fun detect(text: String): TypeDetection {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return TypeDetection(NoteType.NOTE, 0f, "Empty content")

        if (urlRegex.matches(trimmed)) {
            return TypeDetection(NoteType.LINK, 0.95f, "Single link")
        }

        val codeScore = codeScore(trimmed)
        if (codeScore >= 0.5f) {
            return TypeDetection(NoteType.CODE, codeScore.coerceAtMost(0.95f), "Code-like structure")
        }

        val lower = trimmed.lowercase()
        val promptHits = promptPhrases.count { lower.contains(it) }
        if (promptHits >= 1 && trimmed.length > 20) {
            val confidence = (0.6f + 0.1f * promptHits).coerceAtMost(0.9f)
            return TypeDetection(NoteType.PROMPT, confidence, "Instruction-style wording")
        }

        return TypeDetection(NoteType.NOTE, 0.4f, "Free-form text")
    }

    /** Scores structural code signals between 0 and 1. */
    private fun codeScore(text: String): Float {
        var score = 0f
        val lines = text.lines()
        val indentedLines = lines.count { it.startsWith("    ") || it.startsWith("\t") }
        if (indentedLines >= 2) score += 0.25f

        val keywordHits = codeKeywords.count { text.contains(it) }
        if (keywordHits >= 2) score += 0.3f else if (keywordHits == 1) score += 0.15f

        if (text.contains("{") && text.contains("}")) score += 0.15f
        if (Regex("""[;{}]\s*$""", RegexOption.MULTILINE).containsMatchIn(text)) score += 0.15f
        if (Regex("""^\s*(//|#|/\*|\*)""", RegexOption.MULTILINE).containsMatchIn(text)) score += 0.1f
        if (text.contains("```")) score += 0.3f
        if (text.length > 2000 && indentedLines == 0) score -= 0.2f
        return score.coerceIn(0f, 1f)
    }
}

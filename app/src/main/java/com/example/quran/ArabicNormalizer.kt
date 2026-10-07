package com.example.quran

object ArabicNormalizer {
    // Unicode ranges for Quran diacritics / harakat / marks
    private val HARAKAT_PATTERN = Regex("[\\u0617-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u06DF-\\u06E4\\u06E7\\u06E8\\u06EA-\\u06ED]")
    private val PUNCTUATION_PATTERN = Regex("[،؛؟\\.\\,\\;\\?\\!\\(\\)\\[\\]\\{\\}\\-\\–\\—\\\"\\'«»\\s]+")

    fun normalize(rawText: String): String {
        var text = rawText
        // Strip harakat / diacritics
        text = text.replace(HARAKAT_PATTERN, "")
        // Remove Tatweel / Kashida
        text = text.replace("ـ", "")
        // Normalize Alif forms (أ, إ, آ, ٱ) -> ا
        text = text.replace(Regex("[أإآٱ]"), "ا")
        // Normalize Taa Marbuta -> ه
        text = text.replace("ة", "ه")
        // Normalize Alif Maqsura / Yaa -> ي
        text = text.replace("ى", "ي")
        // Normalize Persian / Urdu letters to Arabic equivalents where needed
        text = text.replace("ك", "ك").replace("گ", "ك")
        // Clean multiple spaces and trim
        text = text.replace(PUNCTUATION_PATTERN, " ").trim()
        return text
    }

    /**
     * Compute a similarity score between 0.0 and 1.0 using token matching & Levenshtein
     */
    fun similarity(recognizedText: String, ayahText: String): Float {
        val normRec = normalize(recognizedText).lowercase()
        val normAyah = normalize(ayahText).lowercase()

        if (normRec.isEmpty() || normAyah.isEmpty()) return 0f
        if (normAyah.contains(normRec) || normRec.contains(normAyah)) return 0.95f

        val recWords = normRec.split(" ").filter { it.isNotBlank() }
        val ayahWords = normAyah.split(" ").filter { it.isNotBlank() }

        if (recWords.isEmpty() || ayahWords.isEmpty()) return 0f

        var matchedWords = 0
        for (w in recWords) {
            if (ayahWords.any { it == w || it.contains(w) || w.contains(it) }) {
                matchedWords++
            }
        }

        val wordScore = matchedWords.toFloat() / recWords.size.coerceAtLeast(1)

        // Fuzzy sub-string overlap
        val lcs = longestCommonSubstringLength(normRec, normAyah)
        val lcsScore = (lcs.toFloat() / normRec.length.coerceAtLeast(1)).coerceAtMost(1f)

        return (wordScore * 0.6f + lcsScore * 0.4f).coerceIn(0f, 1f)
    }

    private fun longestCommonSubstringLength(s1: String, s2: String): Int {
        if (s1.isEmpty() || s2.isEmpty()) return 0
        val maxLen = IntArray(s2.length + 1)
        var result = 0
        for (i in 1..s1.length) {
            for (j in s2.length downTo 1) {
                if (s1[i - 1] == s2[j - 1]) {
                    maxLen[j] = maxLen[j - 1] + 1
                    if (maxLen[j] > result) result = maxLen[j]
                } else {
                    maxLen[j] = 0
                }
            }
        }
        return result
    }
}

package com.example

import com.example.model.VideoClip
import com.example.quran.ArabicNormalizer
import com.example.quran.QuranDatabase
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testQuranDatabaseHas114Surahs() {
        val surahs = QuranDatabase.surahs
        assertEquals(114, surahs.size)
        assertEquals("Al-Fatihah", surahs[0].nameEnglish)
        assertEquals("An-Nas", surahs[113].nameEnglish)
    }

    @Test
    fun testAyahRetrieval() {
        val fatihaAyahs = QuranDatabase.getAyahsForSurah(1)
        assertTrue(fatihaAyahs.isNotEmpty())
        assertEquals(1, fatihaAyahs.first().surahNumber)
        assertTrue(fatihaAyahs.first().arabicText.contains("بِسْمِ"))
        assertTrue(fatihaAyahs.first().urduTranslation.contains("اللہ"))
    }

    @Test
    fun testArabicNormalization() {
        val withDiacritics = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val normalized = ArabicNormalizer.normalize(withDiacritics)
        assertFalse(normalized.contains("ِ")) // Kasra removed
        assertFalse(normalized.contains("َ")) // Fatha removed
        assertTrue(normalized.contains("بسم"))
    }

    @Test
    fun testArabicSimilarityMatching() {
        val recitationSample = "الحمد لله رب العالمين"
        val ayah = QuranDatabase.getAyahsForSurah(1)[1] // Al-Hamdu lillahi Rabbil 'Alamin
        val score = ArabicNormalizer.similarity(recitationSample, ayah.arabicText)
        assertTrue("Similarity score should be high for match: $score", score > 0.7f)
    }

    @Test
    fun testVideoClipDurationCalculation() {
        val clip = VideoClip(
            id = "test-1",
            timelineStartMs = 5000L,
            timelineEndMs = 15000L
        )
        assertEquals(10000L, clip.durationMs)
    }
}

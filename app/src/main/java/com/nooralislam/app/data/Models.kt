package com.nooralislam.app.data

data class SurahMeta(
    val id: Int,
    val nameAr: String,
    val ayahCount: Int,
    val revelationType: String,
    val juz: Int
)

data class AyahItem(
    val numberInSurah: Int,
    val globalNumber: Int,
    val page: Int,
    val juz: Int,
    val text: String,
    val tafsir: String,
    val surahId: Int = 1,
    val surahName: String = ""
)

data class ReciterInfo(
    val id: String,
    val nameAr: String,
    val styleAr: String,
    val countryAr: String,
    val serverBaseUrl: String
) {
    fun buildSurahUrl(surahId: Int): String {
        val padded = surahId.coerceIn(1, 114).toString().padStart(3, '0')
        return "${serverBaseUrl}${padded}.mp3"
    }
}

data class HadithEntry(
    val id: Int,
    val title: String,
    val category: String,
    val narrator: String,
    val source: String,
    val text: String,
    val lesson: String
)

data class DhikrEntry(
    val id: String,
    val category: String,
    val title: String,
    val text: String,
    val targetCount: Int,
    val virtue: String
)

data class PrayerTimesModel(
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double
)

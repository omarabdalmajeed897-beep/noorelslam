package com.nooralislam.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class QuranRepository(private val context: Context) {
    private var quranJson: JSONObject? = null
    private var recitersCache: List<ReciterInfo>? = null
    private var surahsCache: List<SurahMeta>? = null
    private var hadithsCache: List<HadithEntry>? = null
    private var adhkarCache: List<DhikrEntry>? = null

    private fun readAssetText(name: String): String {
        return context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun ensureQuranLoaded(): JSONObject {
        quranJson?.let { return it }
        val parsed = JSONObject(readAssetText("quran-offline.json"))
        quranJson = parsed
        return parsed
    }

    fun getSurahs(): List<SurahMeta> {
        surahsCache?.let { return it }
        val root = JSONObject(readAssetText("reciters.json"))
        val arr = root.getJSONArray("surahs")
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SurahMeta(
                        id = o.getInt("id"),
                        nameAr = o.getString("nameAr"),
                        ayahCount = o.getInt("ayahCount"),
                        revelationType = o.getString("revelationType"),
                        juz = o.getInt("juz")
                    )
                )
            }
        }
        surahsCache = list
        return list
    }

    fun getReciters(): List<ReciterInfo> {
        recitersCache?.let { return it }
        val root = JSONObject(readAssetText("reciters.json"))
        val arr = root.getJSONArray("reciters")
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ReciterInfo(
                        id = o.getString("id"),
                        nameAr = o.getString("nameAr"),
                        styleAr = o.getString("styleAr"),
                        countryAr = o.getString("countryAr"),
                        serverBaseUrl = o.getString("serverBaseUrl")
                    )
                )
            }
        }
        recitersCache = list
        return list
    }

    fun getSurahAyahs(surahId: Int): List<AyahItem> {
        val db = ensureQuranLoaded()
        val sName = getSurahs().firstOrNull { it.id == surahId }?.nameAr.orEmpty()
        val arr = db.optJSONArray(surahId.toString()) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    AyahItem(
                        numberInSurah = o.getInt("n"),
                        globalNumber = o.getInt("g"),
                        page = o.getInt("p"),
                        juz = o.getInt("j"),
                        text = o.getString("t"),
                        tafsir = o.optString("f", ""),
                        surahId = surahId,
                        surahName = sName
                    )
                )
            }
        }
    }

    fun getMushafPage(pageNumber: Int): List<AyahItem> {
        val safePage = pageNumber.coerceIn(1, 604)
        val db = ensureQuranLoaded()
        val surahs = getSurahs().associateBy { it.id }
        val result = mutableListOf<AyahItem>()
        for (sId in 1..114) {
            val arr = db.optJSONArray(sId.toString()) ?: continue
            val sName = surahs[sId]?.nameAr.orEmpty()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getInt("p") == safePage) {
                    result.add(
                        AyahItem(
                            numberInSurah = o.getInt("n"),
                            globalNumber = o.getInt("g"),
                            page = safePage,
                            juz = o.getInt("j"),
                            text = o.getString("t"),
                            tafsir = o.optString("f", ""),
                            surahId = sId,
                            surahName = sName
                        )
                    )
                }
            }
        }
        return result
    }

    fun searchQuran(query: String): List<AyahItem> {
        val normQuery = normalizeArabic(query)
        if (normQuery.length < 2) return emptyList()
        val db = ensureQuranLoaded()
        val surahs = getSurahs().associateBy { it.id }
        val hits = mutableListOf<AyahItem>()
        for (sId in 1..114) {
            val arr = db.optJSONArray(sId.toString()) ?: continue
            val sName = surahs[sId]?.nameAr.orEmpty()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val text = o.getString("t")
                if (normalizeArabic(text).contains(normQuery)) {
                    hits.add(
                        AyahItem(
                            numberInSurah = o.getInt("n"),
                            globalNumber = o.getInt("g"),
                            page = o.getInt("p"),
                            juz = o.getInt("j"),
                            text = text,
                            tafsir = o.optString("f", ""),
                            surahId = sId,
                            surahName = sName
                        )
                    )
                    if (hits.size >= 50) return hits
                }
            }
        }
        return hits
    }

    fun getHadiths(): List<HadithEntry> {
        hadithsCache?.let { return it }
        val arr = JSONArray(readAssetText("hadiths.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    HadithEntry(
                        id = o.getInt("id"),
                        title = o.getString("title"),
                        category = o.getString("category"),
                        narrator = o.getString("narrator"),
                        source = o.getString("source"),
                        text = o.getString("text"),
                        lesson = o.getString("lesson")
                    )
                )
            }
        }
        hadithsCache = list
        return list
    }

    fun getAdhkar(): List<DhikrEntry> {
        adhkarCache?.let { return it }
        val arr = JSONArray(readAssetText("adhkar.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    DhikrEntry(
                        id = o.getString("id"),
                        category = o.getString("category"),
                        title = o.getString("title"),
                        text = o.getString("text"),
                        targetCount = o.getInt("targetCount"),
                        virtue = o.optString("virtue", "")
                    )
                )
            }
        }
        adhkarCache = list
        return list
    }

    companion object {
        fun normalizeArabic(input: String): String {
            return input
                .replace(Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]"), "")
                .replace(Regex("[أإآٱ]"), "ا")
                .replace("ى", "ي")
                .replace("ة", "ه")
                .trim()
        }
    }
}

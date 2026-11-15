package com.nooralislam.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooralislam.app.audio.OfflineReciterManager
import com.nooralislam.app.data.DhikrEntry
import com.nooralislam.app.data.HadithEntry
import com.nooralislam.app.data.QuranRepository
import com.nooralislam.app.data.ReciterInfo
import com.nooralislam.app.data.SurahMeta
import com.nooralislam.app.utils.PrayerQiblaMath
import com.nooralislam.app.utils.TasmeeMatchResult
import com.nooralislam.app.utils.TasmeeNormalizer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = QuranRepository(applicationContext)
        val audioManager = OfflineReciterManager(applicationContext)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                NoorAlIslamApp(repo = repo, audioManager = audioManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoorAlIslamApp(repo: QuranRepository, audioManager: OfflineReciterManager) {
    val emerald = Color(0xFF0F5E4D)
    val gold = Color(0xFFB68A35)
    val parchment = Color(0xFFFAF8F4)

    var selectedTab by remember { mutableIntStateOf(0) }
    val surahs = remember { repo.getSurahs() }
    val reciters = remember { repo.getReciters() }
    val hadiths = remember { repo.getHadiths() }
    val adhkar = remember { repo.getAdhkar() }

    var activeSurahId by remember { mutableIntStateOf(1) }
    var activeReciter by remember { mutableStateOf(reciters.first()) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = emerald,
            secondary = gold,
            background = parchment,
            surface = Color.White
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("نور الإسلام", fontWeight = FontWeight.Bold, color = emerald) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = parchment)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    val items = listOf(
                        "الرئيسية" to Icons.Default.Home,
                        "القرآن" to Icons.Default.MenuBook,
                        "الأحاديث" to Icons.Default.AutoStories,
                        "الصلاة" to Icons.Default.Explore,
                        "التسميع" to Icons.Default.Mic
                    )
                    items.forEachIndexed { idx, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(parchment)
                    .padding(innerPadding)
            ) {
                playbackMsg?.let { msg ->
                    Text(
                        text = msg,
                        color = emerald,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                when (selectedTab) {
                    0 -> HomeTab(
                        tasbihInitial = 0,
                        adhkar = adhkar,
                        onOpenQuran = { selectedTab = 1 },
                        onOpenHadiths = { selectedTab = 2 }
                    )
                    1 -> QuranTab(
                        repo = repo,
                        audioManager = audioManager,
                        surahs = surahs,
                        reciters = reciters,
                        activeSurahId = activeSurahId,
                        activeReciter = activeReciter,
                        isPlaying = isPlaying,
                        onSelectSurah = { activeSurahId = it },
                        onSelectReciter = { activeReciter = it },
                        onTogglePlay = {
                            if (isPlaying) {
                                audioManager.stop()
                                isPlaying = false
                                playbackMsg = null
                            } else {
                                val surahName = surahs.firstOrNull { it.id == activeSurahId }?.nameAr ?: "المحددة"
                                playbackMsg = "جاري تشغيل سورة $surahName..."
                                audioManager.playSurah(
                                    reciter = activeReciter,
                                    surahId = activeSurahId,
                                    onPrepared = { isPlaying = true },
                                    onError = { playbackMsg = it; isPlaying = false }
                                )
                            }
                        },
                        onDownloadSurah = {
                            scope.launch {
                                playbackMsg = "جاري تنزيل السورة للاستماع بدون نت..."
                                val ok = audioManager.downloadSurah(activeReciter, activeSurahId)
                                playbackMsg = if (ok) {
                                    "تم حفظ السورة على الهاتف للعمل بدون إنترنت"
                                } else {
                                    "تعذر تنزيل الملف الصوتي — تحقق من الاتصال"
                                }
                            }
                        }
                    )
                    2 -> HadithTab(hadiths = hadiths)
                    3 -> PrayerTab()
                    4 -> TasmeeTab(repo = repo, activeSurahId = activeSurahId)
                }
            }
        }
    }
}

@Composable
fun HomeTab(
    tasbihInitial: Int,
    adhkar: List<DhikrEntry>,
    onOpenQuran: () -> Unit,
    onOpenHadiths: () -> Unit
) {
    var tasbihCount by remember { mutableIntStateOf(tasbihInitial) }
    val times = remember { PrayerQiblaMath.calculateTimes(30.0444, 31.2357, 2.0) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F5E4D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("نسخة أندرويد الأصلية (Kotlin)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "المصحف الكامل بالتفسير والأحاديث والأذكار ومواقيت الصلاة مدمجة للعمل بدون إنترنت.",
                        color = Color(0xFFE5DEC9),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenQuran, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB68A35))) {
                            Text("فتح المصحف")
                        }
                        OutlinedButton(onClick = onOpenHadiths, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                            Text("الأحاديث النبوية")
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("مواقيت الصلاة اليوم", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("الفجر: ${PrayerQiblaMath.formatTime(times.fajr)} · الظهر: ${PrayerQiblaMath.formatTime(times.dhuhr)}")
                    Text("العصر: ${PrayerQiblaMath.formatTime(times.asr)} · المغرب: ${PrayerQiblaMath.formatTime(times.maghrib)} · العشاء: ${PrayerQiblaMath.formatTime(times.isha)}")
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("السبحة الإلكترونية", fontWeight = FontWeight.Bold)
                        Text("العدد الحالي: $tasbihCount", color = Color(0xFF0F5E4D), fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { tasbihCount++ }) {
                        Text("سبّح الآن")
                    }
                }
            }
        }

        item { Text("مختارات الأذكار", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(adhkar.take(8)) { d ->
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(d.title, fontWeight = FontWeight.Bold, color = Color(0xFF0F5E4D))
                    Spacer(Modifier.height(4.dp))
                    Text(d.text, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun QuranTab(
    repo: QuranRepository,
    audioManager: OfflineReciterManager,
    surahs: List<SurahMeta>,
    reciters: List<ReciterInfo>,
    activeSurahId: Int,
    activeReciter: ReciterInfo,
    isPlaying: Boolean,
    onSelectSurah: (Int) -> Unit,
    onSelectReciter: (ReciterInfo) -> Unit,
    onTogglePlay: () -> Unit,
    onDownloadSurah: () -> Unit
) {
    val ayahs = remember(activeSurahId) { repo.getSurahAyahs(activeSurahId) }
    var searchQuery by remember { mutableStateOf("") }
    val searchHits = remember(searchQuery) { if (searchQuery.length >= 2) repo.searchQuran(searchQuery) else emptyList() }
    val isOfflineReady = audioManager.isSurahDownloaded(activeReciter.id, activeSurahId)
    val activeSurahName = surahs.firstOrNull { it.id == activeSurahId }?.nameAr ?: "الفاتحة"

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("ابحث في جميع آيات القرآن...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(surahs) { s ->
                FilterChip(
                    selected = s.id == activeSurahId,
                    onClick = { onSelectSurah(s.id) },
                    label = { Text("${s.id}. ${s.nameAr}", fontSize = 12.sp) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onTogglePlay, modifier = Modifier.weight(1f)) {
                Text(if (isPlaying) "إيقاف التلاوة" else "تشغيل: $activeSurahName")
            }
            OutlinedButton(onClick = onDownloadSurah, enabled = !isOfflineReady) {
                Text(if (isOfflineReady) "محفوظة بدون نت ✓" else "تنزيل بدون نت")
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSelectSurah((activeSurahId - 1).coerceAtLeast(1)) }) { Text("السابقة") }
            Button(onClick = { onSelectSurah((activeSurahId + 1).coerceAtMost(114)) }) { Text("التالية") }
            val nextReciterIdx = (reciters.indexOf(activeReciter).coerceAtLeast(0) + 1) % reciters.size
            OutlinedButton(onClick = { onSelectReciter(reciters[nextReciterIdx]) }) {
                Text(activeReciter.nameAr, maxLines = 1)
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (searchHits.isNotEmpty()) {
                items(searchHits) { hit ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onSelectSurah(hit.surahId); searchQuery = "" }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("سورة ${hit.surahName} · آية ${hit.numberInSurah}", color = Color(0xFF0F5E4D), fontWeight = FontWeight.Bold)
                            Text(hit.text, fontSize = 18.sp)
                        }
                    }
                }
            } else {
                items(ayahs) { ayah ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${ayah.text} ﴿${ayah.numberInSurah}﴾", fontSize = 20.sp, lineHeight = 34.sp)
                            if (ayah.tafsir.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("التفسير: ${ayah.tafsir}", fontSize = 13.sp, color = Color(0xFF5B6864))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HadithTab(hadiths: List<HadithEntry>) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, hadiths) {
        val q = query.trim()
        if (q.isEmpty()) hadiths else hadiths.filter {
            it.title.contains(q) || it.text.contains(q) || it.narrator.contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("ابحث في ${hadiths.size} حديثاً نبوياً...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { h ->
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("${h.id}. ${h.title}", fontWeight = FontWeight.Bold, color = Color(0xFF0F5E4D))
                        Text("${h.narrator} · ${h.source}", fontSize = 12.sp, color = Color(0xFF8B641E))
                        Spacer(Modifier.height(6.dp))
                        Text("«${h.text}»", fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("الفائدة: ${h.lesson}", fontSize = 12.sp, color = Color(0xFF5B6864))
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerTab() {
    val times = remember { PrayerQiblaMath.calculateTimes(30.0444, 31.2357, 2.0) }
    val qibla = remember { PrayerQiblaMath.qiblaBearing(30.0444, 31.2357) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مواقيت الصلاة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("الفجر: ${PrayerQiblaMath.formatTime(times.fajr)}")
                Text("الشروق: ${PrayerQiblaMath.formatTime(times.sunrise)}")
                Text("الظهر: ${PrayerQiblaMath.formatTime(times.dhuhr)}")
                Text("العصر: ${PrayerQiblaMath.formatTime(times.asr)}")
                Text("المغرب: ${PrayerQiblaMath.formatTime(times.maghrib)}")
                Text("العشاء: ${PrayerQiblaMath.formatTime(times.isha)}")
            }
        }
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("اتجاه القبلة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("زاوية القبلة: ${qibla.toInt()}° من الشمال الجغرافي")
            }
        }
    }
}

@Composable
fun TasmeeTab(repo: QuranRepository, activeSurahId: Int) {
    val context = LocalContext.current
    val ayahs = remember(activeSurahId) { repo.getSurahAyahs(activeSurahId) }
    val targetText = remember(ayahs) { ayahs.take(7).joinToString(" ") { it.text } }

    var spokenText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("اضغط التحدث بالمايك للتسميع المباشر بالعربية") }
    var evalResult by remember { mutableStateOf<TasmeeMatchResult?>(null) }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) {
        onDispose { recognizer.destroy() }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isRecording = true; statusText = "جاري الاستماع لتلاوتك..." }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isRecording = false }
                override fun onError(error: Int) { isRecording = false; statusText = "حاول مجدداً بصوت أوضح قرب المايك" }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    spokenText = matches?.firstOrNull().orEmpty()
                    evalResult = TasmeeNormalizer.compare(targetText, spokenText)
                    isRecording = false
                    statusText = "تم التقييم"
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) spokenText = matches.first()
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(intent)
        } else {
            statusText = "يرجى منح إذن الميكروفون من إعدادات التطبيق"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("التسميع الصوتي بالمايك (أول ٧ آيات من السورة)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(statusText, color = Color(0xFF0F5E4D), fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !isRecording) {
                Text(if (isRecording) "جاري الاستماع..." else "بدء التسميع بالمايك")
            }
            OutlinedButton(onClick = { evalResult = TasmeeNormalizer.compare(targetText, spokenText) }) {
                Text("تقييم التسميع")
            }
        }

        OutlinedTextField(
            value = spokenText,
            onValueChange = { spokenText = it },
            label = { Text("النص المسموع") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        evalResult?.let { r ->
            Card {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("نسبة المطابقة: ${r.accuracyPercent}% (${r.matchedWords} / ${r.totalWords} كلمة)", fontWeight = FontWeight.Bold)
                    if (r.missedWords.isNotEmpty()) {
                        Text("كلمات للمراجعة: ${r.missedWords.take(8).joinToString(" ، ")}", color = Color(0xFFB91C1C))
                    }
                }
            }
        }
    }
}

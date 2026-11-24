package dev.kyzenn.nivoryn.ui

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.data.*
import dev.kyzenn.nivoryn.domain.*
import dev.kyzenn.nivoryn.integration.ShareInputParser
import dev.kyzenn.nivoryn.ui.components.*
import dev.kyzenn.nivoryn.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

private val DraftSaver = Saver<CheckDraft, String>(
    save = { d -> JSONObject().put("text", d.text).put("sample", d.sampleId).put("uri", d.attachmentUri).put("name", d.filename).put("mime", d.mime).put("bytes", d.bytes).put("kind", d.kind).put("qr", d.qrPayload).put("source", d.extractionSource).toString() },
    restore = {
        val j = JSONObject(it)
        CheckDraft(j.getString("text"), j.getString("sample"), j.getString("uri"), j.getString("name"), j.getString("mime"), j.getLong("bytes"), j.getString("kind"), j.optString("qr"), j.optString("source"))
    }
)

@Composable
private fun AppNav(route: String, hi: Boolean, rail: Boolean, onNavigate: (String) -> Unit) {
    val roots = listOf("home", "check", "cases", "settings")
    val names = if (hi) listOf("होम", "जाँच", "इतिहास", "सेटिंग") else listOf("Home", "Check", "Activity", "Settings")

    @Composable
    fun item(index: Int) {
        var response by remember { mutableIntStateOf(0) }
        val section=when(route) {
            "check","review","result","samples","processing","scan"->"check"
            "cases","case"->"cases"
            "settings","resources","model","rules","privacy","accessibility","notices"->"settings"
            else->"home"
        }
        val selected=section==roots[index]
        Column(
            modifier = Modifier
                .widthIn(min = 64.dp)
                .selectable(selected, onClick = { response++; onNavigate(roots[index]) }, role = Role.Tab)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavigationGlyph(index, response, Modifier.size(22.dp))
            Text(names[index], color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            val alpha by animateFloatAsState(targetValue = if (selected) 1f else 0f, animationSpec = tween(if (LocalReducedMotion.current) 0 else 200, easing = EaseOutCustom), label = "navigation marker")
            Box(modifier = Modifier.width(24.dp).height(3.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        if (rail) Column(modifier = Modifier.width(140.dp).fillMaxHeight().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { roots.indices.forEach { item(it) } }
        else Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { roots.indices.forEach { item(it) } }
    }
}

@Composable
fun NivorynApp(launchIntent: Intent? = null) {
    val context = LocalContext.current
    val store = remember { FrontendStore(context) }
    val repo = remember { FrontendDemoRepository() }
    val requestGate = remember { AnalysisRequestGate() }
    var requestToken by remember { mutableLongStateOf(0L) }
    val historyStore = remember { CheckHistoryStore(context) }
    var savedChecks by remember { mutableStateOf(emptyList<SavedCheck>()) }
    var historyError by remember { mutableStateOf(false) }
    var analysed by remember { mutableStateOf<Pair<CheckDraft, DemoResult>?>(null) }
    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { historyStore.read() } }
            .onSuccess { savedChecks = it }.onFailure { historyError = true }
    }
    val exportScope = rememberCoroutineScope()
    var exportStatus by remember { mutableStateOf("") }
    var savedResultId by rememberSaveable { mutableStateOf("") }
    var exportText by remember { mutableStateOf("") }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val payload = exportText; exportText = ""
        if (uri != null && payload.isNotBlank()) exportScope.launch {
            exportStatus = try {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(payload.toByteArray()) } ?: error("No output stream") }
                "Result exported. The selected provider controls the exported copy."
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { "Export failed. A provider may have created an incomplete file; your local history is unchanged." }
        }
        else if (uri != null) exportStatus = "Export interrupted by app recreation. No content was written; the selected provider may have created an empty file. Retry the export."
    }
    val pageStates = rememberSaveableStateHolder()
    val shared = remember { ShareInputParser.text(launchIntent) }

    var language by rememberSaveable { mutableStateOf(store.get("language", "en")) }
    var appearance by rememberSaveable { mutableStateOf(store.get("appearance", "System")) }
    var reduced by rememberSaveable { mutableStateOf(store.get("reduced", "false").toBoolean()) }
    val systemReduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    var stack by rememberSaveable { mutableStateOf(if (shared.isNotEmpty()) "check" else if (launchIntent?.getStringExtra("workspace") == "network") "network" else "home") }
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(CheckDraft(text = shared)) }
    var customDraft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(CheckDraft(text = shared)) }
    var cases by remember { mutableStateOf(store.cases()) }
    var selectedCase by rememberSaveable { mutableStateOf("") }
    var importKind by rememberSaveable { mutableStateOf("Screenshot") }
    var dialog by rememberSaveable { mutableStateOf("") }
    var dialogText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var backwards by remember { mutableStateOf(false) }

    val route = stack.substringAfterLast('/')
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hi = language == "hi"

    fun tr(en: String, hindi: String) = if (hi) hindi else en

    fun go(next: String) {
        if (route != next) {
            if (next == "processing") requestToken = requestGate.begin() else if (route == "processing") requestGate.invalidate()
            backwards = false
            stack += "/$next"
            error = ""
        }
    }

    fun back() {
        requestGate.invalidate()
        backwards = true
        if (stack.contains('/')) stack = stack.substringBeforeLast('/') else stack = "home"
        if (stack.substringAfterLast('/') == "home" && draft.sampleId.isNotEmpty()) draft = customDraft
        error = ""
    }

    fun root(next: String) {
        requestGate.invalidate()
        backwards = false
        stack = next
        if (next in listOf("home", "check") && draft.sampleId.isNotEmpty()) draft = customDraft
        error = ""
    }

    fun notify(text: String) { scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(text) } }
    fun persist(value: List<DemoCase>) { cases = value; store.saveCases(value) }

    fun choose(sample: DemoSample) {
        if (draft.sampleId.isEmpty() && draft.canReview) customDraft = draft
        draft = CheckDraft(text = sample.text, sampleId = sample.id, kind = sample.kind)
        go("review")
    }

    fun start() { store.put("welcomed", "true"); root("check") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
              runCatching {
                val attachment = withContext(Dispatchers.IO) { ShareInputParser.attachment(context, uri, importKind) }
                val valid = when (importKind) {
                    "Screenshot", "QR" -> attachment.mime.startsWith("image/")
                    "Document" -> attachment.mime == "application/pdf" || attachment.mime.startsWith("image/")
                    "Audio" -> attachment.mime.startsWith("audio/")
                    "APK" -> attachment.filename.endsWith(".apk", true)
                    else -> false
                }
                require(valid) { "This file format is not supported for $importKind." }
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                if (draft.attachmentUri.isNotEmpty()) dev.kyzenn.nivoryn.integration.ScanFiles.release(context,draft.attachmentUri)
                draft = attachment; customDraft = draft; go("review")
              }.onFailure { error = it.message ?: "The file could not be opened. Choose another file." }
            }
        }
    }

    fun pickFile(kind: String) {
        if (kind == "APK") { go("apk"); return }
        importKind = kind
        runCatching {
            picker.launch(when (kind) {
                "Screenshot", "QR" -> arrayOf("image/*")
                "Document" -> arrayOf("application/pdf", "image/*")
                "Audio" -> arrayOf("audio/*")
                else -> arrayOf("application/vnd.android.package-archive", "application/octet-stream")
            })
        }.onFailure { error = "No file picker is available on this device." }
    }

    fun pick(kind:String) {
        if(kind=="Screenshot" || kind=="QR") { importKind=kind; go("scan") }
        else pickFile(kind)
    }

    BackHandler(stack.contains('/') || route == "welcome" || route == "splash") {
        if (dialog.isNotEmpty()) dialog = "" else back()
    }

    NivorynTheme(appearance, reduced || systemReduced, hi) {
        val reduce = LocalReducedMotion.current
        Surface(color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                val wide = maxWidth >= 760.dp
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        if (route != "splash" && route != "welcome") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (stack.contains('/')) TextAction(tr("Back", "वापस"), ::back)
                                else Row(Modifier.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    BrandMark(Modifier.size(26.dp))
                                    Text("nivoryn", fontFamily = EditorialFont, style = MaterialTheme.typography.titleLarge)
                                }
                                TextAction(if (hi) "English" else "हिन्दी", { language = if (hi) "en" else "hi"; store.put("language", language) })
                            }
                        }
                    },
                    bottomBar = {
                        AnimatedVisibility(!wide && route !in listOf("welcome", "splash"),
                            enter = fadeIn(tween(if (reduce) 0 else 180)) + slideInVertically(tween(if (reduce) 0 else 220, easing = EaseOutCustom)) { if (reduce) 0 else it / 4 },
                            exit = fadeOut(tween(if (reduce) 0 else 120))) { AppNav(route, hi, false, ::root) }
                    },
                    snackbarHost = { SnackbarHost(snack) }
                ) { insets ->
                    Row(modifier = Modifier.padding(insets).fillMaxSize()) {
                        if (wide && route !in listOf("welcome", "splash")) AppNav(route, hi, true, ::root)

                        AnimatedContent(
                            targetState = stack,
                            contentKey = { value -> when (value.substringAfterLast('/')) { "processing", "result" -> "analysis-result"; "home", "check" -> "workspace"; else -> value } },
                            modifier = Modifier.weight(1f),
                            transitionSpec = {
                                val enter = if (reduce) 0 else Motion.Enter
                                val exit = if (reduce) 0 else Motion.Exit
                                (fadeIn(tween(enter, easing = EaseOutCustom)) + slideInHorizontally(tween(enter, easing = EaseOutCustom)) { if (reduce) 0 else if (backwards) -16 else 16 }) togetherWith
                                (fadeOut(tween(exit, easing = EaseOutCustom)) + slideOutHorizontally(tween(exit, easing = EaseOutCustom)) { if (reduce) 0 else if (backwards) 12 else -12 })

                            },
                            label = "page navigation"
                        ) { current ->
                            pageStates.SaveableStateProvider(if(current.substringAfterLast('/') in listOf("home","check")) "workspace" else current) {
                                val page = current.substringAfterLast('/')
                                val scroll = rememberSaveable(current, saver = ScrollState.Saver) { ScrollState(0) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (current != stack) Modifier.clearAndSetSemantics {} else Modifier)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 1000.dp)
                                            .fillMaxWidth()
                                            .verticalScroll(scroll)
                                            .padding(horizontal = if (wide) 40.dp else 24.dp)
                                            .padding(bottom = 28.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        when (page) {
                                            "welcome" -> WelcomeScreen(hi, ::start, { start(); go("samples") })
                                            "home", "check" -> { HomeScreen(
                                                draft, hi,
                                                { draft = draft.copy(text = it.take(12000), sampleId = ""); customDraft = draft },
                                                { if (draft.canReview) go("review") else error = tr("Add a message or choose a file first.", "पहले संदेश या फ़ाइल जोड़ें।") },
                                                ::pick,
                                                { go("samples") },
                                                {
                                                    if (draft.attachmentUri.isNotEmpty()) dev.kyzenn.nivoryn.integration.ScanFiles.release(context,draft.attachmentUri)
                                                    draft = CheckDraft(); customDraft = CheckDraft()
                                                    notify(tr("Draft cleared", "ड्राफ़्ट हटाया गया"))
                                                },
                                                onKind = { kind ->
                                                    val next = draft.forInputMode(kind)
                                                    if (draft.attachmentUri.isNotEmpty() && next.attachmentUri.isEmpty()) dev.kyzenn.nivoryn.integration.ScanFiles.release(context,draft.attachmentUri)
                                                    draft = next; customDraft = next
                                                    if (page == "home") go("check")
                                                },
                                                isCheck = page == "check", onPage = ::go
                                            )
                                              if (page == "home") EngineHomeSection(savedChecks, { check ->
                                                  draft = CheckDraft(text = check.text, kind = check.kind, qrPayload = check.qrPayload, extractionSource = check.extractionSource); customDraft = draft
                                                  if (check.result != null) { savedResultId = check.id; analysed = draft to check.result; go("result") } else go("review")
                                              }, ::go)
                                            }
                                            "samples" -> SampleList(repo.samples, hi, ::choose)
                                            "review" -> {
                                              val reviewedUri = draft.attachmentUri
                                              ReviewScreen(
                                                draft, hi,
                                                { text ->
                                                    if (stack.substringAfterLast('/') == "review" && draft.attachmentUri == reviewedUri) {
                                                        draft = draft.copy(text = text.take(12000), sampleId = ""); customDraft = draft
                                                    }
                                                },
                                                {
                                                    dev.kyzenn.nivoryn.integration.ScanFiles.release(context,draft.attachmentUri)
                                                    draft = draft.copy(attachmentUri = "", filename = "", mime = "", bytes = -1, qrPayload = "", extractionSource = "")
                                                    customDraft = draft
                                                    notify(tr("Attachment removed", "फ़ाइल हटा दी गई"))
                                                },
                                                { pick(draft.kind) },
                                                { go("processing") },
                                                { go("samples") },
                                                onExtracted = { extracted ->
                                                    if (stack.substringAfterLast('/') == "review" && draft.attachmentUri == reviewedUri) {
                                                        draft = draft.copy(text = extracted.text.take(12000), qrPayload = extracted.qr.take(12000), extractionSource = extracted.source, sampleId = "")
                                                        customDraft = draft
                                                    }
                                                }
                                            )
                                            }
                                            "processing" -> {
                                              val processingDraft = draft
                                              LocalProcessingScreen(processingDraft, hi, current == stack, { result, token ->
                                                if (requestGate.accepts(token) && stack.substringAfterLast('/') == "processing" && draft == processingDraft) {
                                                    savedResultId = ""
                                                    analysed = processingDraft to result
                                                    stack = stack.substringBeforeLast('/') + "/result"
                                                }
                                              }, ::back, requestToken)
                                            }
                                            "result" -> {
                                              val restored by produceState<DemoResult?>(repo.resultFor(draft) ?: analysed?.takeIf { it.first == draft }?.second ?: savedChecks.find { it.id == savedResultId }?.result, draft, savedChecks, savedResultId) {
                                                if (savedResultId.isNotEmpty()) value = savedChecks.find { it.id == savedResultId }?.result
                                                else if (value == null) value = (context.applicationContext as dev.kyzenn.nivoryn.NivorynApplication).llama.explain(withContext(Dispatchers.IO) { AnalyseInput(IndicatorStore(context).load(), (context.applicationContext as dev.kyzenn.nivoryn.NivorynApplication).spam).analyse(draft) })
                                              }
                                              if (restored == null && savedResultId.isNotEmpty()) {
                                                  Notice("The saved assessment is loading, expired or unavailable. No new result has been substituted.")
                                                  TextAction("Review input and run a new check", { savedResultId = ""; go("review") })
                                              }
                                              else if (restored == null) Text(tr("Restoring local result…", "स्थानीय परिणाम खुल रहा है…"), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                              else ResultScreen(
                                                restored, hi,
                                                {
                                                    if (restored?.origin == ResultOrigin.AUTHORED) dialog = "save"
                                                    else scope.launch {
                                                        runCatching { withContext(Dispatchers.IO) { historyStore.save(draft.text, draft.kind, result = restored, qrPayload = draft.qrPayload, extractionSource = draft.extractionSource) } }
                                                            .onSuccess { savedChecks = it; historyError = false; notify(tr("Check saved on this device", "जाँच इस फ़ोन पर सहेजी गई")) }
                                                            .onFailure { notify(tr("Could not save securely. Existing history was kept.", "जाँच सुरक्षित सहेजी नहीं जा सकी।")) }
                                                    }
                                                },
                                                { go("samples") },
                                                ::back,
                                                draft,
                                                { draft = CheckDraft(); customDraft = CheckDraft(); root("check"); notify(tr("Input deleted", "इनपुट हटा दिया गया")) }
                                            )
                                              if (restored?.origin == ResultOrigin.LOCAL) {
                                                  Text("Export includes your original text and evidence. Choose a private destination.", style = MaterialTheme.typography.bodySmall)
                                                  if (exportStatus.isNotEmpty()) Text(exportStatus)
                                                  TextAction("Export this result", {
                                                      restored?.let { result ->
                                                          exportText = ResultCodec.export(SavedCheck("export", draft.text, draft.kind, System.currentTimeMillis(), result, draft.qrPayload, draft.extractionSource))
                                                          exportPicker.launch("nivoryn-check.json")
                                                      }
                                                  })
                                              }
                                            }
                                            "cases" -> {
                                              SavedChecksSection(savedChecks, historyError, hi, { check ->
                                                draft = CheckDraft(text = check.text, kind = check.kind, qrPayload = check.qrPayload, extractionSource = check.extractionSource); customDraft = draft
                                                  if (check.result != null) { savedResultId = check.id; analysed = draft to check.result; go("result") } else go("review")
                                              }, { id -> scope.launch {
                                                runCatching { withContext(Dispatchers.IO) { historyStore.remove(id) } }
                                                    .onSuccess { savedChecks = it }.onFailure { notify("Could not update saved checks.") }
                                              } })
                                              CasesScreen(
                                                cases, hi,
                                                { selectedCase = it; go("case") },
                                                { dialogText = ""; dialog = "create" },
                                                {
                                                    val id = "sample-story"
                                                    if (cases.none { it.id == id }) persist(cases + DemoCase(id, "Task offer · sample", listOf("task")))
                                                    selectedCase = id; go("case")
                                                }
                                            )
                                            }
                                            "case" -> cases.find { it.id == selectedCase }?.let { case ->
                                                CaseDetail(
                                                    case, repo.samples, hi,
                                                    { dialogText = case.name; dialog = "rename" },
                                                    { dialog = "delete" },
                                                    { dialog = "add" },
                                                    { id -> persist(cases.map { if (it.id == case.id) it.remove(id) else it }); notify(tr("Item removed", "संदेश हटाया गया")) },
                                                    ::choose
                                                )
                                            } ?: Notice(tr("This case has been deleted.", "यह केस हटा दिया गया है।"))
                                            "guidance" -> GuidanceScreen(hi)
                                            "settings" -> SettingsScreen(
                                                hi, appearance, reduced,
                                                { appearance = it; store.put("appearance", it) },
                                                { reduced = it; store.put("reduced", it.toString()) },
                                                { go(it) },
                                                { dialog = "clear" },
                                                { go("welcome") },
                                                { language=if(hi) "en" else "hi"; store.put("language",language) }
                                            )
                                            "scan" -> ScanScreen(importKind, { captured ->
                                                dev.kyzenn.nivoryn.integration.ScanFiles.release(context,draft.attachmentUri)
                                                draft=captured; customDraft=draft; go("review")
                                            }, ::pickFile)
                                            "rules" -> RulesScreen()
                                            "network" -> NetworkScreen { go("capture") }
                                            "capture" -> InspectionScreen(true)
                                            "resources" -> ResourcesScreen(::go)
                                            "apk" -> InspectionScreen(false)
                                            "model" -> ModelSetupScreen()
                                            "privacy", "accessibility", "notices" -> InformationScreen(page, hi)
                                        }
                                        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                    }
                                }
                            }
                        }
                    }
                }
                if (dialog.isNotEmpty()) {
                    val activeCase = cases.find { it.id == selectedCase }
                    AlertDialog(
                        onDismissRequest = { dialog = "" },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        title = { Text(when (dialog) {
                            "create-save" -> tr("Create a demo case", "डेमो केस बनाएँ")
                            "create" -> tr("Create a demo case", "डेमो केस बनाएँ")
                            "rename" -> tr("Rename case", "केस का नाम बदलें")
                            "delete" -> tr("Delete ${activeCase?.name}?", "${activeCase?.name} हटाएँ?")
                            "clear" -> tr("Clear saved checks and cases?", "सहेजे डेमो केस हटाएँ?")
                            "add" -> tr("Add an example", "उदाहरण जोड़ें")
                            else -> tr("Save example to case", "उदाहरण केस में सहेजें")
                        }) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (dialog == "save" || dialog == "add") Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()) else Modifier),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (dialog) {
                                    "create", "rename" -> FormField(dialogText, tr("Case name", "केस का नाम"), { dialogText = it.take(80) })
                                    "delete" -> Text(tr("This removes the saved demo timeline from this app.", "इस ऐप से सहेजी डेमो टाइमलाइन हट जाएगी।"))
                                    "clear" -> Text(tr("All saved checks and demo cases will be removed. Your current draft and preferences are kept.", "सभी डेमो केस हटेंगे। मौजूदा ड्राफ़्ट और सेटिंग बनी रहेंगी।"))
                                    "add" -> repo.samples.filter { it.id !in activeCase?.sampleIds.orEmpty() }.forEach { sample ->
                                        TextAction(if (hi) sample.titleHi else sample.title, { persist(cases.map { if (it.id == selectedCase) it.add(sample.id) else it }); dialog = "" })
                                    }
                                    "save" -> {
                                        Text(tr("Only the authored example is saved. Custom drafts are not saved to cases.", "केवल उदाहरण सहेजा जाता है। निजी ड्राफ़्ट केस में नहीं सहेजे जाते।"))
                                        cases.forEach { case ->
                                            TextAction(case.name, { if (repo.resultFor(draft) != null) persist(cases.map { if (it.id == case.id) it.add(draft.sampleId) else it }); dialog = ""; notify(tr("Example saved", "उदाहरण सहेजा गया")) })
                                        }
                                        TextAction(tr("Create a new case", "नया केस बनाएँ"), { dialogText = ""; dialog = "create-save" })
                                    }
                                    "create-save" -> FormField(dialogText, tr("Case name", "केस का नाम"), { dialogText = it.take(80) })
                                }
                            }
                        },
                        confirmButton = { when (dialog) {
                            "create", "rename", "create-save" -> ActionButton(tr("Save", "सहेजें"), {
                                if (dialogText.trim().isNotEmpty()) {
                                    if (dialog == "rename") persist(cases.map { if (it.id == selectedCase) it.copy(name = dialogText.trim()) else it })
                                    else persist(cases + DemoCase(java.util.UUID.randomUUID().toString(), dialogText.trim(), if (dialog == "create-save" && repo.resultFor(draft) != null) listOf(draft.sampleId) else emptyList()))
                                    dialog = ""; notify(tr("Case saved locally", "केस फ़ोन पर सहेजा गया"))
                                }
                            }, enabled = dialogText.isNotBlank())
                            "delete" -> ActionButton(tr("Delete case", "केस हटाएँ"), { persist(cases.filterNot { it.id == selectedCase }); dialog = ""; back() })
                            "clear" -> ActionButton(tr("Clear cases", "केस हटाएँ"), {
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { historyStore.clear() } }
                                        .onSuccess { savedChecks = emptyList(); historyError = false; persist(emptyList()); dialog = ""; notify(tr("Saved checks and cases cleared", "सहेजी गई जाँच और केस हटाए गए")) }
                                        .onFailure { notify("Could not clear saved checks. Try again.") }
                                }
                            })
                        } },
                        dismissButton = { TextAction(tr("Close", "बंद करें"), { dialog = "" }) }
                    )
            }
        }
    }
}
}

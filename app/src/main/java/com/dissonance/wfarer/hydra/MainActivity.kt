package com.dissonance.wfarer.hydra

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewAssetLoader
import com.dissonance.wfarer.hydra.db.AppDatabase
import com.dissonance.wfarer.hydra.db.ScriptEntity
import com.dissonance.wfarer.hydra.db.ScriptRepository
import com.dissonance.wfarer.hydra.ui.MainViewModel
import com.dissonance.wfarer.hydra.ui.MainViewModelFactory
import com.dissonance.wfarer.hydra.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var webViewRef: WebView? = null

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScriptRepository(database.scriptDao())
        MainViewModelFactory(repository)
    }

    private val defaultPresets = listOf(
        ScriptEntity(
            title = "Kinetic Feedback",
            code = "// Kinetic Feedback Loop\ns0.initCam()\nosc(10, 0.1, 0.8)\n  .rotate(0.2, 0.1)\n  .modulate(src(s0))\n  .color(1.2, 0.5, 2.0)\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Audio Visualizer Pulse",
            code = "// Audio Visualizer Pulse (Mic Input)\na.show()\na.setBins(4)\na.setScale(2)\nosc(10, 0.1, 1.0)\n  .modulate(noise(3), () => a.fft[1] * 0.5)\n  .color(0.2, 0.8, 1.5)\n  .scale(() => 1.0 + a.fft[0] * 2.5)\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Audio Spectrum Reactive",
            code = "// Audio Spectrum Reactive (Mic Input)\na.show()\na.setBins(6)\na.setScale(2)\nshape(4, () => 0.2 + a.fft[0] * 0.5)\n  .repeat(4, 4)\n  .scale(() => 1.0 + a.fft[1] * 1.8)\n  .color(() => a.fft[0] * 2.5, () => a.fft[1] * 2.5, () => a.fft[2] * 2.5)\n  .rotate(() => time * 0.1 + a.fft[3])\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Mic & Camera Distort",
            code = "// Mic & Camera Distort\ns0.initCam()\na.show()\na.setBins(4)\na.setScale(2)\nsrc(s0)\n  .color(1.5, 0.8, 1.2)\n  .modulateScale(osc(8, 0.2, 1.5), () => a.fft[0] * 1.2)\n  .kaleid(() => Math.floor(3 + a.fft[1] * 4))\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Rainbow Kaleidoscope",
            code = "// Rainbow Kaleidoscope\nosc(20, 0.05, 2.0)\n  .color(0.9, 0.3, 1.2)\n  .rotate(0.1, 0.1)\n  .kaleid(6)\n  .modulate(osc(10).rotate(0.5))\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Geometric Cyber Grid",
            code = "// Geometric Cyber Grid\nshape(4, 0.5, 0.01)\n  .repeat(10, 10)\n  .modulate(osc(10, 0.05).rotate(0.5))\n  .color(0.1, 0.9, 1.0)\n  .add(osc(5, 0.1, 1.5).color(1.0, 0.2, 0.8), 0.5)\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Liquid Neon Flow",
            code = "// Liquid Neon Flow\nvoronoi(8, 0.3, 0.2)\n  .shift(0.5, 0.5, 0.5)\n  .modulatePixelate(noise(4), 30)\n  .color(1.2, 0.4, 1.8)\n  .rotate(() => Math.sin(time * 0.2) * 0.5)\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Hypnotic Tunnel",
            code = "// Hypnotic Tunnel\nshape(100, 0.2, 0.001)\n  .repeat(5, 5)\n  .modulateRotate(osc(6, 0.1, 0.8))\n  .color(0.8, 0.2, 1.2)\n  .out(o0)"
        ),
        ScriptEntity(
            title = "Plasma Matrix Feedback",
            code = "// Plasma Matrix Feedback\nosc(30, 0.1, 1.5)\n  .rotate(0.5)\n  .modulate(src(o0), 0.3)\n  .color(0.1, 1.0, 0.6)\n  .blend(o0, 0.8)\n  .out(o0)"
        )
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!cameraGranted || !audioGranted) {
            Toast.makeText(this, "Camera & Audio access enable full interactive patches.", Toast.LENGTH_SHORT).show()
        } else {
            // Force: bypass the retry throttle — the permission was just granted,
            // so the next getUserMedia is expected to succeed.
            webViewRef?.evaluateJavascript("initHydra(); resumeAudio(true);", null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                HydraApp(
                    viewModel = viewModel,
                    presets = defaultPresets,
                    onWebViewCreated = { webViewRef = it },
                    runScript = { code -> runCurrentScript(code) },
                    resetHydra = {
                        Toast.makeText(this, "Resetting Hydra Output", Toast.LENGTH_SHORT).show()
                        webViewRef?.evaluateJavascript("stopHydra();", null)
                    },
                    activity = this
                )
            }
        }
    }

    private fun runCurrentScript(code: String) {
        if (code.isBlank()) {
            Toast.makeText(this, "Script is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val escapedCode = code
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")

        webViewRef?.evaluateJavascript("runCode(\"$escapedCode\");", null)
    }

    inner class HydraBridge {
        @JavascriptInterface
        fun onSuccess(msg: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "✨ Script executed", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun onError(msg: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "⚠️ JS Error: $msg", Toast.LENGTH_LONG).show()
            }
        }

        @JavascriptInterface
        fun onMicError(msg: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "🎤 Mic unavailable: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }
}

class HydraSyntaxTransformation : VisualTransformation {

    companion object {
        private val STRING_REGEX = Regex("\".*?\"|'.*?'")
        private val NUMBER_REGEX = Regex("\\b\\d+(\\.\\d+)?\\b")
        private val HYDRA_SOURCE_REGEX = Regex("\\b(osc|shape|voronoi|noise|gradient|solid|src|s0|s1|s2|s3|o0|o1|o2|o3|prev|a)\\b")
        private val HYDRA_TRANSFORM_REGEX = Regex("\\b(rotate|scale|pixelate|kaleid|repeat|repeatX|repeatY|modulate|modulateScale|modulateRotate|modulatePixelate|modulateKaleid|modulateScrollX|modulateScrollY|color|contrast|brightness|posterize|invert|shift|add|blend|mult|diff|out|initCam|init|initImage|initVideo|layer|show|hide|setBins|setScale|setSmooth|setCutoff|fft|bins)\\b")
        private val JS_KEYWORD_REGEX = Regex("\\b(function|const|let|var|return|if|else|Math|time|sin|cos|tan|PI|abs|floor|ceil|random)\\b")
        private val COMMENT_REGEX = Regex("//.*")

        private val COLOR_STRING = Color(0xFFC3E88D)
        private val COLOR_NUMBER = Color(0xFFFFB74D)
        private val COLOR_SOURCE = Color(0xFF00E5FF)
        private val COLOR_TRANSFORM = Color(0xFFF06292)
        private val COLOR_KEYWORD = Color(0xFFC792EA)
        private val COLOR_COMMENT = Color(0xFF7A8B9E)
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val builder = AnnotatedString.Builder(rawText)

        // 1. Strings: ".*?" or '.*?'
        for (match in STRING_REGEX.findAll(rawText)) {
            builder.addStyle(
                SpanStyle(color = COLOR_STRING),
                match.range.first,
                match.range.last + 1
            )
        }

        // 2. Numbers: \b\d+(\.\d+)?\b
        for (match in NUMBER_REGEX.findAll(rawText)) {
            builder.addStyle(
                SpanStyle(color = COLOR_NUMBER, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        // 3. Hydra Sources & Outputs
        for (match in HYDRA_SOURCE_REGEX.findAll(rawText)) {
            builder.addStyle(
                SpanStyle(color = COLOR_SOURCE, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        // 4. Hydra Methods / Transforms
        for (match in HYDRA_TRANSFORM_REGEX.findAll(rawText)) {
            builder.addStyle(
                SpanStyle(color = COLOR_TRANSFORM, fontWeight = FontWeight.Medium),
                match.range.first,
                match.range.last + 1
            )
        }

        // 5. JS Keywords & Math
        for (match in JS_KEYWORD_REGEX.findAll(rawText)) {
            builder.addStyle(
                SpanStyle(color = COLOR_KEYWORD, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        // 6. Comments: //.*
        for (match in COMMENT_REGEX.findAll(rawText)) {
            builder.addStyle(
                SpanStyle(color = COLOR_COMMENT, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
                match.range.first,
                match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydraApp(
    viewModel: MainViewModel,
    presets: List<ScriptEntity>,
    onWebViewCreated: (WebView) -> Unit,
    runScript: (String) -> Unit,
    resetHydra: () -> Unit,
    activity: MainActivity
) {
    var code by remember { mutableStateOf(presets[0].code) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    val visualTransformation = remember { HydraSyntaxTransformation() }
    val context = LocalContext.current

    val savedScripts by viewModel.savedScripts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Hydra Synth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("ENGINE V1.2.4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    TextButton(onClick = resetHydra) {
                        Text("RESET", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // WebView Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val assetLoader = WebViewAssetLoader.Builder()
                            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                            .build()

                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                            settings.apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                allowContentAccess = true
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onPermissionRequest(request: PermissionRequest) {
                                    val resources = request.resources
                                    val toGrant = mutableListOf<String>()
                                    if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) && 
                                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        toGrant.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                                    }
                                    if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) && 
                                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        toGrant.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                                    }
                                    if (toGrant.isNotEmpty()) {
                                        request.grant(toGrant.toTypedArray())
                                        post { evaluateJavascript("resumeAudio(true);", null) }
                                    } else {
                                        request.deny()
                                    }
                                }
                                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                                    Log.d("HydraJS", "[${consoleMessage.messageLevel()}] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                                    return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest
                                ): WebResourceResponse? {
                                    return assetLoader.shouldInterceptRequest(request.url)
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    // Permission callbacks can fire before the page has
                                    // loaded (initHydra would be undefined); re-kick here
                                    // so those early calls are not lost.
                                    view.evaluateJavascript("initHydra(); resumeAudio();", null)
                                }
                            }
                            addJavascriptInterface(activity.HydraBridge(), "AndroidBridge")
                            loadUrl("https://appassets.androidplatform.net/assets/index.html")
                            onWebViewCreated(this)
                        }
                    },
                    onRelease = { webView ->
                        webView.stopLoading()
                        webView.destroy()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Editor Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }

                TextField(
                    value = code,
                    onValueChange = { code = it },
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Bottom Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showLoadDialog = true }) {
                        Text("SCRIPTS", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showSaveDialog = true }) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { runScript(code) }) {
                        Text("RUN SCRIPT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom Script") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Patch Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.saveScript(title, code)
                        Toast.makeText(context, "Saved \"${title.trim()}\"", Toast.LENGTH_SHORT).show()
                        showSaveDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showLoadDialog) {
        var showManage by remember { mutableStateOf(false) }

        if (showManage) {
            AlertDialog(
                onDismissRequest = { 
                    showManage = false 
                    showLoadDialog = false
                },
                title = { Text("Manage Saved Scripts") },
                text = {
                    if (savedScripts.isEmpty()) {
                        Text("No saved scripts.")
                    } else {
                        LazyColumn {
                            items(savedScripts, key = { it.id }) { script ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.deleteScript(script)
                                            Toast.makeText(context, "Deleted \"${script.title}\"", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🗑️ ${script.title}")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showManage = false 
                        showLoadDialog = false
                    }) { Text("Close") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showLoadDialog = false },
                title = { Text("Load Visual Script") },
                text = {
                    LazyColumn {
                        item {
                            Text("--- PRESET PATCHES ---", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        items(presets) { preset ->
                            Text(
                                text = "✨ ${preset.title}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        code = preset.code
                                        runScript(code)
                                        showLoadDialog = false
                                        Toast.makeText(context, "Loaded: ${preset.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                        if (savedScripts.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("--- MY SAVED SCRIPTS ---", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(savedScripts, key = { it.id }) { script ->
                                Text(
                                    text = "💾 ${script.title}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            code = script.code
                                            runScript(code)
                                            showLoadDialog = false
                                            Toast.makeText(context, "Loaded: ${script.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showManage = true }) { Text("Manage Saved") }
                },
                dismissButton = {
                    TextButton(onClick = { showLoadDialog = false }) { Text("Close") }
                }
            )
        }
    }
}

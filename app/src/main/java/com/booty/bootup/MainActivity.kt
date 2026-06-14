package com.booty.bootup

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.composed
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var isPermissionGranted = mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        isPermissionGranted.value = checkAccessibilityPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- NEW: HIDE SYSTEM BARS (FULLSCREEN IMMERSIVE MODE) ---
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // ---------------------------------------------------------

        val interceptedPackage = intent.getStringExtra("INTERCEPTED_APP_PACKAGE")

        setContent {
            // ... (The rest of your code stays exactly the same) ...
            BootUpTheme {
                val context = LocalContext.current
                val targetManager = remember { TargetManager(context) }

                // Track the shader setting at the highest level so the background instantly updates
                var isShaderEnabled by remember { mutableStateOf(targetManager.isShaderEnabled()) }

                // Pass the toggle state into the CRT effect
                Box(modifier = Modifier.fillMaxSize().crtEffect(isShaderEnabled)) {
                    val hasPermission by isPermissionGranted

                    if (!hasPermission) {
                        PermissionScreen(
                            onOpenSettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        )
                    } else {
                        if (interceptedPackage != null) {
                            val appName = getAppNameFromPackage(this@MainActivity, interceptedPackage)
                            BootSequenceScreen(targetApp = appName, targetAppPackage = interceptedPackage)
                        } else {
                            var isBiosBooting by remember { mutableStateOf(true) }
                            var currentScreen by remember { mutableStateOf("MAIN_MENU") }

                            if (isBiosBooting) {
                                BiosBootScreen(onBootComplete = { isBiosBooting = false })
                            } else {
                                when (currentScreen) {
                                    "MAIN_MENU" -> MainMenuScreen(
                                        onNavigateToSelector = { currentScreen = "SELECTOR" },
                                        onNavigateToTimeFrame = { currentScreen = "TIME_FRAME" }
                                    )
                                    "SELECTOR" -> AppSelectorScreen(
                                        onBack = { currentScreen = "MAIN_MENU" }
                                    )
                                    "TIME_FRAME" -> TimeFrameScreen(
                                        onBack = { currentScreen = "MAIN_MENU" },
                                        isShaderEnabled = isShaderEnabled,
                                        onToggleShader = {
                                            isShaderEnabled = !isShaderEnabled
                                            targetManager.saveShaderEnabled(isShaderEnabled)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } // End of CRT Box
            }
        }
    }

    private fun checkAccessibilityPermission(): Boolean {
        val expectedComponentName = ComponentName(this, InterceptorService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServicesSetting.contains(expectedComponentName.flattenToString())
    }

    private fun getAppNameFromPackage(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(info).toString().uppercase()
        } catch (e: Exception) {
            "UNKNOWN TARGET"
        }
    }
}

@Composable
fun MainMenuScreen(onNavigateToSelector: () -> Unit, onNavigateToTimeFrame: () -> Unit) {
    val retroFont = FontFamily(Font(R.font.vt323))
    val brightGreen = Color(0xFF4AF626)
    val darkBackground = Color(0xFF0F0F0F)

    val infiniteTransition = rememberInfiniteTransition(label = "BlinkCursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "Blink"
    )

    var selectedChoice by remember { mutableStateOf<String?>(null) }
    var transitionLines by remember { mutableStateOf(listOf<String>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedChoice) {
        if (selectedChoice != null) {
            delay(300)

            val rapidLinesCount = 35
            for (i in 1..rapidLinesCount) {
                val hexAddress = "0x" + (100000..999999).random().toString(16).uppercase()
                val fileType = listOf(".sys", ".dll", ".bin", ".dat", ".cfg", ".exe").random()
                val moduleNum = (10..99).random()
                val rapidLine = "$hexAddress: executing module_$moduleNum$fileType ... [OK]"
                delay((10..25).random().toLong())
                transitionLines = transitionLines + rapidLine
            }

            delay(200)
            transitionLines = transitionLines + " "
            transitionLines = transitionLines + "> redirecting mainframe stream... [DONE]"

            delay(700)

            if (selectedChoice == "1") onNavigateToSelector() else onNavigateToTimeFrame()
        }
    }

    LaunchedEffect(transitionLines.size) {
        if (transitionLines.isNotEmpty()) {
            listState.scrollToItem(transitionLines.size)
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp)) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text("C:\\BOOTUP> MAIN_MENU.exe", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(16.dp))
            Text("BOOTUP MAIN CONTROL CONFIGURATION INTERFACE", color = brightGreen, fontSize = 18.sp, fontFamily = retroFont)
            Text("------------------------------------------------", color = brightGreen, fontSize = 18.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(40.dp))

            Row(modifier = Modifier.fillMaxWidth().clickable { if (selectedChoice == null) selectedChoice = "1" }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("[1] TARGET APPLICATION SELECTOR", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { if (selectedChoice == null) selectedChoice = "2" }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("[2] SYSTEM CONFIGURATION", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text("------------------------------------------------", color = brightGreen, fontSize = 18.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ENTER CHOICE [1-2]: ", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)

                if (selectedChoice != null) {
                    Text(selectedChoice!!, color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
                } else {
                    Text("█", color = brightGreen.copy(alpha = if (cursorAlpha > 0.5f) 1f else 0f), fontSize = 24.sp, fontFamily = retroFont)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(transitionLines) { line ->
            Text(text = line, color = brightGreen, fontSize = 20.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun TimeFrameScreen(onBack: () -> Unit, isShaderEnabled: Boolean, onToggleShader: () -> Unit) {
    val retroFont = FontFamily(Font(R.font.vt323))
    val brightGreen = Color(0xFF4AF626)
    val darkBackground = Color(0xFF0F0F0F)
    val context = LocalContext.current
    val targetManager = remember { TargetManager(context) }

    var inputSeconds by remember { mutableStateOf((targetManager.getBootDuration() / 1000).toString()) }

    Column(modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp)) {
        Text(text = "[ < GO BACK TO MENU ]", color = brightGreen, fontSize = 22.sp, fontFamily = retroFont, modifier = Modifier.clickable { onBack() }.padding(bottom = 24.dp, top = 24.dp))
        Text("C:\\BOOTUP> SYS_CONFIG.exe", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont, modifier = Modifier.padding(bottom = 32.dp))

        Text("SET BOOT SEQUENCE DURATION (SECONDS):", color = brightGreen, fontSize = 20.sp, fontFamily = retroFont)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("> ", color = brightGreen, fontSize = 28.sp, fontFamily = retroFont)

            BasicTextField(
                value = inputSeconds,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        inputSeconds = newValue
                    }
                },
                textStyle = TextStyle(color = brightGreen, fontSize = 28.sp, fontFamily = retroFont),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(brightGreen),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- NEW SHADER TOGGLE SETTING ---
        Text("HARDWARE ACCELERATION:", color = brightGreen, fontSize = 20.sp, fontFamily = retroFont)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggleShader() }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "> ADVANCED CRT SHADER: ",
                color = brightGreen,
                fontSize = 20.sp, // Slightly smaller
                fontFamily = retroFont,
                modifier = Modifier.weight(1f) // Lets it take up available space without pushing the toggle off-screen
            )
            Text(
                text = if (isShaderEnabled) "[ ENABLED ]" else "[ DISABLED ]",
                color = brightGreen,
                fontSize = 20.sp, // Slightly smaller
                fontFamily = retroFont
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "[ SAVE & APPLY CONFIGURATION ]",
            color = brightGreen,
            fontSize = 24.sp,
            fontFamily = retroFont,
            modifier = Modifier.clickable {
                val seconds = inputSeconds.toLongOrNull() ?: 5L
                val finalMs = if (seconds < 1) 1000L else seconds * 1000L
                targetManager.saveBootDuration(finalMs)
                onBack()
            }
        )
    }
}

@Composable
fun PermissionScreen(onOpenSettings: () -> Unit) {
    val retroFont = FontFamily(Font(R.font.vt323))
    val brightGreen = Color(0xFF4AF626)
    val redAlert = Color(0xFFFF3333)
    val darkBackground = Color(0xFF0F0F0F)

    Column(modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("C:\\BOOTUP> init_watcher.exe", color = brightGreen, fontSize = 22.sp, fontFamily = retroFont, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("[FATAL ERROR] KERNEL ACCESS DENIED", color = redAlert, fontSize = 28.sp, fontFamily = retroFont, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Text("The background interceptor is currently offline. To monitor targets and execute the boot sequence, BootUp requires system-level Accessibility permissions.", color = brightGreen, fontSize = 20.sp, fontFamily = retroFont, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(48.dp))
        Box(modifier = Modifier.fillMaxWidth().border(2.dp, redAlert).clickable { onOpenSettings() }.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(">>> GRANT PERMISSIONS <<<", color = redAlert, fontSize = 24.sp, fontFamily = retroFont, textAlign = TextAlign.Center)
        }
    }
}

class TargetManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("BootUpPrefs", Context.MODE_PRIVATE)

    fun getTargets(): Set<String> = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
    fun saveTargets(targets: Set<String>) = prefs.edit().putStringSet("blocked_packages", targets).apply()

    fun getBootDuration(): Long = prefs.getLong("boot_duration", 5000L)
    fun saveBootDuration(durationMs: Long) = prefs.edit().putLong("boot_duration", durationMs).apply()

    // NEW METHODS TO TRACK SHADER PREFERENCE
    fun isShaderEnabled(): Boolean = prefs.getBoolean("shader_enabled", true)
    fun saveShaderEnabled(enabled: Boolean) = prefs.edit().putBoolean("shader_enabled", enabled).apply()
}

data class AppInfo(val name: String, val packageName: String)

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0).map {
        AppInfo(name = it.loadLabel(pm).toString().uppercase(), packageName = it.activityInfo.packageName)
    }.sortedBy { it.name }
}

@Composable
fun AppSelectorScreen(onBack: () -> Unit) {
    val retroFont = FontFamily(Font(R.font.vt323))
    val brightGreen = Color(0xFF4AF626)
    val darkBackground = Color(0xFF0F0F0F)
    val context = LocalContext.current

    val targetManager = remember { TargetManager(context) }
    var installedApps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var blockedPackages by remember { mutableStateOf(targetManager.getTargets()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) { getInstalledApps(context) }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp)) {
        Text(text = "[ < GO BACK TO MENU ]", color = brightGreen, fontSize = 22.sp, fontFamily = retroFont, modifier = Modifier.clickable { onBack() }.padding(bottom = 24.dp, top = 24.dp))

        Text("C:\\BOOTUP> SELECT TARGETS_ ", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont, modifier = Modifier.padding(bottom = 32.dp))

        if (isLoading) {
            Text("SCANNING MAINFRAME FOR TARGETS...", color = brightGreen, fontSize = 20.sp, fontFamily = retroFont)
        } else {
            LazyColumn {
                items(installedApps) { app ->
                    val isSelected = blockedPackages.contains(app.packageName)
                    val prefix = if (isSelected) "[X]" else "[ ]"
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val newBlockedList = if (isSelected) blockedPackages - app.packageName else blockedPackages + app.packageName
                            blockedPackages = newBlockedList
                            targetManager.saveTargets(newBlockedList)
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$prefix ${app.name}", color = brightGreen, fontSize = 26.sp, fontFamily = retroFont)
                    }
                }
            }
        }
    }
}

@Composable
fun BiosBootScreen(onBootComplete: () -> Unit) {
    val retroFont = FontFamily(Font(R.font.vt323))
    val brightGreen = Color(0xFF4AF626)
    val darkBackground = Color(0xFF0F0F0F)

    val bootLines = listOf(
        "BootUp BIOS v1.0.4", "Copyright (C) 1984-2026, Booty Corp.", "Initializing physical memory...", "640K RAM SYSTEM GOOD",
        "Loading system drivers......... OK", "Mounting virtual drives........ OK", "Reticulating splines........... DONE",
        "Downloading more RAM........... 100%", "Hiding browser history......... SECURED", "Checking for loose screws...... IGNORED",
        "[WARN] Coffee levels critically low!", "Injecting caffeine into CPU.... SUCCESS", "Locating the 'Any' key......... FAILED",
        "Ignoring previous error........ OK", "Bypassing mainframe security... OK", "Waking up the server hamsters.. AWAKE",
        "Calibrating flux capacitor..... 1.21GW", "Commencing kernel override protocol..."
    )

    var visibleLines by remember { mutableStateOf(listOf<String>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        for (i in bootLines.indices) {
            val line = bootLines[i]
            val waitTime = when {
                i < 2 -> 600L
                line.contains("WARN") || line.contains("FAILED") -> 800L
                else -> (30..120).random().toLong()
            }
            delay(waitTime)
            visibleLines = visibleLines + line
            listState.scrollToItem(visibleLines.size - 1)
        }
        delay(400)
        val rapidLinesCount = 65
        for (i in 1..rapidLinesCount) {
            val hexAddress = "0x" + (100000..999999).random().toString(16).uppercase()
            val fileType = listOf(".sys", ".dll", ".bin", ".dat", ".cfg").random()
            val moduleNum = (10..99).random()
            val rapidLine = "$hexAddress: executing payload_block_$moduleNum$fileType ... [OK]"
            delay((10..30).random().toLong())
            visibleLines = visibleLines + rapidLine
            listState.scrollToItem(visibleLines.size - 1)
        }
        delay(300)
        visibleLines = visibleLines + " "
        visibleLines = visibleLines + "> booting interface: [OK]"
        listState.scrollToItem(visibleLines.size - 1)
        delay(900)
        onBootComplete()
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp)) {
        items(visibleLines) { line ->
            Text(text = line, color = brightGreen, fontSize = 20.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun BootSequenceScreen(targetApp: String, targetAppPackage: String) {
    val retroFont = FontFamily(Font(R.font.vt323))
    val brightGreen = Color(0xFF4AF626)
    val dimGreen = Color(0xFF3B7342)
    val darkBackground = Color(0xFF0F0F0F)
    val trackGreen = Color(0xFF1E3A20)
    val context = LocalContext.current

    val totalBootDurationMs = remember { TargetManager(context).getBootDuration() }
    var targetProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = totalBootDurationMs.toInt(), easing = LinearEasing),
        label = "LoadingAnimation"
    )

    val phase1Text = remember {
        listOf(
            "Downloading more RAM...",
            "Waking up the server hamsters...",
            "Reticulating splines...",
            "Initializing dummy variables...",
            "Warming up cathode tubes...",
            "Calibrating Pip-Boy 3000 interface...",
            "Scanning for the 'Any' key... (Still missing)...",
            "Waking up the band...",
            "Opening the pod bay doors, HAL...",
            "Spinning up the TARDIS engines...",
            "Booting up the Batcomputer...",
            "Linking the Master Emerald...",
            "Inserting cartridge... blowing on it first...",
        ).random()
    }

    val phase2Text = remember {
        listOf(
            "Hiding your browser history...",
            "Applying percussive maintenance...",
            "Feeding the mainframe gremlins...",
            "Ignoring fatal system errors...",
            "Defragging the cloud...",
            "Charging Portal Gun fluid cells...",
            "Realigning dilithium crystals...",
            "Downloading Kung Fu...",
            "Brewing Polyjuice Potion...",
            "Smashing pots for rupees...",
        ).random()
    }

    val phase3Text = remember {
        listOf(
            "Bypassing mainframe security...",
            "Dividing by zero...",
            "Hacking the Gibson...",
            "Uploading Chicken_jockey.exe...",
            "Rerouting power to life support...",
            "Microwaving the microchips...",
            "Synchronizing Animus memory stream...",
            "Executing Order 66 on background processes...",
            "Going plaid...",
            "Opening a portal to the Upside Down...",
            "Unlocking the Master Sword...",
            "Crossing the streams...",
        ).random()
    }

    val phase4Text = remember {
        listOf(
            "We're in.",
            "Mainframe compromised.",
            "Boot complete.",
            "System hijacked.",
            "Payload delivered.",
            "Humanity Restored.",
            "Task failed successfully.",
            "Task completed. Go home, user, you're drunk.",
            "System online. I'll be back.",
            "Mischief managed.",
            "It's dangerous to go alone. Take this.",
            "Game over, man. Game over!",
            "You are overencumbered and cannot run.",
            "Flawless victory.",
            "Fatality."
        ).random()
    }

    val terminalText = when {
        animatedProgress < 0.3f -> phase1Text
        animatedProgress < 0.6f -> phase2Text
        animatedProgress < 0.9f -> phase3Text
        else -> phase4Text
    }

    LaunchedEffect(Unit) {
        targetProgress = 1f
        delay(totalBootDurationMs)
        delay(500)

        InterceptorService.activeApp = targetAppPackage
        InterceptorService.interceptingApp = null
        InterceptorService.transitionShieldEndTime = System.currentTimeMillis() + 2500L

        (context as? ComponentActivity)?.finish()
    }

    Column(modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("BOOT UP", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(12.dp))
            Text("INTERCEPTING $targetApp", color = dimGreen, fontSize = 20.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(12.dp))
            Text("v1.1", color = dimGreen, fontSize = 18.sp, fontFamily = retroFont)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    text = terminalText,
                    color = brightGreen,
                    fontSize = 22.sp,
                    fontFamily = retroFont,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = brightGreen,
                    fontSize = 22.sp,
                    fontFamily = retroFont
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(4.dp), color = brightGreen, trackColor = trackGreen)
            Spacer(modifier = Modifier.height(32.dp))
            Text("STAND BY...", color = dimGreen, fontSize = 18.sp, fontFamily = retroFont)
        }
        Spacer(modifier = Modifier.weight(0.8f))
    }
}

@Composable
fun BootUpTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(content = content)
}

// --- NOW ACCEPTS THE TOGGLE STATE ---
fun Modifier.crtEffect(isShaderEnabled: Boolean): Modifier = composed {
    // Both constraints must be true to run the heavy math:
    // Android 13+ AND the user hasn't toggled it off
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isShaderEnabled) {

        val infiniteTransition = rememberInfiniteTransition(label = "crtTime")
        val time by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(100000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "time"
        )

        val SHADER_SRC = """
            uniform shader composable;
            uniform float2 resolution;
            uniform float time; 
            
            vec2 warp(vec2 uv) {
                vec2 coord = (uv - 0.5) * 2.0;
                coord.x *= 1.0 + pow(abs(coord.y) / 8.0, 2.0);
                coord.y *= 1.0 + pow(abs(coord.x) / 7.0, 2.0);
                return (coord / 2.0) + 0.5;
            }
            
            vec3 getBrightPixels(vec2 coord) {
                vec3 color = composable.eval(coord).rgb;
                return max(vec3(0.0), color - 0.05); 
            }
            
            half4 main(float2 fragCoord) {
                vec2 uv = fragCoord.xy / resolution.xy;
                vec2 warpedUV = warp(uv);
                
                if (warpedUV.x < 0.0 || warpedUV.x > 1.0 || warpedUV.y < 0.0 || warpedUV.y > 1.0) {
                    return half4(0.0, 0.0, 0.0, 1.0);
                }
                
                vec2 pixelCoord = warpedUV * resolution.xy;
                
                // LAYER 1: CRISP BASE
                vec3 cleanplate = composable.eval(pixelCoord).rgb;
                
                // LAYER 2: GOLDEN SPIRAL BLUR
                vec3 bloom = vec3(0.0);
                float totalWeight = 0.0;
                
                float maxRadius = 40.0; 
                float goldenAngle = 2.3999632; 
                float samples = 45.0; 
                
                for (float i = 0.0; i < 45.0; i++) {
                    float r = maxRadius * sqrt(i / samples);
                    float theta = i * goldenAngle;
                    vec2 offset = vec2(cos(theta), sin(theta)) * r;
                    float weight = 1.0 - (r / maxRadius);
                    bloom += getBrightPixels(pixelCoord + offset) * weight;
                    totalWeight += weight;
                }
                
                bloom /= totalWeight;
                
                // OPACITY BLENDING
                float brushOpacity = 0.40; 
                vec3 finalColor = cleanplate + (bloom * 2.5 * brushOpacity);
                
                // POST-PROCESSING: CLASSIC TERMINAL
                float staticLines = sin(fragCoord.y * 2.5) * 0.04;
                float rollSpeed = 0.8;
                float rollBar = sin((warpedUV.y * 15.0) - (time * rollSpeed)) * 0.06;
                
                finalColor *= (0.92 + staticLines + rollBar);
                
                float dx = 2.0 * warpedUV.x - 1.0;
                float dy = 2.0 * warpedUV.y - 1.0;
                float radius = sqrt(dx*dx + dy*dy);
                float vignette = smoothstep(1.4, 0.7, radius);
                finalColor *= vignette;
                
                return half4(finalColor, 1.0);
            }
        """

        val shader = remember { RuntimeShader(SHADER_SRC) }

        this.graphicsLayer {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", time)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
            clip = true
        }
    } else {
        // --- FALLBACK FOR OLDER ANDROID PHONES OR TOGGLED OFF ---
        this.drawWithContent {
            // Draw the actual UI first
            drawContent()

            // 1. Thinner, much more transparent scanlines (Alpha 0.10 instead of 0.25)
            val scanlineColor = Color.Black.copy(alpha = 0.10f)
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = scanlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 3f
                )
                y += 8f
            }

            // 2. A much softer vignette based on height so it doesn't crush the top/bottom
            val vignette = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.50f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.height * 0.75f // Pushes the shadow far into the corners
            )
            drawRect(brush = vignette)
        }
    }
}
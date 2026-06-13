package com.booty.bootup

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

        val interceptedPackage = intent.getStringExtra("INTERCEPTED_APP_PACKAGE")

        setContent {
            BootUpTheme {
                val hasPermission by isPermissionGranted

                if (!hasPermission) {
                    PermissionScreen(
                        onOpenSettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    )
                } else {
                    if (interceptedPackage != null) {
                        val appName = getAppNameFromPackage(this, interceptedPackage)
                        BootSequenceScreen(targetApp = appName)
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
                                    onBack = { currentScreen = "MAIN_MENU" }
                                )
                            }
                        }
                    }
                }
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

// --- UPDATED: Dynamic Main Menu with Execute Transition Effect ---
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

    // State to track if the user clicked something and what lines to dump
    var selectedChoice by remember { mutableStateOf<String?>(null) }
    var transitionLines by remember { mutableStateOf(listOf<String>()) }
    val listState = rememberLazyListState()

    // When a choice is made, run the text dump animation!
    LaunchedEffect(selectedChoice) {
        if (selectedChoice != null) {
            delay(300) // Small pause after typing the number

            // Fire the rapid text dump
            val rapidLinesCount = 35
            for (i in 1..rapidLinesCount) {
                val hexAddress = "0x" + (100000..999999).random().toString(16).uppercase()
                val fileType = listOf(".sys", ".dll", ".bin", ".dat", ".cfg", ".exe").random()
                val moduleNum = (10..99).random()
                val rapidLine = "$hexAddress: executing module_$moduleNum$fileType ... [OK]"
                delay((10..25).random().toLong())
                transitionLines = transitionLines + rapidLine
            }

            // Dramatic hard stop
            delay(200)
            transitionLines = transitionLines + " "
            transitionLines = transitionLines + "> redirecting mainframe stream... [DONE]"

            delay(700) // Final hold before changing screens

            // Execute the actual navigation
            if (selectedChoice == "1") onNavigateToSelector() else onNavigateToTimeFrame()
        }
    }

    // Auto-scroll effect: keeps the camera at the bottom of the dump
    LaunchedEffect(transitionLines.size) {
        if (transitionLines.isNotEmpty()) {
            listState.scrollToItem(transitionLines.size)
        }
    }

    // We swapped Column for LazyColumn so it can scroll down!
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp)) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text("C:\\BOOTUP> MAIN_MENU.exe", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(16.dp))
            Text("BOOTUP MAIN CONTROL CONFIGURATION INTERFACE", color = brightGreen, fontSize = 18.sp, fontFamily = retroFont)
            Text("------------------------------------------------", color = brightGreen, fontSize = 18.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(40.dp))

            // Only allow clicks if selectedChoice is currently null!
            Row(modifier = Modifier.fillMaxWidth().clickable { if (selectedChoice == null) selectedChoice = "1" }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("[1] TARGET APPLICATION SELECTOR", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { if (selectedChoice == null) selectedChoice = "2" }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("[2] TIME FRAME CONFIGURATION", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text("------------------------------------------------", color = brightGreen, fontSize = 18.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ENTER CHOICE [1-2]: ", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)

                if (selectedChoice != null) {
                    // Show their typed number!
                    Text(selectedChoice!!, color = brightGreen, fontSize = 24.sp, fontFamily = retroFont)
                } else {
                    // Show blinking cursor!
                    Text("█", color = brightGreen.copy(alpha = if (cursorAlpha > 0.5f) 1f else 0f), fontSize = 24.sp, fontFamily = retroFont)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Draw the rapid fire lines below the prompt as they generate
        items(transitionLines) { line ->
            Text(text = line, color = brightGreen, fontSize = 20.sp, fontFamily = retroFont)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun TimeFrameScreen(onBack: () -> Unit) {
    val retroFont = FontFamily(Font(R.font.vt323))
    val brightGreen = Color(0xFF4AF626)
    val darkBackground = Color(0xFF0F0F0F)
    val context = LocalContext.current
    val targetManager = remember { TargetManager(context) }

    var inputSeconds by remember { mutableStateOf((targetManager.getBootDuration() / 1000).toString()) }

    Column(modifier = Modifier.fillMaxSize().background(darkBackground).padding(24.dp)) {
        Text(text = "[ < GO BACK TO MENU ]", color = brightGreen, fontSize = 22.sp, fontFamily = retroFont, modifier = Modifier.clickable { onBack() }.padding(bottom = 24.dp, top = 24.dp))
        Text("C:\\BOOTUP> TIME_CONFIG.exe", color = brightGreen, fontSize = 24.sp, fontFamily = retroFont, modifier = Modifier.padding(bottom = 32.dp))

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
fun BootSequenceScreen(targetApp: String) {
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

    val terminalText = when {
        animatedProgress < 0.3f -> "Initializing system..."
        animatedProgress < 0.6f -> "Allocating memory..."
        animatedProgress < 0.9f -> "Bypassing security..."
        else -> "Boot complete."
    }

    LaunchedEffect(Unit) {
        targetProgress = 1f
        delay(totalBootDurationMs)
        delay(500)
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
                Text(terminalText, color = brightGreen, fontSize = 22.sp, fontFamily = retroFont)
                Text("${(animatedProgress * 100).toInt()}%", color = brightGreen, fontSize = 22.sp, fontFamily = retroFont)
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
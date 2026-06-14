package com.booty.bootup

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class InterceptorService : AccessibilityService() {

    companion object {
        // The app they successfully finished booting (The Unlock Key)
        var activeApp: String? = null

        // The app currently stuck on the loading screen
        var interceptingApp: String? = null

        // Ghost shield for closing apps
        var lastExitTime: Long = 0L

        // Transition shield for when MainActivity closes and hands control back
        var transitionShieldEndTime: Long = 0L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val openedPackage = event.packageName?.toString() ?: return

            // Ignore our own app completely
            if (openedPackage == packageName) return

            val targetManager = TargetManager(this)
            val blockedApps = targetManager.getTargets()

            if (blockedApps.contains(openedPackage)) {

                // Did they already earn the unlock key from MainActivity?
                if (openedPackage == activeApp) return

                // Are we currently showing the loading screen for this exact app?
                if (openedPackage == interceptingApp) return

                // GHOST EXIT SHIELD: Prevent closing animations from triggering a new trap
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastExitTime < 2000L) return

                // It's a fresh open! Hold them at the gate!
                interceptingApp = openedPackage

                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("INTERCEPTED_APP_PACKAGE", openedPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivity(intent)

            } else {
                // They opened a non-blocked app (Launcher, Settings, BootUp Menu, etc.)

                // Ignore invisible system menus and keyboards
                val ignoredPackages = listOf(
                    "com.android.systemui",
                    "com.google.android.inputmethod.latin",
                    "com.samsung.android.honeyboard"
                )
                if (ignoredPackages.contains(openedPackage)) return

                val currentTime = System.currentTimeMillis()

                // TRANSITION SHIELD: Are we currently handing control back to the target app?
                // This prevents invisible system flashes from resetting the trap during the handoff!
                if (currentTime < transitionShieldEndTime) return

                // If they truly left to another app (or bailed out early), WIPE EVERYTHING. Trap resets instantly!
                if (activeApp != null || interceptingApp != null) {
                    activeApp = null
                    interceptingApp = null
                    lastExitTime = currentTime
                }
            }
        }
    }

    override fun onInterrupt() {}
}
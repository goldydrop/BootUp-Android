package com.booty.bootup

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class InterceptorService : AccessibilityService() {

    companion object {
        // Remembers the last app we successfully unlocked so we don't double-intercept it
        private var lastUnlockedPackage: String? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val openedPackage = event.packageName?.toString() ?: return

            // Critical: Don't intercept ourselves!
            if (openedPackage == packageName) return

            val targets = TargetManager(this).getTargets()

            if (targets.contains(openedPackage)) {
                // If it's the app we JUST unlocked, let them stay in it!
                if (openedPackage == lastUnlockedPackage) return

                // Otherwise, they are opening it fresh. Set the trap!
                // We mark it as unlocked so when BootUp closes, it doesn't infinite loop.
                lastUnlockedPackage = openedPackage

                val launchIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("INTERCEPTED_APP_PACKAGE", openedPackage)
                }
                startActivity(launchIntent)
            } else {
                // If they opened ANY OTHER app (like going back to the home screen),
                // we clear the memory. The trap is reset!
                lastUnlockedPackage = openedPackage
            }
        }
    }

    override fun onInterrupt() {
        // Leave blank
    }
}
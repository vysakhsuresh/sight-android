package com.layerbit.ikshana

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * Catches a crash, remembers it, and brings the app back.
 *
 * The owner's rule: a crash is caught, spoken ("Ikshana restarted, say it again"), and the mode
 * returns where it was. A blind user holding a phone whose app has vanished hears nothing and
 * cannot see the system's "app has stopped" dialog to know why, so the handler relaunches the
 * activity before the process goes, and the next start reads the marker and says so (the mode
 * machine's Started(afterCrash = true); the last mode is already in Prefs).
 *
 * Not twice within [LOOP_MS]: a crash that happens on every start would otherwise spin forever
 * with the phone buzzing and speaking, which is worse than the system dialog. The second crash
 * inside the window goes to the default handler.
 *
 * Built, not seen: whether the relaunch survives the process kill on the moto's Android 14 is
 * PENDING_TESTS item 3.
 */
class IkshanaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = Prefs(this)
        val system = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val now = System.currentTimeMillis()
            val previous = prefs.lastCrashAt
            Log.e("Ikshana", "crash, relaunching", error)
            if (previous != 0L && now - previous < LOOP_MS) {
                system?.uncaughtException(thread, error)
                return@setDefaultUncaughtExceptionHandler
            }
            prefs.markCrash(now)
            try {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            } catch (_: Throwable) {
                // Nothing else to try from a dying process; the marker still makes the next
                // manual start say what happened.
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    companion object {
        const val LOOP_MS = 15_000L
    }
}

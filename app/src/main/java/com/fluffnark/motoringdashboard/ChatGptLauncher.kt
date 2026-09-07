package com.fluffnark.motoringdashboard

import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Uses the installed app's public launcher. Does not claim to start a voice session. */
object ChatGptLauncher {
    fun open(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
        if (intent == null) {
            Toast.makeText(context, "Install ChatGPT on your phone first", Toast.LENGTH_LONG).show()
            return
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "ChatGPT is unavailable on this phone", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "Open ChatGPT from your phone while parked", Toast.LENGTH_LONG).show()
        }
    }
}

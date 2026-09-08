package com.fluffnark.motoringdashboard

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.widget.Toast

/** Optional exported voice entry point, verified at runtime; not a stable OpenAI API. */
object ChatGptLauncher {
    fun voiceIntent(context: Context): Intent? {
        val component = ComponentName("com.openai.chatgpt", "com.openai.voice.assistant.AssistantActivity")
        val info = try {
            context.packageManager.getActivityInfo(component, 0)
        } catch (_: PackageManager.NameNotFoundException) { return null }
        if (!info.exported || !info.enabled || !info.applicationInfo.enabled) return null
        if (info.permission != null && context.checkSelfPermission(info.permission) != PackageManager.PERMISSION_GRANTED) return null
        return Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun open(context: Context) {
        val intent = voiceIntent(context) ?: context.packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
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

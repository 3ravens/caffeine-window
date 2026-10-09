package com.caffeinewindow.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.caffeinewindow.app.CaffeineLog
import com.caffeinewindow.app.signal.ScreenUnlockSignalSource

class UnlockReceiver(
  private val screenUnlockSignalSource: ScreenUnlockSignalSource,
) : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Log.d(CaffeineLog.TAG, "UnlockReceiver.onReceive action=${intent.action}")
    if (intent.action == Intent.ACTION_USER_PRESENT) {
      screenUnlockSignalSource.onScreenUnlocked()
    }
  }

  companion object {
    fun register(context: Context, receiver: UnlockReceiver): IntentFilter {
      val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // ACTION_USER_PRESENT is a protected broadcast sent by the system (not by
        // this app), so it must be registered as EXPORTED to be delivered. Being a
        // protected broadcast, no third-party app can spoof it.
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
      } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, filter)
      }
      return filter
    }
  }
}

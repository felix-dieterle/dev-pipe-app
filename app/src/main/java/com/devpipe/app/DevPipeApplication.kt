package com.devpipe.app

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DevPipeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DevPipeApp", "Uncaught exception on thread ${thread.name}", throwable)
            try {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("error", throwable.message ?: throwable.javaClass.simpleName)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("DevPipeApp", "Failed to start CrashActivity", e)
                defaultHandler?.uncaughtException(thread, throwable)
            } finally {
                Process.killProcess(Process.myPid())
            }
        }
    }
}

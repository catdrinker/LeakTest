package com.tt.leaktest

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class Controller {
    private val callbacks = ConcurrentHashMap<String, ConcurrentHashMap<Int, MainCallback>>()

    @Volatile
    private var seq = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    fun start(callback: MainCallback) {
        seq++
        val key = callback.key
        val mutableMap = callbacks[key]
        if (mutableMap == null) {
            seq = 1
            val map = ConcurrentHashMap<Int, MainCallback>()
            map[seq] = callback
            callbacks[key] = map
        } else {
            mutableMap[seq] = callback
        }
        Thread(object : MyRunnable(seq) {
            override fun run() {
                // 网络请求，db操作等异步操作
                val time = (0..10000L).random()
                Log.i("MainActivity", "time $time")
                Thread.sleep(time)
                Log.i("MainActivity", "running task")
                mainHandler.post {
                    callbacks[key]?.get(mSeq)?.onCallback(mSeq)
                }
            }
        }).start()
    }

    fun removeCallback(key: String) {
        callbacks.remove(key)
    }

    private var callback: Callback? = null

    fun startCallback(callback: Callback) {
        synchronized(this) {
            seq++
            this.callback = callback
        }
        Thread {
            Thread.sleep(2000)
            this.callback?.onCallback(seq)
        }.start()
    }

    fun removeCallback() {
        this.callback = null
    }



    abstract inner class MyRunnable(val mSeq: Int) : Runnable
}
package com.tt.leaktest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.telecom.Call
import android.util.Log
import android.widget.Button
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

class MainActivity : AppCompatActivity() {
    private lateinit var controller: Controller

//    private lateinit var btn: Button

    private val key = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.btn)
        controller = Controller()
        btn.setOnClickListener {
            for (i in 0 until 200) {
                controller.start(object : MainCallback(key) {
                    override fun onCallback(seq: Int) {
                        Log.i("MainActivity", "onCallback $seq")
                    }
                })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i("MainActivity", "remove all callback")
    }

    override fun onDestroy() {
        Log.i("MainActivity", "onDestroy")
        controller.removeCallback(key)
        super.onDestroy()
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controller = Controller()

        controller.startCallback(object : Callback {
            override fun onCallback(seq: Int) {
                // login and password
                controller.removeCallback()
            }
        })
    }*/

}



interface Callback {
    fun onCallback(seq: Int)
}

abstract class MainCallback(val key: String) : Callback
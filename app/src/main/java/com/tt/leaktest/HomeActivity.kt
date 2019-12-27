package com.tt.leaktest

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class HomeActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val r = Runnable {
        btn.text = "点击"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler.postDelayed(r, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(r)
    }

}
package com.tt.leaktest

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

class LeakTestActivity :AppCompatActivity() {

    internal lateinit var btn:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn = findViewById(R.id.btn)
    }



    class Callback(activity: Activity):Callbacks{
        private val activityRef = WeakReference(activity)

        @MainThread
        override fun onCallback() {
            val activity = activityRef.get()
            if(activity!=null){
                activity.btn.text = "OnClick"
            }
        }
    }

}

interface Callbacks{
    @MainThread
    fun onCallback()
}
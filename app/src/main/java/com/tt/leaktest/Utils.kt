package com.tt.leaktest

import android.content.Context

object Utils {
    private lateinit var context: Context

    fun init(context: Context){
        this.context = context
    }
}
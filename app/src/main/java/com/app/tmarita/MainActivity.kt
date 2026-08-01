package com.app.tmarita

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.tmarita.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // El fragment ya está declarado en el XML vía android:name, no hace falta
        // hacer supportFragmentManager.beginTransaction() manualmente.
    }
}
package com.muggy8.spell_check_writer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var primaryToolbar:Toolbar = findViewById(R.id.primary_toolbar)
        setSupportActionBar(primaryToolbar)
    }
}
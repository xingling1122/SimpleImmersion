package com.github.xingling.simpleimmersion

import android.app.Activity
import android.os.Bundle
import com.github.xingling.immersionlibrary.SimpleImmersion
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SimpleImmersion.with(this).titleBar(toolbar).init()
    }
}

package com.sajenko.glyphtoys

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View

class CompositeInfoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composite_info)
        findViewById<View>(R.id.compositeInfoRoot).applySystemBarsPadding()
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, CompositeInfoActivity::class.java)
        }
    }
}

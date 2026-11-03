package com.woowang.scrollmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.woowang.scrollmate.ui.HomeScreen
import com.woowang.scrollmate.ui.ScrollMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrollMateTheme {
                HomeScreen()
            }
        }
    }
}

package com.woowang.scrollmate

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.woowang.scrollmate.domain.PDF_MIME_TYPE
import com.woowang.scrollmate.ui.HomeScreen
import com.woowang.scrollmate.ui.ScrollMateTheme
import com.woowang.scrollmate.ui.UploadFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrollMateTheme {
                var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenMultipleDocuments(),
                ) { pickedUris = it }

                HomeScreen(onUploadClick = { picker.launch(arrayOf(PDF_MIME_TYPE)) })

                if (pickedUris.isNotEmpty()) {
                    UploadFlow(uris = pickedUris, onFinished = { pickedUris = emptyList() })
                }
            }
        }
    }
}

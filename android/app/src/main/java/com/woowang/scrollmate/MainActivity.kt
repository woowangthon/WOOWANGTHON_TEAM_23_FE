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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.woowang.scrollmate.domain.SUPPORTED_MIME_TYPES
import com.woowang.scrollmate.ui.AccessibilityGuideScreen
import com.woowang.scrollmate.ui.HomeScreen
import com.woowang.scrollmate.ui.ScrollMateTheme
import com.woowang.scrollmate.ui.UploadFlow

class MainActivity : ComponentActivity() {
    // 설정 화면에 다녀오면 바뀔 수 있으므로 화면으로 돌아올 때마다 다시 확인함
    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceEnabled = isAccessibilityServiceEnabled(this)
        setContent {
            ScrollMateTheme {
                var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenMultipleDocuments(),
                ) { pickedUris = it }

                // 서비스가 꺼져 있으면 안내를 먼저 보여 주고, 켜지면 자동으로 닫힘. 홈에서 다시 열 수도 있음
                var guideSkipped by rememberSaveable { mutableStateOf(false) }
                var guideRequested by rememberSaveable { mutableStateOf(false) }

                if (guideRequested || (!serviceEnabled && !guideSkipped)) {
                    AccessibilityGuideScreen(
                        enabled = serviceEnabled,
                        onOpenSettings = { startActivity(accessibilitySettingsIntent()) },
                        onDismiss = {
                            guideSkipped = true
                            guideRequested = false
                        },
                    )
                } else {
                    HomeScreen(
                        onUploadClick = { picker.launch(SUPPORTED_MIME_TYPES) },
                        onGuideClick = { guideRequested = true },
                    )

                    if (pickedUris.isNotEmpty()) {
                        UploadFlow(uris = pickedUris, onFinished = { pickedUris = emptyList() })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isAccessibilityServiceEnabled(this)
    }
}

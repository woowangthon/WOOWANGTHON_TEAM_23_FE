package com.woowang.scrollmate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import com.woowang.scrollmate.ui.ScrollMateTheme
import com.woowang.scrollmate.ui.UploadFlow

// 다른 앱에서 PDF를 공유하면 이 화면이 투명하게 뜨고 올리기 시트만 보여 줌.
// 보던 앱을 떠나지 않고 올릴 수 있게 하려는 것
class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = sharedUris(intent)
        setContent {
            ScrollMateTheme {
                UploadFlow(uris = uris, onFinished = { finish() })
            }
        }
    }

    private fun sharedUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND ->
            listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        else -> emptyList()
    }
}

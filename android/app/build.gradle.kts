plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.woowang.scrollmate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.woowang.scrollmate"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        // 디버그 빌드는 개발 서버(기본값은 에뮬레이터에서 PC를 가리키는 주소)에 붙음
        debug {
            val apiBaseUrl = providers.gradleProperty("apiBaseUrl").get()
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        // 릴리스 빌드(제출용 APK)는 배포 서버에 붙음. 릴리스 빌드는 http를 허용하지 않으므로 https 주소여야 함
        release {
            val releaseApiBaseUrl = providers.gradleProperty("releaseApiBaseUrl").get()
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            // 제출용 APK를 바로 설치할 수 있게 디버그 키로 서명함. 스토어 배포용 키가 아님
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("androidx.work:work-runtime-ktx:2.10.5")

    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    // 컴파일러가 Kotlin 2.2.x라서 Kotlin 2.4로 빌드된 최신 버전은 쓸 수 없음 (메타데이터 버전 불일치)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.1")
}

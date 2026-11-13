plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.beouttv.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beouttv.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 9
        versionName = "1.6.0"
        buildConfigField("String", "API_BASE", "\"${providers.gradleProperty("apiBase").get()}\"")
        // الاحتفاظ باللغة العربية والإنجليزية فقط (يقلل موارد المكتبات)
        resourceConfigurations += listOf("ar", "en")
    }

    // المحرك البصري Native (C++/CMake/JNI): يُفعَّل تلقائياً إن وُجد NDK (ANDROID_NDK_HOME أو ndk.dir)،
    // أو قسراً بـ -Pbeout.native=true / تعطيله بـ -Pbeout.native=false. بدونه يعمل التطبيق بنسخة Kotlin المكافئة.
    val localProps = rootProject.file("local.properties").takeIf { it.exists() }?.readLines().orEmpty()
    val nativeFlag = providers.gradleProperty("beout.native").orNull
    val nativeOn = when (nativeFlag) {
        "true" -> true
        "false" -> false
        else -> System.getenv("ANDROID_NDK_HOME") != null || System.getenv("ANDROID_NDK_ROOT") != null ||
            localProps.any { it.startsWith("ndk.dir") || it.startsWith("sdk.dir") && File(it.substringAfter('=').trim(), "ndk").exists() }
    }
    if (nativeOn) {
        defaultConfig {
            // ABI واحد لكل بناء إن أردت (مثلاً -Pbeout.abis=arm64-v8a) لتصغير الـ APK
            val abis = (providers.gradleProperty("beout.abis").orNull ?: "arm64-v8a,armeabi-v7a,x86_64,x86").split(",")
            ndk { abiFilters += abis }
            externalNativeBuild { cmake { arguments += listOf("-DANDROID_STL=none") } }
        }
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // استبدله بمفتاح الإصدار الخاص بك
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    // Foundation + UI فقط. لا Material ولا أيقونات ممتدة.
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Media3: المحرك + HLS + DASH فقط (بدون media3-ui وبدون وحدات إضافية)
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
}

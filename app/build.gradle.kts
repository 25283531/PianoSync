plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release 签名：优先读取环境变量（CI/生产），否则读取项目根目录的 keystore.properties，
// 都没有时回退到 debug 密钥，保证 release 构建始终可产出可安装的 APK。
val keystoreProps = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun keystoreValue(env: String, prop: String): String? =
    System.getenv(env) ?: keystoreProps.getProperty(prop)

val releaseStoreFile = keystoreValue("KEYSTORE_FILE", "storeFile")

android {
    namespace = "io.pianosync.midi"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.pianosync.midi"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "0.5.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = file(releaseStoreFile)
                storePassword = keystoreValue("KEYSTORE_STORE_PASSWORD", "storePassword")
                keyAlias = keystoreValue("KEYSTORE_KEY_ALIAS", "keyAlias")
                keyPassword = keystoreValue("KEYSTORE_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 配置了生产密钥则用 release 签名，否则回退 debug 签名以产出可安装 APK
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.json.json)
    implementation("com.github.pdrogfer:MidiDroid:1.3")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
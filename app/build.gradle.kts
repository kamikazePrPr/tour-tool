import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kamikazeprpr.tourtool"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kamikazeprpr.tourtool"
        minSdk = 24
        targetSdk = 35
        versionCode = 19
        versionName = "0.0.14"
        resourceConfigurations += listOf("zh", "zh-rCN")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 绛惧悕閰嶇疆鍦ㄦ湰鍦?signing.properties 閲岋紝涓嶈繘浠撳簱
            val propsFile = rootProject.file("signing.properties")
            if (propsFile.exists()) {
                val props = Properties()
                propsFile.inputStream().use { props.load(it) }
                signingConfigs.create("release") {
                    storeFile = file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

ksp {
    // Room 鐨?schema 瀵煎嚭鍒?app/schemas锛屾敼琛ㄧ粨鏋勬椂鑳藉湪 diff 閲岀湅瑙?    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 鎵撳寘鐗堜腑鏂?OCR锛氭ā鍨嬪湪 APK 鍐咃紝涓嶉渶瑕?Google 鏈嶅姟锛岄璺戜笉鑱旂綉
    implementation(libs.mlkit.text.chinese)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling.preview)
}


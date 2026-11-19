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
        versionCode = 4
        versionName = "0.4.0"
        resourceConfigurations += listOf("zh")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 签名配置在本地 signing.properties 里，不进仓库
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
    // Room 的 schema 导出到 app/schemas，改表结构时能在 diff 里看见
    arg("room.schemaLocation", "$projectDir/schemas")
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

    // 打包版中文 OCR：模型在 APK 内，不需要 Google 服务，首跑不联网
    implementation(libs.mlkit.text.chinese)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling.preview)
}

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------
// 签名：私钥和密码都在 signing/ 里，该目录不进版本库。
// 参考 signing/keystore.properties.example 自行配置。
// ---------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("signing/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val releaseKeystore = rootProject.file(
    keystoreProps.getProperty("storeFile", "signing/ipuser.jks"),
)
val hasAuthorKey = releaseKeystore.exists()

android {
    namespace = "com.functionbuilder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.functionbuilder"
        minSdk = 24
        targetSdk = 36
        versionCode = 100006
        versionName = "0.50b-native"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 作者私钥放在 signing/ 下（已 gitignore，不入库）。
    // 没有私钥时（别人 clone 下来 / CI 环境）自动退回 debug 签名，保证仓库开箱即可构建。
    signingConfigs {
        if (hasAuthorKey) {
            create("author") {
                storeFile = releaseKeystore
                storePassword = keystoreProps.getProperty("storePassword", "")
                keyAlias = keystoreProps.getProperty("keyAlias", "")
                keyPassword = keystoreProps.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (hasAuthorKey) "author" else "debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (hasAuthorKey) "author" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // 素材来自原版工程，个别 lint 规则会误报；这里只提示不阻断构建。
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.documentfile)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    // JVM 单元测试里 android.jar 的 org.json 只是抛异常的桩，这里换成真实实现
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
}

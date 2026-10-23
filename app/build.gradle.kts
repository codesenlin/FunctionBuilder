import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------
// 签名分两条路：
//   debug   —— 用旧密钥 ipuser 直接签，方便在已装旧版的机器上反复覆盖调试；
//   release —— AGP 只产出未签名包，交给 apksigner 用「新密钥 + 轮换链」签
//              v1 + v2 + v3，这样 Android 9+ 可以从旧证书平滑升级到新证书。
// 配置见 signing/keystore.properties（旧密钥）和 signing/rotation.properties（新密钥 + lineage）。
// ---------------------------------------------------------------------------
fun loadProps(path: String): Properties = Properties().apply {
    val f = rootProject.file(path)
    if (f.exists()) f.inputStream().use { load(it) }
}

val keystoreProps = loadProps("signing/keystore.properties")
val releaseKeystore = rootProject.file(keystoreProps.getProperty("storeFile", "signing/ipuser.jks"))
val hasAuthorKey = releaseKeystore.exists()

val rotationProps = loadProps("signing/rotation.properties")
val rotationKeystore = rootProject.file(rotationProps.getProperty("storeFile", "signing/release-v3.jks"))
val rotationLineage = rootProject.file(rotationProps.getProperty("lineage", "signing/lineage"))
val hasRotationKey = rotationKeystore.exists() && rotationLineage.exists()
// 轮换签名需要两把钥匙：旧证书签 v1/v2，新证书签 v3
val hasRotationSetup = hasRotationKey && hasAuthorKey

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
            // 未签名输出，由下面的 signReleaseV3 任务用 apksigner + lineage 签。
            // 没有轮换密钥时退回普通签名，保证仓库开箱可构建。
            signingConfig = if (hasRotationSetup) null
            else signingConfigs.getByName(if (hasAuthorKey) "author" else "debug")
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

// ---------------------------------------------------------------------------
// V3 密钥轮换签名
//
// AGP 只会用单个 keystore 出包，不支持 v3 的 rotation lineage，所以这里在
// assembleRelease 之后用 apksigner 重新签一次：新密钥 + 轮换链 + v1/v2/v3 全开。
// 这样 Android 9 (API 28) 以上可以从旧证书平滑升级，旧设备仍按老规矩只认原证书。
// ---------------------------------------------------------------------------
val apksignerJar: File? = File(android.sdkDirectory, "build-tools")
    .listFiles()
    ?.filter { it.isDirectory }
    ?.sortedByDescending { it.name }
    ?.map { File(it, "lib/apksigner.jar") }
    ?.firstOrNull { it.isFile }

val signReleaseV3 by tasks.registering(JavaExec::class) {
    group = "build"
    description = "用新密钥 + 轮换链给 release APK 签名（v1 + v2 + v3）"

    val outDir = layout.buildDirectory.dir("outputs/apk/release")
    val unsignedApk = outDir.get().file("app-release-unsigned.apk").asFile
    val signedApk = outDir.get().file("app-release.apk").asFile

    onlyIf { hasRotationSetup && apksignerJar != null }
    dependsOn("assembleRelease")

    if (hasRotationSetup && apksignerJar != null) {
        classpath = files(apksignerJar)
        mainClass.set("com.android.apksigner.ApkSignerTool")
        args = listOf(
            "sign",
            // 最老的签名者：v1（以及不支持轮换的老平台上的 v2）由它负责
            "--ks", releaseKeystore.absolutePath,
            "--ks-pass", "pass:${keystoreProps.getProperty("storePassword", "")}",
            "--ks-key-alias", keystoreProps.getProperty("keyAlias", "ipuser"),
            "--key-pass", "pass:${keystoreProps.getProperty("keyPassword", "")}",
            // 当前签名者：v3 用它，并携带轮换链
            "--next-signer",
            "--ks", rotationKeystore.absolutePath,
            "--ks-pass", "pass:${rotationProps.getProperty("storePassword", "")}",
            "--ks-key-alias", rotationProps.getProperty("keyAlias", ""),
            "--key-pass", "pass:${rotationProps.getProperty("keyPassword", "")}",
            "--lineage", rotationLineage.absolutePath,
            "--v1-signing-enabled", "true",
            "--v2-signing-enabled", "true",
            "--v3-signing-enabled", "true",
            "--v4-signing-enabled", "false",
            "--out", signedApk.absolutePath,
            unsignedApk.absolutePath,
        )
        doFirst {
            if (!unsignedApk.isFile) error("没找到未签名 APK：$unsignedApk")
            signedApk.delete()
        }
        doLast {
            logger.lifecycle("已签名（v1+v2+v3 + 轮换链）：${signedApk.absolutePath}")
        }
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    if (hasRotationSetup) finalizedBy(signReleaseV3)
}

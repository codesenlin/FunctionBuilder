// ---------------------------------------------------------------------------
// 国内网络加速（默认关闭，不影响 CI 与海外用户）：
//   ./gradlew -DcnMirror=true assembleDebug
// Gradle 本体由 gradle/wrapper/gradle-wrapper.properties 里的 distributionUrl 决定，
// 换成腾讯源可显著加快首次下载：
//   https://mirrors.cloud.tencent.com/gradle/gradle-8.14.5-bin.zip
// ---------------------------------------------------------------------------

pluginManagement {
    val cnMirror = System.getProperty("cnMirror") == "true"
    repositories {
        if (cnMirror) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val cnMirror = System.getProperty("cnMirror") == "true"
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (cnMirror) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "FunctionBuilder"
include(":app")

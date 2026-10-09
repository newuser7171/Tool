plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.jev.toolkit"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.jev.toolkit"
        minSdk = 26
        targetSdk = 28
        versionCode = 5
        versionName = "0.5"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { arguments += "-DJEV_WITH_IMGUI=ON" } }
    }
    externalNativeBuild { cmake { path = file("../native/CMakeLists.txt"); version = "3.22.1" } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

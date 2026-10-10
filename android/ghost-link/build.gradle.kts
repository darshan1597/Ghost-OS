plugins { id("com.android.application") }
android {
    namespace = "com.ghostos.link.receiver"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ghostos.link.receiver"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.3.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main").java.srcDir("../link-shared/src/main/kotlin")
}
kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }

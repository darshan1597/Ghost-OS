plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.ghostos.ghost_os"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.ghostos.ghost_os"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 26
        testInstrumentationRunner = "com.ghostos.ghost_os.voice.VoiceInstrumentation"
        // Match packaged JNI libraries to Flutter's requested targets. Local JNI folders
        // otherwise bundle every ABI even when --target-platform android-arm64 is used.
        ndk {
            abiFilters += (project.findProperty("target-platform") as String?)
                ?.split(",")?.mapNotNull { when (it) {
                    "android-arm64" -> "arm64-v8a"
                    "android-arm" -> "armeabi-v7a"
                    "android-x64" -> "x86_64"
                    else -> null
                } } ?: listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildFeatures { buildConfig = true }
    // Read the large neural model directly from the APK; only eSpeak data is extracted.
    androidResources { noCompress += listOf("onnx", "mp3") }
    // Flutter's debug plugin adds all ABIs after defaultConfig. Explicit packaging
    // exclusions also cover the local neural libraries for a single-target APK.
    val requestedTargets = (project.findProperty("target-platform") as String?)?.split(",")
    if (requestedTargets != null) {
        mapOf("android-arm64" to "arm64-v8a", "android-arm" to "armeabi-v7a", "android-x64" to "x86_64")
            .filterKeys { it !in requestedTargets }.values.forEach { abi ->
                packaging.jniLibs.excludes += "**/$abi/*.so"
            }
    }
    sourceSets.getByName("main").java.srcDir("../link-shared/src/main/kotlin")
    flavorDimensions += "capabilities"
    productFlavors {
        create("standard") {
            dimension = "capabilities"
            buildConfigField("boolean", "GHOST_WHATSAPP_AUTOMATION", "false")
        }
        create("automation") {
            dimension = "capabilities"
            buildConfigField("boolean", "GHOST_WHATSAPP_AUTOMATION", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}

dependencies {
    implementation(project(":llama-ai"))
    implementation("com.alphacephei:vosk-android:0.3.75@aar")
    implementation("net.java.dev.jna:jna:5.18.1@aar")
    testImplementation("junit:junit:4.13.2")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Give Kotlin/Gradle one canonical output path when this Mac uses the SSD cache.
providers.environmentVariable("NIVORYN_ANDROID_BUILD_DIR").orNull?.let {
    layout.buildDirectory.set(file(it))
}

android {
    namespace = "dev.kyzenn.nivoryn"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "dev.kyzenn.nivoryn"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Public early-access artifact: optimized release code, same preview certificate
        // so existing installs can update without losing app-private history/models.
        create("preview") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            ndk { abiFilters += "arm64-v8a" }
        }
    }
}

dependencyLocking { lockAllConfigurations() }

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("org.pytorch:executorch-android:1.2.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("com.google.zxing:core:3.5.4")
    // Bundled OCR models: first use works without Play Services downloads.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.01"))
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// Explicit opt-in evaluator: external labelled data is never required by the unit-test suite.
tasks.register<JavaExec>("evaluateSmsCorpus") {
    dependsOn("compileDebugUnitTestKotlin")
    mainClass.set("dev.kyzenn.nivoryn.SmsCorpusEvaluation")
    classpath = files(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"), layout.buildDirectory.dir("tmp/kotlin-classes/debug")) + configurations.getByName("debugUnitTestRuntimeClasspath")
    doFirst {
        args(providers.gradleProperty("smsCorpus").get(), providers.gradleProperty("evaluationOutput").get())
    }
}

tasks.register<JavaExec>("writeAdvisoryPromptFixture") {
    dependsOn("compileDebugUnitTestKotlin")
    mainClass.set("dev.kyzenn.nivoryn.AdvisoryPromptFixture")
    classpath = files(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"), layout.buildDirectory.dir("tmp/kotlin-classes/debug")) + configurations.getByName("debugUnitTestRuntimeClasspath")
    doFirst { args(providers.gradleProperty("promptOutput").get()) }
}

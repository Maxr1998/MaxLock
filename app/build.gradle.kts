import java.util.Properties
import com.android.builder.model.SigningConfig

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    //id("com.jakewharton.hugo")
}

val keyStoreFile = project.file("keyStore.properties")

android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "de.Maxr1998.xposed.maxlock"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 56
        versionName = "7.0.4"
        resConfigs(*Config.languages)
        setProperty("archivesBaseName", "ML-$versionName")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        vectorDrawables.useSupportLibrary = true
    }
    sourceSets {
        getByName("main") {
            res.srcDir("src/main/res/translations")
        }
    }
    lintOptions {
        isAbortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    val signingConfigMain = if (keyStoreFile.exists()) {
        val props = Properties().apply {
            load(keyStoreFile.inputStream())
        }
        Config.crowdinKey = props["crowdinKey"].toString()
        signingConfigs.create("main") {
            storeFile = project.file(props["keyStore"].toString())
            storePassword = props["keyStorePassword"].toString()
            keyAlias = props["keyAlias"].toString()
            keyPassword = props["keyPassword"].toString()
        }
    } else null
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigMain
        }
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules-debug.pro")
            signingConfig = signingConfigMain
            versionNameSuffix = "-beta"
        }
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    androidExtensions {
        isExperimental = true
    }
}

repositories {
    flatDir {
        dir("libs")
    }
}

dependencies {
    // Kotlin stdlib & coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Config.kotlinVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.0.1")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("androidx.browser:browser:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.fragment:fragment:1.1.0-alpha02")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.0.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0-alpha01")
    implementation("com.google.android.material:material:1.1.0-alpha02")
    //-- KTX
    implementation("androidx.core:core-ktx:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.0.0")

    // From JCenter or other repositories
    //-- 3rd-party
    implementation("com.anjlab.android.iab.v3:library:1.0.38@aar")
    implementation("de.Maxr1998.android:modernpreferences:0.1")
    implementation("com.crossbowffs.remotepreferences:remotepreferences:0.5")
    implementation("com.github.attenzione:android-ColorPickerPreference:e3aa301016")
    implementation("com.heinrichreimersoftware:material-intro:1.6")
    implementation("com.twofortyfouram:android-plugin-api-for-locale:1.0.2")
    implementation("commons-io:commons-io:2.5")
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")

    // Local files
    implementation("", "PatternLock", ext = "aar")

    // Testing dependencies
    androidTestUtil("androidx.test:orchestrator:1.1.1-beta01")
    androidTestImplementation("androidx.test:rules:1.1.1-beta01")
    androidTestImplementation("androidx.test:runner:1.1.1-beta01")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0-alpha4") {
        exclude("com.google.code.findbugs", "jsr305")
    }
    androidTestImplementation("com.github.Zhuinden:espresso-helper:0.1.3") {
        exclude("com.google.code.findbugs", "jsr305")
    }
}

tasks.register("updateTranslations") {
    doLast {
        Config.crowdinKey ?: return@doLast
        assert(Config.languages.size == Config.crowdinLanguages.size) {
            "Language arrays are of different length!"
        }
        for (i in Config.languages.indices) {
            val lang = Config.languages[i]
            val crowdinLang = Config.crowdinLanguages[i]
            for (fileName in arrayOf("strings", "implementation_strings")) {
                val outputFile = "\$PWD/src/main/res/translations/values-$lang/$fileName.xml"
                exec {
                    executable = "bash"
                    args("-c", "wget -O \"$outputFile\" \"https://api.crowdin.com/api/project/MaxLock/export-file?file=$fileName.xml&language=$crowdinLang&key=${Config.crowdinKey}\"")
                }
            }
        }
    }
}
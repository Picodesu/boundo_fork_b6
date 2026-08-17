import com.cliuff.boundo.build.getCustomConfig

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.aboutLibraries)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

aboutLibraries {
    collect.configPath = file("config")
}

android {
    var verInc = 0
    var verCommit: String? = null
    val isIncVer = (findProperty("ver.inc") as? String)?.toBooleanStrictOrNull()
    if (isIncVer == true) {
        val commitIncOutput = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText
        val commitHashOutput = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText
        verInc = commitIncOutput.get().trim().toInt()
        verCommit = commitHashOutput.get().trim()
    }

    buildToolsVersion = "36.0.0"
    val customConfig = getCustomConfig(project)
    val buildPackage = customConfig.buildPackage
    val configSigning = customConfig.signing != null
    signingConfigs {
        customConfig.signing?.run {
            create("Sign4Release") {
                keyAlias = key.alias
                keyPassword = key.password
                storeFile = rootProject.file(store.path)
                storePassword = store.password
            }
        }
    }
    namespace = "com.madness.collision"
    compileSdk = 36
    defaultConfig {
        manifestPlaceholders["buildPackage"] = buildPackage
        applicationId = "com.madness.collision"
        minSdk = 23
        targetSdk = 36
        versionCode = 26070800 + (verInc % 540)
        versionName = listOfNotNull("5.1.2", verCommit).joinToString(separator = "-")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "${applicationId}.test"
        renderscriptSupportModeEnabled = true
        if (configSigning) {
            signingConfig = signingConfigs.getByName("Sign4Release")
        }
        buildConfigField("String", "BUILD_PACKAGE", "\"$buildPackage\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "0")
        resValue("string", "buildPackage", buildPackage)
        androidResources.localeFilters.addAll(arrayOf(
            "ar",
            "bn", "bn-BD",
            "de", "de-DE",
            "el", "el-GR",
            "en", "en-GB", "en-US",
            "es", "es-ES", "es-US",
            "fa", "fa-AF", "fa-IR",
            "fr", "fr-FR",
            "hi", "hi-IN",
            "in", "in-ID",
            "it", "it-IT",
            "ja", "ja-JP",
            "ko", "ko-KR",
            "mr", "mr-IN",
            "pa", "pa-PK",
            "pl", "pl-PL",
            "pt", "pt-PT",
            "ru", "ru-RU",
            "th", "th-TH",
            "tr", "tr-TR",
            "uk", "uk-UA",
            "vi", "vi-VN",
            "zh", "zh-CN", "zh-HK", "zh-MO", "zh-SG", "zh-TW",
        ))
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".mortal"
            isDebuggable = true
            isJniDebuggable = false
            if (configSigning) {
                signingConfig = signingConfigs.getByName("Sign4Release")
            }
            renderscriptOptimLevel = 3
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            isDebuggable = false
            isJniDebuggable = false
            if (configSigning) {
                signingConfig = signingConfigs.getByName("Sign4Release")
            }
            renderscriptOptimLevel = 3
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("long", "BUILD_TIMESTAMP", System.currentTimeMillis().toString())
        }
        create("foss") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            buildConfigField("long", "BUILD_TIMESTAMP", "0")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    kotlin.jvmToolchain(17)
    packaging {
        resources.excludes.add("DebugProbesKt.bin")
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    buildFeatures {
        buildConfig = true
        resValues = true
        viewBinding = true
        compose = true
    }
    dynamicFeatures.add(":api_viewing")
    bundle {
        language.enableSplit = false
    }
}

dependencies {

    coreLibraryDesugaring(libs.androidDesugaring)

    implementation(project(":mods:core"))
    implementation(platform(libs.androidxComposeBom))
    // OPPO seamless animation (compileOnly per official docs)
    compileOnly("com.oplus.animation:viewseamless:1.0.0@aar")
    listOf(
        fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))),
        libs.androidxCore,
        libs.androidxCoreKtx,
        libs.androidxComposeRuntimeLiveData,
        libs.androidxComposeFoundation,
        libs.androidxComposeUi,
        libs.androidxComposeActivity,
        libs.androidxComposeMaterial3,
        libs.androidxComposeMaterialIcons,
        libs.androidxComposeMaterialIconsExtended,
        libs.androidxComposeAnimation,
        libs.androidxComposeUiToolingPreview,
        libs.androidxComposeViewModel,
        libs.androidxActivity,
        libs.androidxAppcompat,
        libs.androidxFragment,
        libs.androidxFragmentCompose,
        libs.androidxWindow,
        libs.androidx.material3.adaptive,
        libs.androidx.material3.adaptive.layout,
        libs.androidx.material3.adaptive.navigation,
        libs.androidxDrawerLayout,
        libs.androidxSwipeRefreshLayout,
        libs.androidxConstraintLayout,
        libs.androidxPalette,
        libs.androidxCardView,
        libs.androidxRecyclerView,
        libs.androidxViewPager,
        libs.androidxLifecycleRuntime,
        libs.androidxLifecycleRuntimeCompose,
        libs.androidxLifecycleCommon,
        libs.androidxLifecycleViewModel,
        libs.androidxLifecycleLiveData,
        libs.androidxPaging,
        libs.androidxPreference,
        libs.androidxNavigationFragment,
        libs.androidxNavigationUI,
        libs.androidxDocumentFile,
        libs.androidxHeifWriter,
        libs.googleMaterialComponents,
        libs.googleGson,
        libs.gglGuava,
        libs.kotlinStdlib,
        libs.kotlinCoroutines,
        libs.rxJava,
        libs.jbAnnotations,
        libs.okhttp,
        libs.coil,
        libs.coilCompose,
        libs.androidDeviceNames,
        libs.appIconLoader,
        libs.smoothCornerCompose,
        libs.haze,
        libs.aboutlibraries.core,
        libs.aboutlibraries.compose.m3,
    ).forEach { implementation(it) }

    debugImplementation(libs.androidxComposeUiTooling)

    listOf(libs.mockito, libs.googleTruth, libs.googleTruthExtensions, libs.junit4).forEach { testImplementation(it) }

}

tasks.register<Copy>("genUniversalApks") {
    from(zipTree(file("build/outputs/app-universal-release.apks")))
    into(file("build/outputs/apks/release"))
    tasks["printBundleToolVersion"].mustRunAfter("buildUniversalApks")
    dependsOn("buildUniversalApks", "printBundleToolVersion")
}

tasks.register<Copy>("genFossApks") {
    from(zipTree(file("build/outputs/app-universal-foss.apks")))
    into(file("build/outputs/apks/foss"))
    tasks["printBundleToolVersion"].mustRunAfter("buildFossApks")
    dependsOn("buildFossApks", "printBundleToolVersion")
}

tasks.register<Exec>("buildUniversalApks") {
    dependsOn("bundleRelease")
    val bundleToolPath = providers.exec {
        commandLine("find", "build-tools", "-name", "bundletool.jar", "-print", "-quit")
    }.standardOutput.asText.get().trim()
    commandLine("java", "-jar", bundleToolPath, "build-apks",
        "--bundle=app/build/outputs/bundle/release/app-release.aab",
        "--output=app/build/outputs/app-universal-release.apks",
        "--ks=doconfig/keystore.jks",
        "--ks-pass=pass:",
        "--ks-key-alias=",
        "--key-pass=pass:",
        "--mode=universal")
    doFirst {
        val signing = getCustomConfig(project).signing
        if (signing != null) {
            environment("SIGNING_KEYSTORE_PASSWORD", signing.store.password)
            environment("SIGNING_KEY_ALIAS", signing.key.alias)
            environment("SIGNING_KEY_PASSWORD", signing.key.password)
        }
    }
}

tasks.register<Exec>("buildFossApks") {
    dependsOn("bundleFoss")
    val bundleToolPath = providers.exec {
        commandLine("find", "build-tools", "-name", "bundletool.jar", "-print", "-quit")
    }.standardOutput.asText.get().trim()
    commandLine("java", "-jar", bundleToolPath, "build-apks",
        "--bundle=app/build/outputs/bundle/foss/app-foss.aab",
        "--output=app/build/outputs/app-universal-foss.apks",
        "--ks=doconfig/keystore.jks",
        "--ks-pass=pass:",
        "--ks-key-alias=",
        "--key-pass=pass:",
        "--mode=universal")
    doFirst {
        val signing = getCustomConfig(project).signing
        if (signing != null) {
            environment("SIGNING_KEYSTORE_PASSWORD", signing.store.password)
            environment("SIGNING_KEY_ALIAS", signing.key.alias)
            environment("SIGNING_KEY_PASSWORD", signing.key.password)
        }
    }
}
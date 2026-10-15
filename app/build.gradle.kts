plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.spotless)
}

val releaseKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
val releaseSigningValues =
  listOf(releaseKeystorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val releaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }
require(releaseSigningValues.none { !it.isNullOrBlank() } || releaseSigningConfigured) {
  "Release signing requires KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD together"
}

val releaseVersionCode =
  providers.environmentVariable("THAI_CLOCK_FIX_VERSION_CODE").orNull?.toIntOrNull() ?: 1
require(releaseVersionCode > 0) { "THAI_CLOCK_FIX_VERSION_CODE must be a positive integer" }
val releaseVersionName =
  providers.environmentVariable("THAI_CLOCK_FIX_VERSION_NAME").orNull?.takeIf { it.isNotBlank() }
    ?: "1.0.0"
val coverageEnabled = providers.gradleProperty("coverage").map(String::toBoolean).getOrElse(false)

android {
  namespace = "com.thaiclockfix"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.thaiclockfix"
    minSdk = 26
    targetSdk = 37
    versionCode = releaseVersionCode
    versionName = releaseVersionName
  }

  signingConfigs {
    if (releaseSigningConfigured) {
      create("release") {
        storeFile = file(requireNotNull(releaseKeystorePath))
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (releaseSigningConfigured) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      // Coverage instrumentation slows normal builds, so keep it opt-in.
      enableUnitTestCoverage = coverageEnabled
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      if (coverageEnabled) {
        all { test ->
          // JaCoCo must count Robolectric sandbox classes, otherwise every
          // Robolectric-run class reports 0% coverage.
          test.extensions.configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
          }
        }
      }
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.shizuku.api)
  implementation(libs.shizuku.provider)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

spotless {
  kotlin {
    target("src/main/**/*.kt", "src/test/**/*.kt", "src/androidTest/**/*.kt")
    ktlint("1.8.0").editorConfigOverride(
      mapOf(
        // Compose convention: @Composable functions are PascalCase by design.
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
        // Test files deliberately use kotlinx.coroutines.* style wildcard imports.
        "ktlint_standard_no-wildcard-imports" to "disabled"
      )
    )
  }
  kotlinGradle {
    target("*.gradle.kts")
  }
}

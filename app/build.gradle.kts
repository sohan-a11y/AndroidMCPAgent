import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun signingValue(propertyKey: String, envKey: String): String? {
    val propertyValue = keystoreProperties.getProperty(propertyKey)?.trim()
    if (!propertyValue.isNullOrEmpty()) return propertyValue
    val envValue = System.getenv(envKey)?.trim()
    return envValue?.takeIf { it.isNotEmpty() }
}

val releaseStoreFile = signingValue("storeFile", "ANDROID_AI_MCP_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "ANDROID_AI_MCP_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "ANDROID_AI_MCP_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "ANDROID_AI_MCP_KEY_PASSWORD")
val ciVersionCode = providers.gradleProperty("ciVersionCode").orNull?.toIntOrNull()
val ciSignDebug = providers.gradleProperty("ciSignDebug").orNull?.toBooleanStrictOrNull() ?: false

val hasReleaseSigningConfig = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.android.ai.mcp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.ai.mcp"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode ?: 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningConfig) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Use default debug signing (auto-generated debug.keystore)
            // This is usually automatic, but being explicit helps CI
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            if (ciSignDebug && hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Storage
    implementation(libs.security.crypto)
    implementation(libs.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Compose + AndroidX
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Tests
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))

    debugImplementation(libs.compose.ui.tooling)
}

gradle.taskGraph.whenReady {
    val requiresReleaseSigning = allTasks.any { task ->
        task.name.contains("Release", ignoreCase = true)
    }

    if (requiresReleaseSigning && !hasReleaseSigningConfig) {
        throw GradleException(
            "Release signing config missing. Provide keystore.properties or env vars: " +
                "ANDROID_AI_MCP_STORE_FILE, ANDROID_AI_MCP_STORE_PASSWORD, " +
                "ANDROID_AI_MCP_KEY_ALIAS, ANDROID_AI_MCP_KEY_PASSWORD."
        )
    }
}

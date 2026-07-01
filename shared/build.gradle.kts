plugins {
    id("com.android.library")
    kotlin("android")
    id("app.cash.sqldelight")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.fieldcrm.shared"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Ktor Client for API requests
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-okhttp:2.3.8")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
    
    // SQLDelight common DB API & Android Driver
    implementation("app.cash.sqldelight:runtime:2.0.1")
    implementation("app.cash.sqldelight:android-driver:2.0.1")
    
    // Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.fieldcrm.shared.db")
        }
    }
}

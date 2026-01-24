plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// =============================================================================
// Station Build Task - generates RadioRepository.kt from stations.yaml
// =============================================================================
val buildStations = tasks.register<Exec>("buildStations") {
    description = "Build radio station resources from stations.yaml"
    group = "build"
    workingDir = rootProject.projectDir

    inputs.file("${rootProject.projectDir}/stations.yaml")
    inputs.file("${rootProject.projectDir}/scripts/requirements.txt")
    inputs.dir("${rootProject.projectDir}/scripts")
    outputs.file("${projectDir}/src/main/java/org/guakamole/onair/data/RadioRepository.kt")

    // Use system Python directly - dependencies should be installed system-wide or via pip
    // For venv support, run: python3 -m venv .venv && .venv/bin/pip install -r scripts/requirements.txt
    commandLine("python3", "scripts/build_stations.py")
}

tasks.named("preBuild") {
    dependsOn(buildStations)
}
// =============================================================================


android {
    namespace = "org.guakamole.onair"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.guakamole.onair"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }
    
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.6.0")
    
    // Media3 (ExoPlayer) - using 1.0.2 for SDK 33 compatibility
    implementation("androidx.media3:media3-exoplayer:1.0.2")
    implementation("androidx.media3:media3-session:1.0.2")
    implementation("androidx.media3:media3-ui:1.0.2")
    implementation("androidx.media3:media3-exoplayer-hls:1.0.2")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-svg:2.4.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

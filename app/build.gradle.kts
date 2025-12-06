plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Supabase configuration
        buildConfigField("String", "SUPABASE_URL", "\"https://rnsivwuxmekckvqwonaw.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJuc2l2d3V4bWVrY2t2cXdvbmF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkwNzE3NjIsImV4cCI6MjA3NDY0Nzc2Mn0.s8ioNGxJke4G3PCf5ejPfRPtz2wtQFrWmsEYWotE6-U\"")
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5000\"") // Android emulator localhost
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Networking - Using Retrofit to call Supabase REST API
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    
    // Data Storage
    implementation(libs.androidx.datastore.preferences)
    
    // Browser - Para CustomTabs (OAuth)
    implementation(libs.androidx.browser)
    
    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    
    // Room - Commented temporarily, not needed for initial build
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
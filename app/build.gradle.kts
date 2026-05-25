plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt")           // Для Room и других annotation processors
    id("kotlin-parcelize")      // Для @Parcelize
}

android {
    namespace = "com.example.check_911"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.check_911"
        minSdk = 26 //24
        targetSdk = 34
        versionCode = 39 //08.04.26 //37 //07.04.26 //36 //13.03.26 //35 //11.03.26 //34 //26.02.26 //33 //11.02.26 //32 //30.12.25 //31 //24.11.25 //30 //12.11.25 //29 //17.10.25//26 //25 //08.10.25 //23 //07.10.25 //21 //06.10.25 //20 //30.09.25 //19 //29.09.25 //18 //24.09.25 //17 //23.09.25 //16 //13.09.25 //14 //10.09.25 //13 //04.09.25 //12// 01.09.25 //11 //13.08.25 //10 //11.08.25 // 9 //29.07.25
        versionName = "15.04.26"

        // дата версии
        buildConfigField("String", "DATE_VERSION", "\"15.04.26\"")
        buildConfigField("Boolean", "DEBUG_VERSION", "true")
//        buildConfigField("Boolean", "DEBUG_VERSION", "false")

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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {



    implementation ("androidx.camera:camera-core:1.3.4")
    implementation ("androidx.camera:camera-camera2:1.3.4")
    implementation ("androidx.camera:camera-lifecycle:1.3.4")
    implementation ("androidx.camera:camera-view:1.3.4")

    implementation ("com.google.mlkit:barcode-scanning:17.2.0")
//    implementation ("androidx.camera:camera-core:1.3.0")
//    implementation ("androidx.camera:camera-camera2:1.3.0")
//    implementation ("androidx.camera:camera-lifecycle:1.3.0")
//    implementation ("androidx.camera:camera-view:1.3.0")
    implementation ("androidx.camera:camera-mlkit-vision:1.4.0-alpha04")



    implementation ("androidx.cardview:cardview:1.0.0")

    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//    implementation ("io.insert-koin:koin-android:3.3.2")
    implementation ("com.localebro:okhttpprofiler:1.0.8")

    implementation("com.squareup.okhttp3:okhttp:4.12.0") // стабильная версия



    implementation ("androidx.recyclerview:recyclerview:1.2.1")

    implementation ("io.insert-koin:koin-android:3.5.0")
    implementation ("io.insert-koin:koin-core:3.5.0")
//    implementation ("io.insert-koin:koin-androidx-viewmodel:3.5.0")

    implementation ("com.google.android.material:material:1.11.0")

    implementation ("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    implementation(libs.filament.android)


    kapt("androidx.room:room-compiler:2.6.1")  // KAPT обязателен для генерации MainDb_Impl
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}
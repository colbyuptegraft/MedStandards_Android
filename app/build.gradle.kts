plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.colbycoapps.med_standards"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.colbycoapps.med_standards"
        minSdk = 26
        targetSdk = 36
        versionCode = 146029
        versionName = "2.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
 //   implementation(libs.android.pdf.viewer)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("com.google.firebase:firebase-messaging:25.1.1")
    implementation("io.github.nikartm:fit-button:2.0.0")
    implementation("com.android.billingclient:billing:9.1.0")
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.firebase:firebase-inappmessaging-display")
    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.github.chrisbanes:PhotoView:2.3.0")
//    implementation("com.pdftron:pdftron:11.0.0")
    //implementation("com.pdftron:pdftron-standard:11.0.0")
//    implementation("com.pdftron:tools:11.0.0")
//    implementation("androidx.multidex:multidex:2.0.1")
//    implementation("com.pspdfkit:pspdfkit:2024.9.1")
}
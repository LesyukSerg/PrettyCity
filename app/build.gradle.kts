plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.raziel.prettycity"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raziel.prettycity"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Google Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.google.direction.library)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    implementation(libs.exifinterface)

    implementation (libs.recyclerview.swipedecorator)

    implementation (libs.glide)
    annotationProcessor (libs.compiler)

    // Room (Java)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
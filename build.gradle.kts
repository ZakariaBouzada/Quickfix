// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.1" apply false // <-- This is the AGP version
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false // Note: 4.4.1 is a common stable version
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
    
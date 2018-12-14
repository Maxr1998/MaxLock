// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Config.BuildPlugins.androidGradlePlugin)
        classpath(Config.BuildPlugins.kotlinGradlePlugin)
        classpath("com.jakewharton.hugo:hugo-plugin:1.2.1")
    }
}

allprojects {
    repositories {
        google()
        maven { setUrl("https://jitpack.io") }
        jcenter()
    }
}

tasks.create<Delete>("clean") {
    delete(rootProject.buildDir)
}
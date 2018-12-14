object Config {
    const val androidGradleVersion = "3.3.0-rc02"
    const val kotlinVersion = "1.3.11"

    object BuildPlugins {
        val androidGradlePlugin = "com.android.tools.build:gradle:$androidGradleVersion"
        val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    }

    object Deps {
        val kotlinAndroidStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
    }
}
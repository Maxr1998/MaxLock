object Config {
    const val androidGradleVersion = "4.0.0-beta03"
    const val kotlinVersion = "1.3.71"

    val languages =
            arrayOf("de", "es", "fa", "fr", "in", "it", "ja", "pl", "pt", "ru", "tr", "zh-rCN", "zh-rTW")
    val crowdinLanguages =
            arrayOf("de", "es-ES", "fa", "fr", "id", "it", "ja", "pl", "pt-PT", "ru", "tr", "zh-CN", "zh-TW")
    var crowdinKey: String? = null

    object BuildPlugins {
        const val androidGradlePlugin = "com.android.tools.build:gradle:$androidGradleVersion"
        const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    }

    object Deps {
        const val kotlinAndroidStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
        const val androidJar = "libs/hidden-api.jar"
        const val splittiesBase = "com.louiscad.splitties"
        const val splittiesVersion = "2.1.1"
    }

    object Tasks {
        val copyAndroidJar = "copyAndroidJar"
    }
}
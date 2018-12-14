object Config {
    const val androidGradleVersion = "3.3.0-rc02"
    const val kotlinVersion = "1.3.11"

    val languages =
            arrayOf("de", "es", "fa", "fr", "in", "it", "ja", "pl", "pt", "ru", "tr", "zh-rCN", "zh-rTW")
    val crowdinLanguages =
            arrayOf("de", "es-ES", "fa", "fr", "id", "it", "ja", "pl", "pt-PT", "ru", "tr", "zh-CN", "zh-TW")
    var crowdinKey: String? = null

    object BuildPlugins {
        val androidGradlePlugin = "com.android.tools.build:gradle:$androidGradleVersion"
        val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    }

    object Deps {
        val kotlinAndroidStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
    }
}
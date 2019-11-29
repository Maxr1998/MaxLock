import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.Project
import java.io.File
import java.util.*

object SigningConfigHelper {
    private const val ENV_KEYSTORE = "SIGNING_KEYSTORE"
    private const val ENV_KEYSTORE_PASSWORD = "SIGNING_KEYSTORE_PASSWORD"
    private const val ENV_KEY_ALIAS = "SIGNING_KEY_ALIAS"
    private const val ENV_KEY_PASSWORD = "SIGNING_KEY_PASSWORD"

    fun getConfig(project: Project, android: BaseExtension): SigningConfig? {
        val keyStoreEnv = System.getenv(ENV_KEYSTORE)
        val keyStoreProps = getKeyStoreProps(project)

        lateinit var keyStoreFile: File
        lateinit var keyStorePassword: String
        lateinit var keyAlias: String
        lateinit var keyPassword: String

        when {
            keyStoreEnv != null -> {
                keyStoreFile = try {
                    project.file("/tmp/ML-keystore.jks").apply {
                        writeBytes(Base64.getDecoder().decode(keyStoreEnv))
                    }
                } catch (e: RuntimeException) {
                    return null
                }
                keyStorePassword = System.getenv(ENV_KEYSTORE_PASSWORD) ?: return null
                keyAlias = System.getenv(ENV_KEY_ALIAS) ?: return null
                keyPassword = System.getenv(ENV_KEY_PASSWORD) ?: return null
            }
            keyStoreProps != null -> {
                keyStoreFile = keyStoreProps["keyStore"]?.let(project::file) ?: return null
                keyStorePassword = keyStoreProps["keyStorePassword"]?.toString() ?: return null
                keyAlias = keyStoreProps["keyAlias"]?.toString() ?: return null
                keyPassword = keyStoreProps["keyPassword"]?.toString() ?: return null
            }
            else -> return null
        }

        return android.signingConfigs.create("main") {
            storeFile = keyStoreFile
            storePassword = keyStorePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }

    private fun getKeyStoreProps(project: Project): Properties? {
        val keyStoreConfig = project.file("keyStore.properties")
        return if (keyStoreConfig.exists()) {
            Properties().apply {
                load(keyStoreConfig.inputStream())
                Config.crowdinKey = this["crowdinKey"].toString()
            }
        } else null
    }
}
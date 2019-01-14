import org.gradle.api.artifacts.repositories.ArtifactRepository
import java.io.File
import java.io.InputStream

fun ArtifactRepository.includes(vararg includes: String): ArtifactRepository {
    content {
        for (i in includes) {
            includeGroupByRegex(i)
        }
    }
    return this
}

fun ArtifactRepository.excludes(vararg includes: String): ArtifactRepository {
    content {
        for (i in includes) {
            excludeGroupByRegex(i)
        }
    }
    return this
}

fun InputStream.copyTo(file: File): Long = file.outputStream().use { copyTo(it) }
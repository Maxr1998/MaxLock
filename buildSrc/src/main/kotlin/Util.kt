import org.gradle.api.artifacts.repositories.ArtifactRepository

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
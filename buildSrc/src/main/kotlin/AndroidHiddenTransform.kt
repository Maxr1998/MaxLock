import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.File

class AndroidHiddenTransform : Transform() {
    override fun getName() = "AndroidHiddenTransform"

    override fun getInputTypes(): Set<QualifiedContent.ContentType> =
            setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun isIncremental() = false

    override fun getScopes(): MutableSet<QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun transform(transformInvocation: TransformInvocation) {
        println("Transforming with $name")
        transformInvocation.outputProvider.deleteAll()
        val outputDir = transformInvocation.outputProvider.getContentLocation(
                "classes",
                outputTypes,
                scopes,
                Format.DIRECTORY
        )
        transformInvocation.inputs.forEach { transformInput ->
            transformInput.directoryInputs.forEach { inputDirectory ->
                processDirectory(inputDirectory.file, outputDir)
            }
        }
        println("$name finished.")
    }

    private fun processDirectory(directory: File, outputDir: File) {
        println("Processing ${directory.absolutePath}")
        for (file in directory.walkTopDown()) {
            val target = File(outputDir, file.toRelativeString(directory))
            when {
                file.isDirectory -> target.mkdirs()
                file.extension == "class" -> {
                    val classReader = file.inputStream().use { ClassReader(it) }
                    val classWriter = ClassWriter(classReader, 0)
                    val classVisitor = ClassRemapper(classWriter, AndroidHiddenClassRemapper())

                    // Read data into visitor, and thus the writer
                    classReader.accept(classVisitor, 0)

                    // Write output
                    target.writeBytes(classWriter.toByteArray())
                }
                else -> file.copyTo(target)
            }
        }
    }

    class AndroidHiddenClassRemapper : Remapper() {
        override fun map(typeName: String): String {
            return typeName.replace("android_hidden", "android")
        }
    }
}
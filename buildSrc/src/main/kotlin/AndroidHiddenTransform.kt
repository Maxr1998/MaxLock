import com.android.build.api.transform.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.File

class AndroidHiddenTransform : Transform() {
    override fun getName() = "AndroidHiddenTransform"

    override fun getInputTypes(): Set<QualifiedContent.ContentType> =
            setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun isIncremental() = true

    override fun getScopes(): MutableSet<QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun transform(transformInvocation: TransformInvocation) {
        val incremental = transformInvocation.isIncremental
        println("Transforming with $name, incremental: $incremental")

        transformInvocation.inputs.forEach { transformInput ->
            transformInput.directoryInputs.forEach { directoryInput ->
                val inputDir = directoryInput.file
                val outputDir = getOutputFile(transformInvocation.outputProvider, Format.DIRECTORY)
                println("Processing ${inputDir.absolutePath}")
                if (incremental) {
                    for ((file, status) in directoryInput.changedFiles) {
                        val target = File(outputDir, file.toRelativeString(inputDir))
                        if (status == Status.REMOVED) {
                            target.deleteRecursively()
                        } else if (status != Status.NOTCHANGED) {
                            processFile(file, target)
                        }
                    }
                } else {
                    for (file in inputDir.walkTopDown()) {
                        val target = File(outputDir, file.toRelativeString(inputDir))
                        processFile(file, target)
                    }
                }
            }
            transformInput.jarInputs.forEach { jarInput ->
                if (!incremental || jarInput.status != Status.NOTCHANGED) {
                    val output = getOutputFile(transformInvocation.outputProvider, Format.JAR)
                    jarInput.file.copyTo(output, true)
                }
            }
        }
        println("$name finished.")
    }

    private fun processFile(file: File, output: File) {
        when {
            file.isDirectory -> output.mkdirs()
            file.extension == "class" -> {
                val classReader = file.inputStream().use { ClassReader(it) }
                val classWriter = ClassWriter(classReader, 0)
                val classVisitor = ClassRemapper(classWriter, AndroidHiddenClassRemapper())

                // Read data into visitor, and thus the writer
                classReader.accept(classVisitor, 0)

                // Write output
                output.writeBytes(classWriter.toByteArray())
            }
            else -> file.copyTo(output, true)
        }
    }

    private fun getOutputFile(outputProvider: TransformOutputProvider, format: Format): File {
        return outputProvider.getContentLocation("aht", inputTypes, scopes, format)
    }

    class AndroidHiddenClassRemapper : Remapper() {
        override fun map(typeName: String): String {
            return typeName.replace("android_hidden", "android")
        }
    }
}
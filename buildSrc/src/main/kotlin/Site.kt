
import java.io.File

class FileRange(private val file: File, private val range: IntRange) {

    companion object {
        fun parse(parent: File, path: String): FileRange {
            val tokens = path.split(":")
            val filePath = tokens[0].trim()
            val file = parent.toPath().resolve(filePath).toFile()
            return if (tokens.size == 1) {
                FileRange(file)
            }
            else {
                val range = tokens[1]
                val rangeTokens = range.split(",")
                if (rangeTokens.size == 1)
                    FileRange(file, rangeTokens[0])
                else
                    FileRange(file, rangeTokens[0].toInt(), rangeTokens[1].toInt())
            }
        }
    }

    @JvmOverloads
    constructor(file: File, begin: Int? = null, end: Int? = null) : this(
        file,
        (begin ?: 0) .. (end ?: file.readLines().size - 1)
    )

    constructor(file: File, tag: String) : this(
        file,
        file
            .readLines()
            .map { it.trim() }
            .let { lines ->
                val start = lines.indexOfFirst { it.contains("// $tag") } + 1
                val end = lines.indexOfLast { it.contains("// $tag") } - 1
                start .. end
            }
    )

    fun lines(): List<String> = file.readLines().slice(range)

    fun text(): String = lines().joinToString("\n").trimIndent()

    fun strippedLines(): List<String> = lines().map { it.trim() }.filter { it.isNotEmpty() }

    override fun toString(): String = "$file.absolutePath [$range]"
}

fun checkSamplesCode(documentation: FileRange, source: FileRange) {
    if (documentation.strippedLines() != source.strippedLines())
        error("""
            Documentation $documentation does not match $source

            DOC -----------------------------------------------
            ${documentation.text()}

            SRC -----------------------------------------------
            ${source.text()}
        """.trimIndent())
}

fun insertSamplesCode(markdownFile: File, content: String): String {
    val samples = "@sample (.*)".toRegex().findAll(content)
    var result = content

    samples.forEach { sample ->
        val sampleLocation = sample.groups[1]?.value?.trim() ?: error("Location expected")
        val fileRange = FileRange.parse(markdownFile, sampleLocation)
        val replacement = "```kotlin\n" + fileRange.text().trim() + "\n```"
        result = result.replace("@sample $sampleLocation", replacement)
    }

    return result
}

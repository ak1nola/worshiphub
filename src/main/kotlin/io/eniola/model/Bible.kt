package io.eniola.model

import io.eniola.Main
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

class Bible private constructor (val version: String) {
    companion object {
        val log = Logger.getLogger(Bible::class.java.name)!!

        private val bibles = mutableMapOf<String, Bible>()
        private val path = Main.res.resolve("bibles")

        val versions: List<String> =
                Files.list(path)
                        .filter { it.toFile().name.endsWith(".xmm") }
                        .map { it.toFile().name.replace("\\.xmm".toRegex(), "") }
                        .collect(Collectors.toList())

        fun get(version: String): Bible {
            log.info("Loading $version.xmm -> ${path.resolve("$version.xmm").toFile().exists()}")

            return when (bibles.containsKey(version) && bibles[version] != null) {
                true -> bibles[version]!!
                else -> {
                    val bible = Bible(version)
                    bibles[version] = bible

                    bible
                }
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val bible = Bible("MSG")
            println(bible.books)
        }
    }

    val content = mutableMapOf<String, List<List<String>>>()
    val books = mutableListOf<String>()

    init {
        try {
            books.clear()
            content.clear()

            val p = path.resolve("$version.xmm")
            val lines = Files.readAllLines(p, StandardCharsets.ISO_8859_1)
            val raw = lines.joinToString("")
            //log.info(raw)

            //FIXME - this logic breaks with unicode
            val bookPattern = """<b\s+n="(.+?)">(.+?)</b>""".toRegex()
            val chapterPattern = """<c\s+n="(.+?)">(.+?)</c>""".toRegex()
            val versePattern = """<v\s+n="(.+?)">(.+?)</v>""".toRegex()

            bookPattern.findAll(raw).forEach { bk ->
                val book = bk.groupValues[1]
                books.add(book)

                content.put(book,
                        chapterPattern.findAll(bk.value).map { ch ->
                            //val openBibleBookChapter = ch.groupValues[1]

                            versePattern.findAll(ch.value).map { vs ->
                                //val verse = vs.groupValues[1]
                                val text = vs.groupValues[2]

                                //println("$openBiblebook $openBibleBookChapter:$verse\t\t$text")

                                text
                            }.toList()
                        }.toList()
                )
            }
        } catch (e: Exception) {
            log.log(Level.SEVERE, "Error: ${e.message}", e)
        }
    }
}
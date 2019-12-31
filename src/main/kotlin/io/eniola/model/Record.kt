package io.eniola.model

import io.eniola.Controller
import io.eniola.Controller.Companion.DELIMITER
import io.eniola.Main
import io.eniola.model.RecordType.Companion.initialText
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList

import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject

import java.io.StringReader
import java.sql.*

import java.util.logging.Logger

enum class RecordType {
    TEXT, AUDIO, VIDEO, IMAGE, NONE;

    companion object {
        fun get(name: String) =
                when (name) {
                    TEXT.toString() -> TEXT
                    AUDIO.toString() -> AUDIO
                    VIDEO.toString() -> VIDEO
                    IMAGE.toString() -> IMAGE
                    NONE.toString() -> NONE
                    else -> TEXT
                }

        val initialText = """
            Add a new item by overwriting this content and then click on "Save" button

            Hint: use the "Select image/audio/video" button to selectItem the video, audio or image

            Hint: enter a new line between each paragraph to separate verses and choruses

            Hint: to help visualize whitespace, use settings to toggle visibility of whitespaces as ↵
        """.trimIndent()
    }
}

data class Record(
        val id: Int = -1,
        val type: RecordType = RecordType.TEXT,
        val title: SimpleStringProperty = SimpleStringProperty("***Create New***"),
        val author: SimpleStringProperty = SimpleStringProperty(""),
        val data: SimpleStringProperty = SimpleStringProperty(initialText),
        val displayIndex: Int = -1,
        val displayType: RecordType = RecordType.NONE
) {

    private val content = FXCollections.observableArrayList(data.value.split(DELIMITER))

    val meta = HashMap<String, Any>()

    init {
        data.addListener { _, oValue, nValue ->
            when (nValue) {
                null -> content.clear()
                oValue -> Unit
                else -> {
                    content.clear()
                    content.addAll(data.value.split(DELIMITER))
                }
            }
        }
    }

    fun nextIndex() =
        this.copy(displayIndex = if (displayIndex >= content().size - 1 || displayIndex == -1) 0 else displayIndex + 1)

    fun displayText() = if (displayIndex == -1) title.value else content[displayIndex]



    fun content(): ObservableList<String> {
        if (data.value == initialText && id != -1) {
            connect().use { conn ->
                conn.prepareStatement(dmlSelect).use { stmt: PreparedStatement ->
                    stmt.setInt(1, id)
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        data.value = rs.getString("CONTENT")
                    }
                }
            }
        }

        return content
    }

    override fun toString() =
            "$id | $title | $type -> [${data.value.replace(DELIMITER, " || ").replace("\n", ", ")}] @@@ $displayIndex|$displayType"

    fun persist(): Record {
        var uid = id

        connect().use { conn: Connection ->
            if (id == -1) {
                conn.prepareStatement(dmlInsert, Statement.RETURN_GENERATED_KEYS).use { stmt: PreparedStatement ->
                    stmt.setString(1, title.value)
                    stmt.setString(2, author.value)
                    stmt.setString(3, data.value)
                    stmt.setString(4, type.toString())

                    stmt.executeUpdate()

                    stmt.getGeneratedKeys().use { rs: ResultSet ->
                        if (rs.next())
                            uid = rs.getInt(1)
                    }
                }
            } else {
                conn.prepareStatement(dmlUpdate).use { stmt: PreparedStatement ->
                    stmt.setString(1, title.value)
                    stmt.setString(2, author.value)
                    stmt.setString(3, data.value)
                    stmt.setString(4, type.toString())
                    stmt.setInt(5, id)

                    stmt.execute()
                }
            }
        }

        return if (uid != id) this.copy(id = uid) else this
    }

    fun delete() {
        if (id > -1)
            connect().use { conn: Connection ->
                conn.prepareStatement(dmlDelete).use { stmt: PreparedStatement ->
                    stmt.setInt(1, id)

                    stmt.execute()
                }
            }
    }

    companion object {
        val log = Logger.getLogger(Record::class.java.name)!!

        val ddlCreate =
                """
                CREATE TABLE RECORDS (
                    ID BIGINT primary key GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
                    ADDED_ON TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    TITLE VARCHAR(255),
                    AUTHOR VARCHAR(255),
                    CONTENT CLOB,
                    TYPE VARCHAR(32),
                    BACKGROUND VARCHAR(255),
                    VERSION SMALLINT
                )
                """.trimIndent()

        val ds = arrayOf("Introit", "Call To Worship\nAkin Olaolu", "Open Hymn\nHope Ohiarah", Controller.OPEN_WORSHIP_PROTOCOL + "SDAH 001").joinToString(
                Controller.DELIMITER
        )

        val ss = listOf("Prayer Session", "Jump Around").joinToString(Controller.DELIMITER)

        val ddlInsert = mutableListOf(
                "insert into records (title, type, content) values ('Divine Service', 'TEXT', '$ds')",
                "insert into records (title, type, content) values ('Sabbath School', 'TEXT', '$ss')",
                "insert into records (title, type, content) values ('Special Video', 'VIDEO', '/home/mishel/workspace/Jesu-yi.mp4')",
                "insert into records (title, type, content) values ('Special Audio', 'AUDIO', '/home/mishel/workspace/Jesu-yi.mp3')"
        )

        val ddlDrop = "DROP TABLE RECORDS"

        val dmlLoad = "SELECT ID, TITLE, AUTHOR, CONTENT, TYPE FROM RECORDS ORDER BY TITLE"
        val dmlSelect = "SELECT CONTENT FROM RECORDS WHERE id = ?"
        //val dmlSearch = "SELECT * FROM RECORDS WHERE title like ? or content like ?"

        val dmlUpdate = "UPDATE RECORDS SET TITLE=?, AUTHOR=?, CONTENT=?, TYPE=? WHERE ID=?"
        val dmlInsert = "INSERT INTO RECORDS (TITLE,AUTHOR,CONTENT,TYPE) VALUES (?,?,?,?)"
        val dmlDelete = "DELETE FROM RECORDS WHERE ID=?"

        var path: String = Main.res.resolve("db").toFile().absolutePath

        val connectionStr = "jdbc:derby:$path/worshipdb;create=true"
        val driverName = "org.apache.derby.jdbc.EmbeddedDriver"

        fun connect(): java.sql.Connection = DriverManager.getConnection(connectionStr)

        fun record(id: Int = -1, type: String, title: String, author: String = "", content: String, displayIndex: Int = -1, displayType: String = RecordType.NONE.toString()) =
                Record(id, RecordType.get(type),
                        SimpleStringProperty(title),
                        SimpleStringProperty(author),
                        SimpleStringProperty(content),
                        displayIndex,
                        RecordType.get(displayType)
                )

        fun loads(): List<Record> {
            val list = mutableListOf<Record>()

            connect().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(dmlLoad)

                    while (rs.next()) {
                        val rec = record(
                                rs.getInt("ID"),
                                rs.getString("TYPE") ?: "TEXT",
                                rs.getString("TITLE") ?: "",
                                rs.getString("AUTHOR") ?: "",
                                rs.getString("CONTENT") ?: ""
                        )

                        list.add(rec)
                    }
                }
            }

            log.info("### Retrieved ${list.size} records from database")
            return list
        }

        fun ingest(data: String): List<Record> {
            val reader = Json.createReader(StringReader(data))

            reader.use {
                val jElement = it.read()

                val json =
                        if (jElement is JsonObject && jElement.containsKey("songs"))
                            jElement["songs"] as JsonArray
                        else if (jElement is JsonArray)
                            jElement
                        else
                            Json.createArrayBuilder().build()

                connect().use { conn ->
                    json.forEach { o ->
                        val r = o as JsonObject

                        conn.prepareStatement(dmlInsert).use { stmt ->
                            val author = r.getString("author", "")
                            stmt.setString(1, r.getString("title", r.getString("name", "")))
                            stmt.setString(2, if ("null" == author) "" else author)
                            stmt.setString(3, r.getString("content"))
                            stmt.setString(4, r.getString("type", RecordType.TEXT.name).toString())

                            stmt.execute()
                        }
                    }
                }
            }


            return loads()
        }

        //https://stackoverflow.com/a/35137606
inline fun <T : AutoCloseable, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            close()
        } catch (closeException: Exception) {
            e.addSuppressed(closeException)
        }
        throw e
    } finally {
        if (!closed) {
            close()
        }
    }
}

        fun export(): String {
            val array = Json.createArrayBuilder().build()

            loads().forEach { record ->
                val o = Json.createObjectBuilder().build()

                o.set("type", Json.createValue(record.type.toString()))
                o.set("title", Json.createValue(record.title.value))
                o.set("author", Json.createValue(record.author.value))
                o.set("content", Json.createValue(record.data.value))

                array.add(o)
            }

            return array.toString()
        }

        init {
            println(connectionStr)

            //Class.forName(driverName)

            val array = arrayOf("SDAH 002: All Creatures of our God and King", "SDAH 004: This is the Day of Joy", "You alone are worthy Lord", "There is none Holy as the Lord")

            for ((_, title) in array.withIndex())
                ddlInsert.add("insert into records (title, type, content) values ('$title', 'TEXT', '${title + Controller.DELIMITER + title}')")

            connect().use { conn: Connection ->
                val rs = conn.metaData.getTables(null, null, null, null)

                //conn.createStatement().use { it.execute(ddlDrop) }

                var foundTable = false

                while (rs.next())
                    if ("RECORDS".equals(rs.getString(3))) {
                        foundTable = true
                        break
                    }

                if (!foundTable) {
                    conn.createStatement().use {
                        println(ddlCreate)
                        it.execute(ddlCreate)
                        ddlInsert.forEach { sql -> it.execute(sql) }
                    }
                }
            }
        }

        @JvmStatic
        fun main() {
            System.out.println(path)
        }
    }
}
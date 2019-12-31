package io.eniola

import io.eniola.model.Bible
import io.eniola.model.Record
import io.eniola.model.RecordType
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.transformation.FilteredList
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Rectangle2D
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.stage.*
//import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level

import java.util.logging.Logger

import java.util.function.Predicate

import javafx.util.Duration
import javafx.application.Platform

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.*
import javafx.event.ActionEvent
import javafx.scene.control.Hyperlink
import javafx.scene.input.KeyEvent

import javafx.scene.shape.Rectangle
import java.io.FileWriter
import java.util.*

class Controller {
    companion object {
        val OPEN_WORSHIP_PROTOCOL = "worshiphub://"
        val DELIMITER = "\n\n"
    }



    @FXML var imgPreview = ImageView()
    @FXML var imgAnchor = BorderPane()

    //only need these defined so we are force the verses of records to show up by default in the accordion
    @FXML var accordion = Accordion()
    @FXML var recordPane = TitledPane()

    //these will be managed by Projector.kt
    @FXML var cboProjectorScreen = ComboBox<String>()
    @FXML var tblProjectorRecords = TreeTableView<Record>()
    @FXML var colProjectorRecords = TreeTableColumn<Record, Record>()

    //these are controlled by the curator
    @FXML var txtSearch = TextField()
    @FXML var txtTitle = TextField()
    @FXML var txtAuthor = TextField()
    @FXML var txtContent = TextArea()

    @FXML var lstRecords = ListView<Record>()

    @FXML var lstBook = ListView<String>()
    @FXML var lstChapter = ListView<Int>()
    @FXML var lstVerse = ListView<Int>()

    @FXML var chkSortBooks = CheckBox()
    @FXML var chkWhitespace = CheckBox()
    @FXML var cboVersion = ComboBox<String>()

    //all settings are actually only used by the projector - so that they are managed by Projector.kt
    @FXML var chkBackgroundActive = CheckBox()

    @FXML var cboShadowColor = ColorPicker()
    @FXML var cboShadowSpread = ComboBox<Number>()
    @FXML var cboShadowRadius = ComboBox<Number>()

    @FXML var cboFontSize = ComboBox<Number>()
    @FXML var cboFontColor = ColorPicker()
    @FXML var cboFontName = ComboBox<String>()

    val log = Logger.getLogger(Controller::class.java.name)!!

    val supportedImageExtensions = arrayOf("jpg", "jpeg", "gif", "png", "svg")
    val supportedVideoExtensions = arrayOf("mp4")
    val supportedAudioExtensions = arrayOf("mp3", "ogg")

    //these functions are a kludge. Should really let the File Dialog determine the type of the record
    fun isImage(value: String): Boolean = supportedImageExtensions.any { value.endsWith(it) }
    fun isVideo(value: String): Boolean = supportedVideoExtensions.any { value.endsWith(it) }
    fun isAudio(value: String): Boolean = supportedAudioExtensions.any { value.endsWith(it) }

    private val PLAY_STATUS = "PLAY"
    private val PAUSE_STATUS = "PAUSE"

    private val path = Paths.get(".")

    //private val IMG_EDIT = Image( Main.resourcePath.resolve("png/edit.png"))
    private val IMG_EDIT = Image(Main.res.resolve("png/edit.png").toUri().toURL().toString())
    private val IMG_STOP = Image(Main.res.resolve("png/stop.png").toUri().toURL().toString())
    private val IMG_PAUSE = Image(Main.res.resolve("png/pause.png").toUri().toURL().toString())
    private val IMG_PLAY = Image(Main.res.resolve("png/play.png").toUri().toURL().toString())
    private val IMG_VIDEO = Image(Main.res.resolve("png/video.png").toUri().toURL().toString())
    private val IMG_AUDIO = Image(Main.res.resolve("png/audio.png").toUri().toURL().toString())
    private val IMG_TEXT = Image(Main.res.resolve("png/text.png").toUri().toURL().toString())
    private val IMG_UP = Image(Main.res.resolve("png/up.png").toUri().toURL().toString())
    private val IMG_DOWN = Image(Main.res.resolve("png/down.png").toUri().toURL().toString())
    private val IMG_CLOSE = Image(Main.res.resolve("png/close.png").toUri().toURL().toString())
    private val IMG_SLIDE = Image(Main.res.resolve("png/slide.png").toUri().toURL().toString())
    private val IMG_TICK = Image(Main.res.resolve("png/tick.png").toUri().toURL().toString())
    private val IMG_TICK2 = Image(Main.res.resolve("png/tick2.png").toUri().toURL().toString())
    private val IMG_PIX = Image(Main.res.resolve("png/pix.png").toUri().toURL().toString())

    private fun createImage(type: String) =
            when (type) {
                "EDIT" -> ImageView(IMG_EDIT)
                "STOP" -> ImageView(IMG_STOP)
                "PAUSE" -> ImageView(IMG_PAUSE)
                "PLAY" -> ImageView(IMG_PLAY)
                "VIDEO" -> ImageView(IMG_VIDEO)
                "AUDIO" -> ImageView(IMG_AUDIO)
                "TEXT" -> ImageView(IMG_TEXT)
                "UP" -> ImageView(IMG_UP)
                "DOWN" -> ImageView(IMG_DOWN)
                "CLOSE" -> ImageView(IMG_CLOSE)
                "SLIDE" -> ImageView(IMG_SLIDE)
                "TICK" -> ImageView(IMG_TICK)
                "TICK2" -> ImageView(IMG_TICK2)
                else -> ImageView(IMG_PIX)
            }

    private val fileChooser = FileChooser()

    private fun initFileChooser() {
            fileChooser.title = "Worship Hub - multi-media selector"
            fileChooser.initialDirectory = File(System.getProperty("user.dir"))
            fileChooser.selectedExtensionFilter = FileChooser.ExtensionFilter("MP3", "*.mp3", "MP4", "*.mp4", "PNG", "*.png", "JPEG", "*.jpg", "JPEG", ".jpeg")
    }

    //tblRecords: TreeTableView<Record>, val tblColumn: TreeTableColumn<Record, Record>, val cboProjectorScreen: ComboBox<String>

    private fun hideWhitespaceContent(s: String?): String =
			s?.replace("↵".toRegex(RegexOption.MULTILINE), "")?.replace("·".toRegex(RegexOption.MULTILINE), " ") ?: ""

    private fun showWhitespaceContent(s: String?): String {
		return if (chkWhitespace.isSelected)
			s?.replace("\\n".toRegex(RegexOption.MULTILINE), "↵\n")?.replace(" ".toRegex(RegexOption.MULTILINE), "·") ?: ""
		else
			s?.trim() ?: ""
	}

    private val records = FXCollections.observableArrayList<Record> { arrayOf(it.title, it.author, it.content()) }
    private val list = FilteredList<Record>(records)

    private val bible = SimpleObjectProperty<Bible>()

    data class BibleQuote(var index: Int = -1, var verses: MutableList<String> = mutableListOf()) {
        fun clear() {
            index = -1
            verses.clear()
        }

        fun current() = if (index < 0 || index >= verses.size) "" else verses[index]
    }

    private val quote = BibleQuote()

    //TODO - addToProjector ability to check for dirty textarea
    private val formRecord = SimpleObjectProperty<Record>()

    private val stage = Stage(StageStyle.UNDECORATED)
    private var stackPane = StackPane()

    private var mainNode: Node = Text()
    private val titleNode = Text()

    //private var background: Background = nil  //oadBackground(stage.maxWidth, stage.maxHeight)
    private var motionBackground = MediaView()

    private var projectorScreenBounds = ArrayList<Rectangle2D>()
    private var currentRecord = Record()

    fun initialize() {


        txtContent.isWrapText = true
        txtContent.focusedProperty().addListener { _, _, focused ->
            txtContent.text =
                if(focused) hideWhitespaceContent(txtContent.text)
                else showWhitespaceContent(txtContent.text)
        }

        records.add(Record())
        records.addAll(Record.loads())

        lstRecords.setCellFactory { _ -> renderLibraryItemCell() }
        lstRecords.items = list

        formRecord.addListener { _, oldValue: Record?, newValue: Record ->
            if (oldValue != newValue) {
                log.info("### Editing $newValue")

                txtTitle.text = newValue.title.value
                txtAuthor.text = newValue.author.value
                txtContent.text = showWhitespaceContent(newValue.data.value)
            }
        }

        //doing this after adding the listener so that the form will always default to empty record
        formRecord.value = records[0]

        initFileChooser()

        //stage.initStyle(StageStyle.UTILITY)
        configLoad()

        lstVerse.selectionModel.selectionMode = SelectionMode.MULTIPLE

        //set projection stage to close when app closes
        initMainStageCallbacks()

        initScreenBounds()

        //load the projector queue with stuff from the properties file
        initProjectorRecords()

        onProjectorSettingsBackgroundToggle()
    }

    fun onProjectorQueueItemClicked(event: MouseEvent) {

        val rec = tblProjectorRecords.selectionModel.selectedItem?.value

        if (event.clickCount == 2 && event.button == MouseButton.PRIMARY && rec?.displayType != RecordType.TEXT && rec?.displayType != RecordType.IMAGE)
            return

        //if (event.clickCount == 2 && event.button == MouseButton.PRIMARY && (rec?.displayType == RecordType.TEXT || rec?.displayType == RecordType.IMAGE))
        if (event.button == MouseButton.PRIMARY && (rec?.displayType == RecordType.TEXT || rec?.displayType == RecordType.IMAGE))
            project(rec)
    }

    fun onProjectorQueueItemKeyPressed(event: KeyEvent) {
        val target = tblProjectorRecords.selectionModel.selectedItem

        if (event.code == KeyCode.ENTER && !target.isLeaf) {
            target.isExpanded = !target.isExpanded
        } else if (target.value.displayType == RecordType.TEXT || target.value.displayType == RecordType.IMAGE) {
            project(target.value)
        }
    }

    private fun initMainStageCallbacks() {
        Main.stage!!.onCloseRequest = EventHandler<WindowEvent> {
            projectorRecords = //save off the items in the queue to properties file
                    tblProjectorRecords.root.children
                            .filter { i -> i.value.displayType == RecordType.NONE }
                            .map { i -> i.value.id.toString() }
                            .joinToString(",")

            projectionQueueTickerStop(true)
            configSave()
            stage.close()

            System.exit(0)
        }

        //Main.stage!!.setOnHiding { _ -> stage.toBack() }
        //Main.stage!!.setOnHidden { _ -> stage.toBack() }

        //Main.stage!!.setOnShowing { _ -> stage.toFront() }
        //Main.stage!!.setOnShown { _ -> stage.toFront() }

        stage.toFront()
        Main.stage!!.toFront()
    }

    private fun initProjectorRecords() {
        val parent = TreeItem(Record())
        parent.isExpanded = true

        tblProjectorRecords.root = parent
        tblProjectorRecords.isShowRoot = false

        colProjectorRecords.setCellFactory { renderProjectorQueueItemCell() }
        colProjectorRecords.setCellValueFactory { param -> SimpleObjectProperty(param.value.value) }

        try {
            //set the accordion to show the library by default
            accordion.expandedPane = recordPane

            val records =
                    projectorRecords.split(",").filterNot { it == "" }.map { i -> records.find { it.id == i.toInt() }!! }.toTypedArray()

            projectionQueueAppend(*records)
        } catch (e: Exception) {
            log.log(Level.SEVERE, "Could not load projector queue", e)
        }
    }

    fun onLibraryFileChange () {
        var path = File(System.getProperty("user.home"))
        fileChooser.initialDirectory = path
        val file = fileChooser.showOpenDialog(Main.stage)
        if (file != null)
            txtContent.text = file.absolutePath
    }

    fun onLibraryItemSelected1(event: KeyEvent) {
        onLibraryItemSelected()
    }

    fun onLibraryItemSelected2(event: MouseEvent) {
        val record = onLibraryItemSelected()

        if (event.clickCount == 2 && record.id != -1)
            projectionQueueAppend(record)
    }

    fun onLibraryItemSelected(record: Record? = null): Record {
        log.info("selecting an item")
        formRecord.value = record ?: lstRecords.selectionModel.selectedItem
        log.info("### Selected Item [${formRecord.value}]")
        return formRecord.value
    }

    fun onBibleBookChanged() {
            val book = lstBook.selectionModel.selectedItem
            val size = bible.value.content[book]?.size ?: 1
            val chapters = IntRange(1, size)

            lstVerse.items.clear()

            lstChapter.items.clear()
            lstChapter.items.addAll(chapters.toList())

            lstChapter.selectionModel.selectIndices(0)
    }

    fun onBibleChapterChanged() {
        log.info("updating openBibleBookChapter")

        val book = lstBook.selectionModel.selectedItem
        val chapter = lstChapter.selectionModel.selectedIndex

        val size = bible.value.content[book]?.get(chapter)?.size ?: 1
        val verses = IntRange(1, size)

        lstVerse.items.clear()
        lstVerse.items.addAll(verses)
    }

    fun onBibleSortClicked() {
        val books = if (chkSortBooks.isSelected) bible.value.books.sorted() else bible.value.books

        lstBook.items.clear()
        lstBook.items.addAll(books)

        lstBook.selectionModel.select(0)
    }

    fun onBibleVersionChanged() {
        bible.value = Bible.get(cboVersion.value)
        onBibleVerseClicked()  //if verses were already selected then update the openBibleVersion
    }

    fun onBibleNextVerse(): Record? {
        //TODO iterate to the selectNextBibleVerse verse
        val record: Record? =
            if (!quote.verses.isEmpty()) {
                onBibleVerseClicked(quote.index)  //just in case user selected a different item and then attempted to continue

                quote.index = if (quote.index + 1 >= quote.verses.size) quote.verses.size - 1 else quote.index + 1
                log.info("### Showing selectNextBibleVerse quote: ${quote.current()}")
                formRecord.value.copy(displayIndex = quote.index, displayType = RecordType.TEXT)
            } else
                null

        if (record != null)
            project(record)

        return record
    }

    fun onBiblePrevVerse(): Record? {
        //TODO iterate to the selectPrevBibleVerse verse
        val record: Record? =
            if (!quote.verses.isEmpty()) {
                onBibleVerseClicked(quote.index)  //just in case user selected a different item and then attempted to continue

                quote.index = if (quote.index - 1 <= 0) 0 else quote.index - 1
                log.info("### Showing previous quote: ${quote.current()}")
                return formRecord.value.copy(displayIndex = quote.index, displayType = RecordType.TEXT)
            } else
                null

        if (record != null)
            project(record)

        return record
    }

    fun onLibraryDelete(): Record {  //save hymn,audio,video entry
        if (formRecord.value.id == -1)  return formRecord.value

        val record = formRecord.value

        log.info("### Deleting Hymn [$record]")
        record.delete()
        log.info("### Deleted Hymn [$record]")

        records.remove(record)
        formRecord.value = records[0]

        projectionQueueRemove(record)
        return record
    }

    fun onLibraryPersist(): Record { //save hymn,audio,video entry
        val text = hideWhitespaceContent(txtContent.text).trim()

        var rtype = when {
            isImage(text) -> RecordType.IMAGE
            isVideo(text) -> RecordType.VIDEO
            isAudio(text) -> RecordType.AUDIO
            else -> RecordType.TEXT
        }

        var record =
                when (formRecord.value.id) {
                    -1 -> Record(type = rtype)
                    else -> formRecord.value.copy(type = rtype)
                }

        record.title.value = txtTitle.text
        record.author.value = txtAuthor.text
        record.data.value = text

        log.info("${supportedImageExtensions.any { text.endsWith(it) }}")
        log.info("### ${if (record.id == -1) "Inserting" else "Updating"} Hymn [$rtype] - $record")
        record = record.persist()

        log.info("### Updated Hymn [$record]")

        if (formRecord.value.id != record.id) { //if this is a new record
            records.add(record)
        }

        return record
    }

    fun onProjectorQueueItemAppend() { //save hymn,audio,video entry & addToProjector to projector queue
        val record = onLibraryPersist()
        projectionQueueAppend(record)
    }

    fun onLibraryImport() {
        //curator.value.ingestItems()
        val dialog = FileChooser()
        dialog.title = "Select json file to import"
        val file = dialog.showOpenDialog(Main.stage)

        if (file != null) {
            val data = String(Files.readAllBytes(Paths.get(file.toURI())))
            val rr = Record.ingest(data)

            records.clear()
            records.add(Record())
            records.addAll(rr)

            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Import Complete"
            alert.headerText = "All songs including in opened file have been imported"
            alert.contentText = "You may now addToProjector any of the imported songs to Presentations"

            alert.showAndWait()
        }
    }

    fun onLibraryExport() {
        val dialog = FileChooser()
        dialog.title = "Choose output filename & location"
        val file = dialog.showSaveDialog(Main.stage)

        if (file != null) {
            val export = Record.export()
            Files.write(Paths.get(file.toURI()), export.toByteArray())

            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Export Complete"
            alert.headerText = "All items copied to specified file"
            alert.contentText = "Export file: ${file.absolutePath}"

            alert.showAndWait()
        }
    }

    fun help() {
		// Not using HostServices because there's an openjdk bug
		var url = ClassLoader.getSystemResource("application.html").toURI()
        java.awt.Desktop.getDesktop().browse(url)
    }

    fun onLibraryFilter() {
        val text = txtSearch.text
        list.predicate = Predicate { s -> if (text.trim().isEmpty()) true else s.title.value.toLowerCase().contains(text.toLowerCase()) }
    }

    private fun renderLibraryItemCell() =
            object : ListCell<Record>() {
                override fun updateItem (record: Record?, empty: Boolean) {
                    super.updateItem(record, empty)

                    val graphic =
                            if (record?.type == null || empty) null
                            else {
                                val label = Label("", createImage(record.type.toString()))
                                label.textProperty().bind(record.title)
                                label
                            }
                    setGraphic(graphic)
                }
            }

    fun onBibleVerseClicked(event: MouseEvent) {
        val record = onBibleVerseClicked()

        if (event.clickCount == 2 && event.button == MouseButton.PRIMARY) {
            project(record.copy(displayIndex = 0))
        }
    }

    private fun onBibleVerseClicked(vararg index: Int): Record {
        quote.clear()

        if (lstVerse.selectionModel.selectedIndices.size != 0) {

            val book = lstBook.selectionModel.selectedItem
            val chapter = lstChapter.selectionModel.selectedIndex
            val verses = lstVerse.selectionModel.selectedIndices.map { verse ->
                "${verse + 1}. ${bible.value.content[book]?.get(chapter)?.get(verse)}"
            }

            val length = lstVerse.selectionModel.selectedIndices.size

            val vv =
                    if (length == 1) "${lstVerse.selectionModel.selectedIndices[0] + 1}"
                    else "${lstVerse.selectionModel.selectedIndices[0] + 1}-${lstVerse.selectionModel.selectedIndices[0] + length}"

            val header = "$book ${chapter + 1}:$vv (${cboVersion.selectionModel.selectedItem})"

            formRecord.value = Record.record(
                    type = RecordType.TEXT.toString(),
                    title = header,
                    content = verses.joinToString(DELIMITER),
                    displayType = RecordType.TEXT.toString()
            )

            if (!index.isEmpty()) quote.index = index[0]
            quote.verses.addAll(verses)
        }

        return formRecord.value
    }

    /*********************************** projector **********************************************************/

    fun onProjectorSettingsBackgroundChange () {
        val path = Main.res.resolve("background").toFile()

        if(path.exists())
            fileChooser.initialDirectory = path
        else
            log.warning("Invalid home directory: " + path.absolutePath)

        val file = fileChooser.showOpenDialog(Main.stage)
        if (file != null) {
            backgroundFile = file.absolutePath
            onProjectorSettingsBackgroundToggle()
        }
    }

    fun onProjectorSettingsBackgroundToggle() {
        if (!chkBackgroundActive.isSelected ||
                (!onProjectorSettingsBackgroundImageToggle(backgroundFile) && !onProjectorSettingsBackgroundMotionToggle(backgroundFile))) {

            if (!chkBackgroundActive.isSelected) {
                if (motionBackground.mediaPlayer != null) {
                    motionBackground.mediaPlayer.stop()
                    motionBackground.mediaPlayer.dispose()
                }

                stackPane.children.remove(motionBackground)
                stackPane.background = Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
            }
        }
    }

    private fun onProjectorSettingsBackgroundMotionToggle(filename: String): Boolean {
        for (ext in videoExtensions.split(",")) {
            if (filename.endsWith(".$ext")) {
                try {
                    stackPane.background = Background.EMPTY

                    if (motionBackground.mediaPlayer != null && motionBackground.mediaPlayer.status == MediaPlayer.Status.PLAYING)
                        motionBackground.mediaPlayer.stop()

                    if (motionBackground.mediaPlayer != null)
                        motionBackground.mediaPlayer.dispose()

                    motionBackground.x = 0.0
                    motionBackground.y = 0.0
                    motionBackground.fitWidthProperty().bind(stage.widthProperty())
                    motionBackground.fitHeightProperty().bind(stage.heightProperty())
                    motionBackground.isPreserveRatio = false

                    if (motionBackground.parent != stackPane) {
                        stackPane.children.add(motionBackground)
                        StackPane.setAlignment(motionBackground, Pos.CENTER)
                        motionBackground.toBack()
                    }

                    val file = File(filename).toURI().toString()
                    motionBackground.mediaPlayer = MediaPlayer(Media(file))
                    motionBackground.mediaPlayer.cycleCount = MediaPlayer.INDEFINITE
                    motionBackground.mediaPlayer.volume = 0.0
                    motionBackground.mediaPlayer.isAutoPlay = true

                    return true
                } catch (e: Exception) {
                    log.log(Level.SEVERE, "Could not set motion background: " + filename + " " + e.message, e)
                }
            } else {
                log.warning("background file [$filename] does not end with .$ext!")
            }
        }
        return false
    }

    private fun onProjectorSettingsBackgroundImageToggle(filename: String): Boolean {
        for (ext in imageExtensions.split(",")) {
            if (filename.endsWith(".$ext")) {
                try {
                    if (motionBackground.parent == stackPane) {
                        if (motionBackground.mediaPlayer != null)
                            motionBackground.mediaPlayer.stop()
                        stackPane.children.remove(motionBackground)
                    }

                    stackPane.background = loadBackground(stage.maxWidth, stage.maxHeight)
                    log.info("loading background image: $backgroundFile")

                    return true
                } catch (e: Exception) {
                    log.log(Level.SEVERE, "Could not update the background", e)
                }

            } else {
                log.warning("background file [$filename] does not end with .$ext!")
            }
        }
        return false
    }

    fun project (record: Record, node: Node? = null, auto: Boolean = false) {
        if (!auto)  projectionQueueAutoSlideStop()

        currentRecord = record

        log.info("### Projecting Current record: $currentRecord | ${record.displayIndex}")

        if (mainNode.parent == stackPane)
            stackPane.children.remove(mainNode)

        if (titleNode.parent == stackPane)
            stackPane.children.remove(titleNode)

        if (mainNode is MediaView) { //if the current i.e soon to be destroyed node is a mediaplayer, then pause it first
            val mView: MediaView = mainNode as MediaView

            if (mView.mediaPlayer != null) {
                mView.mediaPlayer.pause()
            }

            mediaView.fitWidthProperty().bind(stage.widthProperty())
            mediaView.fitHeightProperty().bind(stage.heightProperty())
            mediaView.isPreserveRatio = true
        }

        mainNode = when {
            node != null && node is MediaView -> {
                mediaView.fitWidthProperty().bind(stage.widthProperty())
                mediaView.fitHeightProperty().bind(stage.heightProperty())
                mediaView.isPreserveRatio = false

                node
            }
            node != null -> node
            currentRecord.type == RecordType.IMAGE -> {
                val imageView = io.eniola.ImageView()
                imageView.image = Image(File(currentRecord.data.value).toURI().toURL().toString())

                imageView
            }
            else -> projectionQueueText(record.displayText())
        }

        titleNode.text = record.title.get()


        stackPane.children.add(mainNode)
        StackPane.setAlignment(mainNode, Pos.CENTER)

        stackPane.children.add(titleNode)
        StackPane.setAlignment(titleNode, Pos.BOTTOM_LEFT)

        onFontPropertyChanged(titleNode, 0.35)

        if (mainNode is Text) {  //TODO - :)
            //while (node is Label && node.boundsInLocal.height > stage.height && node.font.size > 5.0)
            //node.font = Font(config.fontName, node.font.size - 1.0)
        }

        stage.title = "Worship Hub - Projector"
        stage.icons.removeAll()
        stage.icons.add(Image(Main.res.resolve("application.png").toUri().toURL().toString()))
        stage.show()
        stage.toFront()

        Main.stage!!.requestFocus()

        updatePreview()
    }

    fun updatePreview() {
        log.info("Creating preview")

        //create the screenshot of the other screen
        try {
            val snapshot = stage.scene.snapshot(null)
            imgPreview.image = snapshot
        } catch (e: Exception) {
            log.log(Level.SEVERE, "Could not set live preview", e)
        }
    }

    fun onFontPropertyChanged(event: ActionEvent) {
        onFontPropertyChanged(titleNode, 0.35)

        if (mainNode is Text) {
            onFontPropertyChanged(mainNode as Text)  //Cast like this not good as the type could have changed at this point
        }
    }

    private fun onFontPropertyChanged(txt: Text, sizeFactor: Double = 1.0): Text {
        txt.fill = cboFontColor.value
        txt.font = Font(cboFontName.value, cboFontSize.value.toDouble() * sizeFactor)

        val ds = DropShadow()
        ds.radius = cboShadowRadius.value.toDouble()
        ds.color = cboShadowColor.value
        ds.spread = cboShadowSpread.value.toDouble()

        txt.effect = ds

        return txt
    }

    private fun initScreenBounds () {
        if (stage.isShowing) stage.close()

        val selected = cboProjectorScreen.selectionModel.selectedItem
        cboProjectorScreen.items.remove(1, cboProjectorScreen.items.size)

        projectorScreenBounds.clear()

        for ((index, screen) in Screen.getScreens().withIndex()) {
            projectorScreenBounds.add(screen.bounds)
            cboProjectorScreen.items.add("Screen${index + 1}")
        }

        cboProjectorScreen.selectionModel.select(selected)
        log.info("current projector screen: " + cboProjectorScreen.value + " out of " + cboProjectorScreen.items.toList() + ". It was " + selected)
        onProjectorSettingsScreenChange(ActionEvent())
    }

    fun onProjectorSettingsScreenChange(e: ActionEvent) {
        val index = cboProjectorScreen.selectionModelProperty().get().selectedIndex

        if (index == 0) initScreenBounds()
        else if (index < 0) log.info("don't know how to set projector screen to index $index")
        else {
                val bounds = projectorScreenBounds[index - 1]
                stage.x = bounds.minX
                stage.maxWidth = bounds.width
                stage.minWidth = bounds.width

                stage.y = bounds.minY
                stage.maxHeight = bounds.height
                stage.minHeight = bounds.height

                stackPane = StackPane()

                stackPane.children.addAll(tickerBackground, tickerText)
                StackPane.setAlignment(tickerBackground, Pos.TOP_CENTER)
                StackPane.setAlignment(tickerText, Pos.TOP_CENTER)

                onProjectorSettingsBackgroundToggle()
                stage.scene = Scene(stackPane, stage.maxWidth, stage.maxHeight)

                stage.title = "Worship Hub - Projector"
                stage.icons.removeAll()
                stage.icons.add(Image(Main.res.resolve("application.png").toUri().toURL().toString()))
                stage.show()

                stage.isResizable = false

                Main.stage!!.requestFocus()
                Main.stage!!.toFront()
            }
    }

    private fun projectionQueueText(text: String): Text {
        val node = Text(text)
        node.textOrigin = VPos.CENTER
        node.textAlignment = TextAlignment.CENTER
        node.wrappingWidthProperty().bind(stage.widthProperty().add(-450))

        onFontPropertyChanged(node)  //make sure the node has the right size

        return node
    }

    fun projectionQueueAppend(vararg records: Record) =
            records.forEach {
                log.info("inserting ${it.id} - ${it.title} into projection list")
                tblProjectorRecords.root.children.add(projectorQueueTreeItem(it))
            }

    fun projectionQueueRemove(vararg records: Record) =
            records.forEach { record ->
                log.info("removing ${record.id} - ${record.title} from projection list")
                tblProjectorRecords.root.children.removeIf { i ->
                    !i.isLeaf && i.value.id == record.id
                }
            }

    fun projectorQueueTreeItem(record: Record, treeItem: TreeItem<Record> = TreeItem(record), hasListener: Boolean = false): TreeItem<Record> {

        log.info("creating projectorItems for ${record.id} - ${record.title} - sized: ${record.content().size}")

        record.content().withIndex().forEach { (index, entry) ->
            if (!entry.startsWith(OPEN_WORSHIP_PROTOCOL)) {
                val temp = record.copy(displayIndex = index, displayType = record.type)
                treeItem.children.add(TreeItem(temp))
            } else {  //TODO - find a way to do actual search by record title
                //we don't want up,down,deleteFromProjector buttons for this record - so we make the meta.type != None
                val temp = Record().copy(displayIndex = 0, displayType = RecordType.TEXT)
                treeItem.children.add(projectorQueueTreeItem(temp))
            }

            Unit
        }

        if (!hasListener) {
            val listener =
                    ListChangeListener<String> {
                        //TODO: when the treeItem has been deleted, we want no new nodes added to the tree
                        treeItem.children.clear()
                        projectorQueueTreeItem(record, treeItem)
                    }
            record.content().addListener(listener)
        }
        return treeItem
    }

    private fun renderProjectorQueueItemCell() = object : TreeTableCell<Record, Record>() {
        override fun updateItem(record: Record?, empty: Boolean) {
            super.updateItem(record, empty)

            if (empty || record?.displayType == null) setGraphic(null)
            else {
                val graphic: Node =
                    when (record?.displayType) {
                        RecordType.TEXT -> projectionQueueCellForText(record)
                        RecordType.AUDIO, RecordType.VIDEO -> projectionQueueCellForMediaPlayer(record)
                        RecordType.IMAGE -> projectionQueueCellForImage(record)
                        else -> projectionQueueCellForHeader(record, this.treeTableRow)
                    }

                setGraphic(graphic)
            }

        }
    }

    private fun projectionQueueCellForHeader(record: Record, treeTableRow: TreeTableRow<Record>): GridPane {
        log.info("Creating projector queue entry for $record")

        val label = Label()
        label.textProperty().bind(record.title)
        label.textOverrun = OverrunStyle.ELLIPSIS

        val property =
                when (record.type) {
                    RecordType.AUDIO, RecordType.VIDEO -> record.data
                    else -> record.title
                }

        label.tooltip = Tooltip(property.value)
        label.tooltip.textProperty().bind(property)
        label.graphic = createImage(record.type.toString())
        label.graphic.styleClass.add("projectorQueueImage")

        val deleteBtn = Hyperlink()
        deleteBtn.graphic = createImage("CLOSE")
        deleteBtn.styleClass.add("projectorQueueImage")
        deleteBtn.onAction = EventHandler {
            val treeItem = treeTableRow.treeItem
            treeItem.parent.children.remove(treeItem)

            //TODO - for audio,video,background - make sure to stop the player if it's currently playing this file
        }

        val editBtn = Hyperlink()
        editBtn.graphic = createImage("EDIT")
        editBtn.styleClass.add("projectorQueueImage")
        editBtn.onAction = EventHandler { onLibraryItemSelected(treeTableRow.treeItem.value) }

        val autoSlideBtn = projectionQueueAutoSlide(record)
        val tickerBtn = projectionQueueTicker(record)

        val grid = GridPane()

        if (record.type == RecordType.TEXT)
            grid.addRow(0, label, tickerBtn, autoSlideBtn, editBtn, deleteBtn)
        else
            grid.addRow(0, label, editBtn, deleteBtn)

        val columnConstraints = ColumnConstraints()
        columnConstraints.hgrow = Priority.ALWAYS
        grid.columnConstraints.add(columnConstraints)

        return grid
    }

    private fun projectionQueueCellForImage(record: Record): Label {
        log.info("Creating projector queue entry for ${record.displayType} - ${record.data.value}")

        val imageView = io.eniola.ImageView()
        imageView.image = Image(File(record.data.value).toURI().toURL().toString(), 50.0, 50.0, false, false)

        val label = Label(record.data.value, imageView)
        label.isWrapText = true

        return label
    }

    private fun projectionQueueCellForText(record: Record): Label {
        log.info("Creating projector queue entry for text - ${record.title.value} - ${record.displayIndex}")

        val label = Label(record.displayText())
        if (record.displayIndex == -1) label.textProperty().bind(record.title)
        return label
    }

    //************************************** code related to player *************************************************

    var mediaView = MediaView()
    var mediaPlayer:MediaPlayer? = null

    private fun projectionQueueMediaPlayerStart(record: Record): MediaPlayer {
        projectionQueueMediaPlayerStop()

        mediaPlayer = MediaPlayer(Media(File(record.displayText()).toURI().toString()))

        if (isVideo(record.displayText())) {
            mediaView = MediaView(mediaPlayer)
            mediaPlayer?.onEndOfMedia = Runnable { projectionQueueMediaPlayerStop() }

            project(record, mediaView)
        }

        return mediaPlayer!!
    }

    private fun projectionQueueMediaPlayerStop() {
        mediaPlayer?.pause()
    }

    fun projectionQueueCellForMediaPlayer(record: Record): Node {
        log.info("Creating projector queue entry for ${record.displayType} - ${record.data.value}")

        val button = ToggleButton()
        button.tooltip = Tooltip(PLAY_STATUS)
        button.graphic = createImage(PLAY_STATUS)

        val lblTime = Label()
        lblTime.text = "00:00"

        val slider = Slider(0.0, 100.0, 0.0)

        val hBox = HBox()
        hBox.children.addAll(button, slider, lblTime)
        hBox.alignment = Pos.CENTER

        HBox.setHgrow(slider, Priority.ALWAYS)

        fun updateCurrentTime(player: MediaPlayer, position: Duration, duration: Duration) {
            slider.value = (position.toMillis() / duration.toMillis()) * 100.0
            lblTime.text = "${asDurationString(position)}/${asDurationString(player.media.duration)}"
        }

        button.onAction = EventHandler {
            if (!button.isSelected) {
                projectionQueueMediaPlayerStop()

                button.tooltip.text = PLAY_STATUS
                button.graphic = createImage(PLAY_STATUS)
            } else {
                val player = projectionQueueMediaPlayerStart(record)

                player.setOnReady {
                    val duration = player.media.duration
                    val position = duration.multiply(slider.value / 100.0)

                    log.info("Duration: ${duration.toSeconds()}; Position: ${position.toSeconds()}; Slider: ${slider.value}")

                    player.currentTimeProperty().addListener { _, _, pos -> updateCurrentTime(player, pos, duration) }
                    player.seek(position)
                    player.play()
                }

                button.tooltip.text = PAUSE_STATUS
                button.graphic = createImage(PAUSE_STATUS)
            }
        }

        if (record.meta.containsKey("player")) { //this only happens when the UI is redrawn
            val player = record.meta["player"] as MediaPlayer
            player.currentTimeProperty().addListener { _, _, pos -> updateCurrentTime(player, pos, player.media.duration) }
        }

        return hBox
    }

    private fun asDurationString(duration: Duration): String {
        var seconds = Math.floor(duration.toSeconds()).toInt()
        val hours = seconds / 3600
        seconds -= if (hours > 0) hours * 3600 else 0
        val mins = seconds / 60
        seconds -= if (mins > 0) mins * 60 else 0

        return if (hours <= 0) String.format("%02d:%02d", mins, seconds) else String.format("%d:%02d:%02d", hours, mins, seconds)
    }

    private var autoSlideTimer = Timer()

    private fun projectionQueueAutoSlideStart(rec: Record) {
        projectionQueueAutoSlideStop()

        autoSlideTimer = Timer()

        var record = rec.copy()

        autoSlideTimer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                Platform.runLater {
                    record = record.nextIndex()  //this is what actually moves the code forward
                    project(record, null, true)
                }
            }
        }, 0, tileTimeout * 1000)

        log.info("Scheduled auto slides in $tileTimeout seconds")
    }

    private fun projectionQueueAutoSlideStop() {
        autoSlideTimer.cancel()
        autoSlideTimer.purge()
    }

    fun projectionQueueAutoSlide(record: Record): Node {
        val button = ToggleButton()
        button.graphic = createImage("SLIDE")

        button.onAction = EventHandler {
            button.graphic =
                if (button.isSelected) {
                    projectionQueueAutoSlideStart(record)
                    createImage("STOP")
                } else {
                    projectionQueueAutoSlideStop()
                    createImage("SLIDE")
                }
        }

        return button
    }

    private val tickerBackground = Rectangle()
    private val tickerText = Text()
    private var tickerSpeed = 18000.0

    private var timelineForTickerText = Timeline()
    private var timelineForTickerBackground = Timeline()
    private var timelineForTickerStop = Timeline()

    private fun projectionQueueTickerStart(record: Record) {
        projectionQueueTickerStop(true) //set the previous ticker to false

        //background.translateX = p.stage.maxWidth
        tickerBackground.height = stage.maxHeight / 8
        tickerBackground.isVisible = true
        tickerBackground.fill = Color.GRAY
        tickerBackground.width = stage.width
        tickerBackground.opacity = 0.5

        tickerText.text = record.data.value.replace("\n", "\t\t")
        tickerText.fill = cboFontColor.value
        tickerText.font = Font(cboFontName.value, cboFontSize.value.toDouble())

        tickerBackground.height = tickerText.layoutBounds.height + 5

        tickerSpeed = tickerText.layoutBounds.width * 10

        timelineForTickerText = Timeline()
        timelineForTickerText.keyFrames.addAll(
                KeyFrame(Duration.ZERO,
                        KeyValue(tickerText.opacityProperty(), 1.0),
                        KeyValue(tickerText.translateXProperty(), stage.maxWidth)),
                KeyFrame(Duration(tickerSpeed),
                        KeyValue(tickerText.translateXProperty(), -tickerText.layoutBounds.width))
        )
        timelineForTickerText.cycleCount = 3
        timelineForTickerText.onFinished = EventHandler { projectionQueueTickerStop() }

        timelineForTickerBackground = Timeline()
        timelineForTickerBackground.keyFrames.addAll(
                KeyFrame(Duration.ZERO,
                        KeyValue(tickerBackground.translateXProperty(), 0.0),
                        KeyValue(tickerBackground.translateYProperty(), - stage.maxHeight/10),
                        KeyValue(tickerBackground.opacityProperty(), 0.0)),
                KeyFrame(Duration(500.0),
                        KeyValue(tickerBackground.translateXProperty(), 0.0),
                        KeyValue(tickerBackground.translateYProperty(), 0.0),
                        KeyValue(tickerBackground.opacityProperty(), 0.50),
                        KeyValue(tickerText.opacityProperty(), 1.0))
        )
        timelineForTickerBackground.play()
        timelineForTickerBackground.onFinished = EventHandler { timelineForTickerText.play() }
    }

    private fun projectionQueueTickerStop(force: Boolean = false) {
        if (force && timelineForTickerBackground.status != Animation.Status.STOPPED) timelineForTickerBackground.stop()
        if (force && timelineForTickerText.status != Animation.Status.STOPPED) timelineForTickerText.stop()
        if (force && timelineForTickerStop.status != Animation.Status.STOPPED) timelineForTickerStop.stop()

        timelineForTickerStop = Timeline()
        timelineForTickerStop.keyFrames.addAll(
                KeyFrame(Duration.ZERO,
                        KeyValue(tickerBackground.translateXProperty(), 0.0),
                        KeyValue(tickerBackground.opacityProperty(), 1.0)),
                KeyFrame(Duration(500.0),
                        KeyValue(tickerBackground.translateYProperty(), -tickerBackground.height),
                        KeyValue(tickerBackground.opacityProperty(), 0.5),
                        KeyValue(tickerText.opacityProperty(), 0.0))
        )
        timelineForTickerStop.play()

        timelineForTickerStop.onFinished = EventHandler {
            log.info("stopped ticker")
        }
    }

    fun projectionQueueTicker(record: Record): Node {
        val button = ToggleButton()
        button.graphic = createImage("TICK")
        button.tooltip = Tooltip("Click to show ticker at top of screen")

        button.onAction = EventHandler {
            button.graphic =
                if (button.isSelected) {
                    projectionQueueTickerStart(record)
                    button.tooltip = Tooltip("Click to hide ticker at top of screen")
                    createImage("TICK2")
                } else {
                    projectionQueueTickerStop(true)
                    button.tooltip = Tooltip("Click to show ticker at top of screen")
                    createImage("TICK")
                }
        }

        return button
    }

    var backgroundFile = ""
    var projectorRecords = ""

    var audioExtensions = ""
    var videoExtensions = ""
    var imageExtensions = ""

    var tileTimeout = 0L

    fun configSave() {
        val properties = Properties()

        properties.setProperty("shadow.Radius", cboShadowRadius.value.toString())
        properties.setProperty("shadow.Color", cboShadowColor.value.toString())
        properties.setProperty("shadow.Spread", cboShadowSpread.value.toString())

        properties.setProperty("font.Size", cboFontSize.value.toString())
        properties.setProperty("font.Color", cboFontColor.value.toString())
        properties.setProperty("font.Name", cboFontName.value)

        var bk = backgroundFile
        if (backgroundFile.startsWith(Main.res.toFile().absolutePath))
            bk = bk.replace(Main.res.toFile().absolutePath + File.separator, "")

        properties.setProperty("background.File", bk)
        properties.setProperty("background.Active", chkBackgroundActive.isSelected.toString())

        if (cboProjectorScreen.value != null)
            properties.setProperty("projector.Screen", cboProjectorScreen.value)

        properties.setProperty("projector.Records", projectorRecords)

        properties.setProperty("extensions.Audio", audioExtensions)
        properties.setProperty("extensions.Video", videoExtensions)
        properties.setProperty("extensions.Image", imageExtensions)

        properties.setProperty("bible.openBibleVersion", cboVersion.value)
        properties.setProperty("bible.sorted", chkSortBooks.isSelected.toString())

        properties.setProperty("show.whitespace", chkWhitespace.isSelected.toString())

        val file = Main.res.resolve("application.properties").toAbsolutePath().toFile()
        properties.store(FileWriter(file.absolutePath), "")
    }

    fun configLoad() {
        val properties = Properties()
        try {
            properties.load(Files.newInputStream(Main.res.resolve("application.properties")))
        } catch (e: Exception) {
            log.log(Level.SEVERE, "Could not load config", e)
        }

        cboShadowRadius.items.addAll(IntRange(1, 30))
        cboShadowRadius.selectionModel.select(properties.getProperty("shadow.Radius", "0.5").toDouble())

        cboShadowColor.value = Color.valueOf(properties.getProperty("shadow.Color", "BLACK"))

        cboShadowSpread.items.addAll(0.rangeTo(40).map { it / 20.0 })
        cboShadowSpread.selectionModel.select(properties.getProperty("shadow.Spread", "12").toDouble())

        val defaultFont = Font.getDefault()

        cboFontSize.items.addAll(IntRange(7, 132))
        cboFontSize.selectionModel.select(properties.getProperty("font.Size", "${defaultFont.size * 3}").toDouble())

        cboFontColor.value = Color.valueOf(properties.getProperty("font.Color", "WHITE"))

        cboFontName.items.addAll(Font.getFontNames())
        cboFontName.selectionModel.select(properties.getProperty("font.Name", defaultFont.family.toString()))

        //if this is a background packaged with the application, we want to use relative path
        backgroundFile = properties.getProperty("background.File", "/background/Blank2560x1600.png")
        if (!backgroundFile.startsWith(File.separator) && !backgroundFile.contains(":"))
            backgroundFile = Main.res.toFile().absolutePath + File.separator + backgroundFile

        chkBackgroundActive.isSelected = properties.getProperty("background.Active", "true").toBoolean()

        cboProjectorScreen.selectionModel.select(properties.getProperty("projector.Screen", "Refresh Monitors"))
        projectorRecords = properties.getProperty("projector.Records", "")

        audioExtensions = properties.getProperty("extensions.Audio", supportedAudioExtensions.joinToString(","))
        videoExtensions = properties.getProperty("extensions.Video", supportedVideoExtensions.joinToString(","))
        imageExtensions = properties.getProperty("extensions.Image", supportedImageExtensions.joinToString(","))

        cboVersion.items.addAll(Bible.versions)
        cboVersion.selectionModel.select(properties.getProperty("bible.openBibleVersion", "KJV"))
        chkSortBooks.isSelected = properties.getProperty("bible.sorted", "true").toBoolean()

        tileTimeout = properties.getProperty("tile.timeout", "30").toLong()

        chkWhitespace.isSelected = properties.getProperty("show.whitespace", "false").toBoolean()

        //call these guys at least once manually - the FXML will handle it going forward
        onBibleVersionChanged()
        onBibleSortClicked()
    }

    fun loadBackground(width: Double, height: Double): Background {
        return Background(
                BackgroundImage(
                        Image(File(backgroundFile).toURI().toString()),
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        BackgroundSize(width, height, false, false, true, true)
                )
        )
    }
}
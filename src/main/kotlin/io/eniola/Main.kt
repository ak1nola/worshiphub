package io.eniola

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import java.util.logging.Logger
import java.util.logging.Level
import javafx.application.HostServices
import java.nio.file.Paths

//import org.scenicview.ScenicView

class Main : Application() {
    override fun start(stage : Stage?) {
        try {


            Companion.stage = stage
            //Main.hostService = getHostServices()
            val loader = FXMLLoader(res.resolve("application.fxml").toUri().toURL())
            val parent: AnchorPane = loader.load()

            //val ctlr: Controller = loader.getController()

            val scene = Scene(parent, 1080.0, 720.0)
            scene.stylesheets.add(Main.res.resolve("application.css").toUri().toURL().toExternalForm())

            //ScenicView.show(scene)

            stage!!.scene = scene

            stage.title = "WorshipHub"
            stage.icons.add(Image(Main.res.resolve("application.png").toUri().toURL().toString()))

            stage.show()
		} catch (e: Exception) {
			log.log(Level.SEVERE, "Could not start application", e)
			throw e
		}
    }

    companion object {
        var stage: Stage? = null
        var hostService: HostServices? = null

        var cwd = System.getProperty("user.dir")
        var res = Paths.get(cwd)
		
        public val log = Logger.getLogger(Main::class.java.name)!!

        @JvmStatic
        fun main(args: Array<String>) {
            var os = os()

            if (System.getenv("resource_prefix") != null) {
                res = res.resolve(System.getenv("resource_prefix"))
            } else {
                //https://openjdk.java.net/jeps/343 - shows the various app image directory structures - would be cool if they all had the same, no :(

                log.info(cwd)

                when (os) {
                    "unix" -> {
                        var path = cwd.substring(0, cwd.lastIndexOf("/bin"))
                        res = Paths.get(path).resolve("lib/app")
                    }
                    "mac" -> {
                        var path = cwd.substring(0, cwd.lastIndexOf("/MacOS"))
                        res = Paths.get(path).resolve("app")
                    }
                    "windows" -> res = Paths.get(cwd).resolve("app")
                    "" -> {
                        log.warning("Could not determine the OS in use")
                        return
                    }
                }
            }

            log.info("Load resources for *" + os() + "* from: " + res.toAbsolutePath().toFile().toString())

            //println(Bible("KJV").raw)
            launch(Main::class.java)
        }

        @JvmStatic
        fun os(): String {
            var name = System.getProperty("os.name").toLowerCase()

            if (name.contains("mac") || name.contains("darwin"))
                return "mac"
            if (name.contains("win"))
                return "windows"
            if (name.contains("nix") || name.contains("nux") || name.contains("aix"))
                return "unix"

            return ""
        }
    }
}

package io.eniola

class ImageView : javafx.scene.image.ImageView() {

    init {
        isPreserveRatio = true
    }

    override fun isResizable(): Boolean = true

    override fun prefWidth(height: Double): Double {
        val img = getImage() ?: return minWidth(height)
        return img!!.width
    }

    override fun maxWidth(height: Double): Double = 16384.0
    override fun minWidth(height: Double): Double = 40.0

    override fun maxHeight(width: Double): Double = 16384.0
    override fun minHeight(width: Double): Double = 40.0

    override fun prefHeight(width: Double): Double {
        val img = getImage() ?: return minHeight(width)
        return img!!.height
    }

    override fun resize(width: Double, height: Double) {
        fitWidth = width
        fitHeight = height
    }
}
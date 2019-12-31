module worshiphub {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires java.logging;
    requires java.sql;

    requires java.json;

    requires kotlin.stdlib;

    requires org.apache.derby.engine;
    requires org.apache.derby.commons;
    requires java.desktop;

    exports io.eniola;

    opens io.eniola to javafx.graphics, javafx.fxml;
}
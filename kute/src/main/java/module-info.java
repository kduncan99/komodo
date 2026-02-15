module com.bearsnake.komodo.kute {
    requires javafx.controls;
    requires javafx.fxml;
    requires kutelib;
    requires org.controlsfx.controls;

    opens com.bearsnake.komodo.kute to javafx.fxml;
    exports com.bearsnake.komodo.kute;
}
module kutelib {
    requires java.datatransfer;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires baselib;
    requires utslib;
    requires netlib;
    requires java.desktop;
    requires jdk.compiler;

    exports com.bearsnake.komodo.kutelib;
    exports com.bearsnake.komodo.kutelib.exceptions;
    exports com.bearsnake.komodo.kutelib.keypads;
    exports com.bearsnake.komodo.kutelib.panes;
}
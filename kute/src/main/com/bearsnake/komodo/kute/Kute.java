/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import com.bearsnake.komodo.kutelib.Terminal;
import com.bearsnake.komodo.kutelib.TerminalStack;
import com.bearsnake.komodo.kutelib.keypads.ControlKeyPad;
import com.bearsnake.komodo.kutelib.keypads.CursorKeyPad;
import com.bearsnake.komodo.kutelib.keypads.FunctionKeyPad;
import com.bearsnake.komodo.kutelib.keypads.MiscKeyPad;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.*;

/**
 * Implements a terminal emulator for Komodo
 * We do NOT implement the conventional protocol in terms of RID/SID/DID, nor do we do checksum calculations.
 * We do not implement KANJI.
 * We *might* not implement transferring to virtual diskette
 * We probably will implement printing... probably
 * We support terminal sizes from 12x64 through 256x256 per convention
 */
public class Kute extends Application {

    private static Kute _instance;

    private TerminalStack _terminalStack;
    private Scene _scene;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        var root = new VBox();
        var menuBar = createMenuBar();
        var content = createContentPane();
        root.getChildren().addAll(menuBar, content);
        _scene = new Scene(root);

        // TODO somehow this needs to go into Terminal instead of here
        //   We get PRESSED then TYPED (not for modifier keys), then RELEASED
        //   We need to deal with kb locked on both PRESSED and TYPED, and still allow certain things like
        //      ctrl+B, ctrl+C, ctrl+5, escape (if it is msg wait) etc
        _scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            IO.println("Typed: " + event.getCharacter());//TODO remove
            getActiveTerminal().handleKeyTyped(event.getCharacter());
            event.consume();
        });

        //TODO what happens is we consume the keyPressed event, but it still generates a subsequent keyReleased event.
        //  So... how do we NOT process the keyReleased event?
        _scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            IO.println("Pressed: " + event.getCode());//TODO remove
            getActiveTerminal().handleKeyPressed(event.getCode());
            event.consume();
        });

        _scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            IO.println("Released: " + event.getCode());//TODO remove
            getActiveTerminal().handleKeyReleased(event.getCode());
            event.consume();
        });

        primaryStage.setTitle("Kute - Komodo UTS Terminal Emulator");
        primaryStage.setScene(_scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        _instance = this;
    }

    public void cycleTabs() {
        _terminalStack.cycleTabs();
    }

    public static Kute getInstance() {
        return _instance;
    }

    public Scene getScene() {
        return _scene;
    }

    public Terminal getActiveTerminal() {
        return _terminalStack.getActiveTerminal();
    }

    @Override
    public void stop() throws Exception {
        // TODO ask if the user is sure he wants to exit
        _terminalStack.closeAll();
    }

    // ---------------------------------------------------------------------------------------------
    // Graphical content
    // ---------------------------------------------------------------------------------------------

    private BorderPane createContentPane() {
        var content = new BorderPane();

        var ctlPad = new ControlKeyPad();
        BorderPane.setMargin(ctlPad, new Insets(5)); // optional
        content.setTop(ctlPad);

        var fkPad = new FunctionKeyPad();
        BorderPane.setMargin(fkPad, new Insets(5)); // optional
        content.setLeft(fkPad);

        var cursorPad = new CursorKeyPad();
        var miscKeyPad = new MiscKeyPad();
        var cursorMiscSet = new VBox();
        cursorMiscSet.setSpacing(10.0);
        cursorMiscSet.getChildren().addAll(miscKeyPad, cursorPad);
        BorderPane.setMargin(cursorMiscSet, new Insets(5));
        content.setRight(cursorMiscSet);

        _terminalStack = new TerminalStack();
        _terminalStack.registerKeyPad(ctlPad);
        _terminalStack.registerKeyPad(fkPad);
        _terminalStack.registerKeyPad(cursorPad);
        _terminalStack.registerKeyPad(miscKeyPad);
        BorderPane.setMargin(_terminalStack, new Insets(5)); // optional
        content.setCenter(_terminalStack);

        return content;
    }

    // ---------------------------------------------------------------------------------------------
    // Menu bar
    // ---------------------------------------------------------------------------------------------

    public Menu createFileMenu() {
        var menu = new Menu("File");

        var connectItem = new MenuItem("Connect");
        connectItem.setOnAction(e -> _terminalStack.getActiveTerminal().connect());

        var disconnectItem = new MenuItem("Disconnect");
        disconnectItem.setOnAction(e -> _terminalStack.getActiveTerminal().disconnect(true));

        var exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());

        menu.getItems().addAll(connectItem, disconnectItem, exitItem);
        return menu;
    }

    public Menu createHelpMenu() {
        var menu = new Menu("Help");
        menu.getItems().add(new MenuItem("About"));
        return menu;
    }

    public MenuBar createMenuBar() {

        var menuBar = new MenuBar();
        menuBar.getMenus().add(createFileMenu());
        menuBar.getMenus().add(createHelpMenu());
        menuBar.setUseSystemMenuBar(true);
        return menuBar;
    }
}

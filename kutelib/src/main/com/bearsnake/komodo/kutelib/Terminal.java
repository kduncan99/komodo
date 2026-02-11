/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.exceptions.*;
import com.bearsnake.komodo.kutelib.messages.*;
import com.bearsnake.komodo.kutelib.uts.*;
import com.bearsnake.komodo.kutelib.panes.*;
import com.bearsnake.komodo.netlib.SocketTrace;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 * Implements a display with backing memory, cursor and protocol handling, and a status line.
 */
public class Terminal extends Pane implements UTSSocketListener {

    private final String _name;
    private final TerminalSettings _settings;

    private TerminalDisplayPane _displayPane;
    private StatusPane _statusPane;
    private ControlPagePane _controlPagePane;
    private TerminalDisplayPane _activeDisplayPane;

    private UTSSocketHandler _socketHandler;

    private PrintMode _printMode;
    private TransferMode _transferMode;
    private TransmitMode _transmitMode;

    private Emphasis _emphasis;
    private EmphasisAction _emphasisAction;

    public Terminal(final String name,
                    final TerminalSettings terminalSettings,
                    final FontInfo initialFontInfo) {
        _name = name;
        _settings = terminalSettings;

        _statusPane = new StatusPane(_settings.getDisplayGeometry(), initialFontInfo, _settings.getColorSet());
        _displayPane = new TerminalDisplayPane(_settings.getDisplayGeometry(), initialFontInfo, _settings.getColorSet(), _statusPane);
        _controlPagePane = null;
        _activeDisplayPane = _displayPane;
        _emphasis = new Emphasis();

        _displayPane.setLayoutX(0);
        _displayPane.setLayoutY(0);
        _statusPane.setLayoutX(0);
        _statusPane.setLayoutY(_displayPane.getHeight());

        _printMode = PrintMode.PRINT;
        _transferMode = TransferMode.ALL;
        _transmitMode = TransmitMode.ALL;
        setStyle("-fx-background-color: linear-gradient(to bottom, #6a5a7a, #3a2a4a);");
        setFocusTraversable(true);

        getChildren().addAll(_displayPane, _statusPane);
        setMinHeight(_displayPane.getHeight() + _statusPane.getHeight());
        setMinWidth(_displayPane.getWidth());
        setPrefHeight(_displayPane.getHeight() + _statusPane.getHeight());
        setPrefWidth(_displayPane.getWidth());

        reset();

        addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    public void adjustLayout() {
        var hMargin = (getWidth() - _displayPane.getWidth()) / 2;
        var vMargin = (getHeight() - _displayPane.getHeight() - _statusPane.getHeight()) / 2;

        var dp = _displayPane;
        dp.setLayoutX(hMargin);
        dp.setLayoutY(vMargin);

        var sp = _statusPane;
        sp.setLayoutX(hMargin);
        sp.setLayoutY(dp.getHeight() + vMargin);

        if (_controlPagePane != null) {
            var cp = _controlPagePane;
            cp.setLayoutX(hMargin);
            cp.setLayoutY(vMargin);
        }
    }

    /**
     * Closes the terminal and cleans up resources
     */
    public void close() {
        if (_socketHandler != null) {
            _socketHandler.close();
        }

        _activeDisplayPane = null;

        getChildren().remove(_displayPane);
        _displayPane.close();
        _displayPane = null;

        getChildren().remove(_statusPane);
        _statusPane.close();
        _statusPane = null;

        if (_controlPagePane != null) {
            getChildren().remove(_controlPagePane);
            _controlPagePane.close();
            _controlPagePane = null;
        }
    }

    /**
     * Connects to the given host
     */
    public void connect() {
        if (_socketHandler == null) {
            try {
                InetSocketAddress address = new InetSocketAddress(_settings.getHostName(), _settings.getHostPort());
                SocketChannel channel = SocketChannel.open(address);
                _socketHandler = new UTSSocketHandler(channel, this);
                _statusPane.setConnected(true);
                reset();
            } catch (IOException ex) {
                System.err.println("Error creating socket: " + ex.getMessage());
            }
        }
    }

    /**
     * Redirects request to whichever pane is active.
     */
    public void cycleBackgroundColor() {
        _displayPane.cycleBackgroundColor();
    }

    /**
     * Redirects request to whichever pane is active.
     */
    public void cycleTextColor() {
        _displayPane.cycleTextColor();
    }

    /**
     * Disconnects from the host
     */
    public void disconnect(final boolean withAlert) {
        if (_socketHandler != null) {
            var proceed = !withAlert;
            if (withAlert) {
                var alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Disconnect Session");
                alert.setHeaderText("Disconnecting from " + _settings.getHostName());
                alert.setContentText("Do you really want to disconnect the session?");
                var result = alert.showAndWait();
                proceed = result.isPresent() && (result.get() == ButtonType.OK);
            }

            if (proceed) {
                _socketHandler.close();
                _socketHandler = null;
                _statusPane.setConnected(false);
            }
        }
    }

    private boolean controlPageIsActive() {
        return _controlPagePane != null;
    }

    /**
     * Handles a KeyPressed event, redirecting execution according to the key.
     * This fires multiple times when the key is held down.
     */
    private void handleKeyPressed(final KeyEvent event) {
        if (event.isMetaDown() || event.isShortcutDown() || event.isAltDown()) {
            return;
        }

        // If any of the switch cases trigger, then we handle the key event (or beep).
        // This does NOT mean we engage the described function -
        // if the keyboard is locked, we beep instead of engaging.
        switch (event.getCode()) {
            case ESCAPE -> {
                if (_settings.getEscapeKeyIsMessageWait()) {
                    kbMessageWait();
                } else {
                    return;
                }
            }
            case DELETE -> kbDeleteInLine();
            case INSERT -> kbInsertInLine();
            case BACK_SPACE -> kbBackSpace();
            case HOME -> kbCursorToHome();
            case DOWN -> kbScanDown();
            case LEFT -> kbScanLeft();
            case RIGHT -> kbScanRight();
            case UP -> kbScanUp();
            case ENTER -> {
                if (_settings.getReturnKeyIsTransmit()) {
                    kbTransmit();
                } else {
                    kbCursorReturn();
                }
            }
            case TAB -> kbTabForward();
            //TODO backward tab
            case F1 -> kbSendFunctionKey(1);
            case F2 -> kbSendFunctionKey(2);
            case F3 -> kbSendFunctionKey(3);
            case F4 -> kbSendFunctionKey(4);
            case F5 -> kbSendFunctionKey(5);
            case F6 -> kbSendFunctionKey(6);
            case F7 -> kbSendFunctionKey(7);
            case F8 -> kbSendFunctionKey(8);
            case F9 -> kbSendFunctionKey(9);
            case F10 -> kbSendFunctionKey(10);
            case F11 -> kbSendFunctionKey(11);
            case F12 -> kbSendFunctionKey(12);
            case F13 -> kbSendFunctionKey(13);
            case F14 -> kbSendFunctionKey(14);
            case F15 -> kbSendFunctionKey(15);
            case F16 -> kbSendFunctionKey(16);
            case F17 -> kbSendFunctionKey(17);
            case F18 -> kbSendFunctionKey(18);
            case F19 -> kbSendFunctionKey(19);
            case F20 -> kbSendFunctionKey(20);
            case F21 -> kbSendFunctionKey(21);
            case F22 -> kbSendFunctionKey(22);
            default -> {
                return;
            }
        }

        event.consume();
    }

    /**
     * Special handling for certain keystrokes.
     * This fires multiple times when the key is held down.
     * We translate them to desired actions.
     */
    private void handleKeyTyped(final KeyEvent event) {
        if (event.isControlDown() || event.isMetaDown() || event.isShortcutDown() || event.isAltDown()) {
            return;
        }

        if (!event.getCharacter().isEmpty()) {
            var ch = event.getCharacter().charAt(0);
            switch (ch) {
                case 0x08 -> /* ctrl h */ {
                    kbBackSpace();
                    event.consume();
                }

                case ASCII_LF -> {
                    kbPutCharacter(ASCII_LF);
                    event.consume();
                }

                case ASCII_FF -> {
                    kbPutCharacter(ASCII_FF);
                    event.consume();
                }

                default -> {
                    if ((ch >= ASCII_SP) && (ch <= ASCII_DEL)) {
                        kbPutCharacter((byte) (ch & 0xFF));
                        event.consume();
                    }
                }
            }
        }
    }

    /**
     * We don't resolve protected cells until after a key is released...
     * at least not for cursor scanning key input which we get from handleKeyPressed().
     * This strategy allows us to scan through protected cells.
     * So we have to do it when the key is released.
     * @param keyCode key which was released (which we currently don't need to know)
     */
    public void handleKeyReleased(final KeyCode keyCode) {
        _activeDisplayPane.resolveProtectedCell();
    }

    /**
     * Resets the terminal - this can be invoked externally, but that is not a requirement of this project.
     */
    public void reset() {
        _activeDisplayPane = _displayPane;
        _controlPagePane = null;
        _displayPane.reset();
        _statusPane.setErrorIndicator(false);
        _statusPane.setKeyboardLocked(false);
        _statusPane.setMessageWaiting(false);
    }

    /*
     * Sends a byte stream to a host-virtual printer.
     * It is not entirely clear how this will work.
     */
    private void sendToPrinter(final byte[] data) {
        // TODO
//        try {
//            PrinterJob printJob = PrinterJob.getPrinterJob();
//            PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
//            if (defaultPrintService != null) {
//                printJob.setPrintService(defaultPrintService);
//            } else {
//                System.err.println("No default print service found.");
//                return;
//            }
//        } catch (Exception ex) {
//            System.err.println(ex.getMessage());
//        }
    }

    public boolean startNetworkTrace() {
        if (_socketHandler == null) {
            return false;
        }
        return _socketHandler.traceStart();
    }

    public boolean pauseNetworkTrace() {
        if (_socketHandler == null) {
            return false;
        }
        _socketHandler.tracePause();
        return true;
    }

    public SocketTrace stopNetworkTrace() {
        if (_socketHandler == null) {
            return null;
        }

        var trace = _socketHandler.traceStop();
        var viewer = new TraceViewer(trace, _name);
        viewer.show();
        return trace;
    }

    // ---------------------------------------------------------------------------------------------
    // The following methods are designed to respond to keyboard inputs.
    // Some of the keyboard specificity is derived by other classes (e.g., keypads),
    // so at least some of these methods need to be public.
    // ---------------------------------------------------------------------------------------------

    public void kbBackSpace() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (_activeDisplayPane.backSpace()) {
            _activeDisplayPane.resolveProtectedCell();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbClearChanged() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.clearChangedBits()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbCursorReturn() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.cursorReturn()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbCursorToHome() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.cursorToHome()) {
            Toolkit.getDefaultToolkit().beep();
        }

        _activeDisplayPane.resolveProtectedCell();
    }

    public void kbDeleteInDisplay() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.deleteInDisplay()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbDeleteInLine() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.deleteInLine()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbDeleteLine() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.deleteLine()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbDuplicateLine() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.duplicateLine()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // Used when the space bar does not erase the character under it
    // (which we do not support now, but might later).
    public void kbEraseCharacter() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        kbPutCharacter(ASCII_SP);
    }

    // Erases all data from the cursor to the end of the screen - protected or unprotected (including FCCs)
    public void kbEraseDisplay() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.eraseDisplay()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // Erases unprotected data from the cursor to the end of the screen,
    // setting (unprotected) changed field bits to false.
    // There is no host-initiated analog for this function.
    public void kbEraseToEndOfDisplay() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.eraseToEndOfDisplay()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // Erases unprotected data from the cursor to the end of the field (or display),
    // setting (unprotected) changed field bits to false.
    // NOT the same as host-initiated function. TODO Well, what's the difference?
    public void kbEraseToEndOfField() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        /* TODO
        if (_controlPage != null) {
            _controlPage.eraseToEndOfField();
            return;
        }

        var coord =  _cursorPosition.copy();
        var cell = getCharacterCell(coord);
        var attr = getControllingAttributes(coord);
        do {
            if (attr == null) {
                cell.setCharacter(ASCII_SP);
                cell.getEmphasis().clear();
            } else {
                if (!attr.isProtected()) {
                    attr.setChanged(false);
                    cell.setCharacter(ASCII_SP);
                    if (!attr.isProtectedEmphasis()) {
                        cell.getEmphasis().clear();
                    }
                }
            }

            advanceCoordinates(coord);
            cell = getCharacterCell(coord);
            if (cell.getAttributes() != null) {
                break;
            }
        } while (!coord.equals(Coordinates.HOME_POSITION));
        draw();
         */
    }

    // Erases unprotected data from the cursor to the end of the field (or line),
    // setting (unprotected) changed field bits to false.
    // NOT the same as host-initiated function. TODO what's the difference?
    public void kbEraseToEndOfLine() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        /* TODO
        if (_controlPage != null) {
            _controlPage.eraseToEndOfField();
            return;
        }

        var coord =  _cursorPosition.copy();
        var cell = getCharacterCell(coord);
        var attr = getControllingAttributes(coord);
        do {
            if (attr == null) {
                cell.setCharacter(ASCII_SP);
                cell.getEmphasis().clear();
            } else {
                if (!attr.isProtected()) {
                    attr.setChanged(false);
                    cell.setCharacter(ASCII_SP);
                    if (!attr.isProtectedEmphasis()) {
                        cell.getEmphasis().clear();
                    }
                }
            }

            advanceCoordinates(coord);
            cell = getCharacterCell(coord);
            if (cell.getAttributes() != null) {
                break;
            }
        } while (coord.getColumn() != 1);
        draw();
         */
    }


    public void kbFCCClear() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.fccClear()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // Re-enables all FCCs on the display.
    // This enables protect, emphasis protect, right-justify, alpha-only, and numeric-only
    // behavior for the affected fields.
    public void kbFCCEnable() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.fccEnable()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // Pulls up the FCC Generate dialog
    public void kbFCCGenerate() {
        if (_statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        //TODO
//        var cPos = _activeDisplayPane.getCursorPosition();
//        // TODO what do we really need here? How do we derive that from ourself?
//        var dialog = new FieldDialog(Kute.getInstance().getScene().getWindow(), cPos);
//        var opt = dialog.showDialog();
//        opt.ifPresent(field -> _activeDisplayPane.putFCC(field));
    }

    // Locates the first character following the cursor which contains an FCC character,
    // and moves the cursor to that position. If there are none from the cursor to the
    // end of the display, move the cursor to the home position.
    // Also, set all FCCs on the display to disabled.
    // This inhibits protect, emphasis protect, right-justify, alpha-only, and numeric-only
    // behavior for the affected fields.
    public void kbFCCLocate() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.fccLocate()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbInsertInDisplay() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.insertInDisplay()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbInsertInLine() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.insertInLine()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbInsertLine() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.insertLine()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * MessageWait key was pressed
     */
    public void kbMessageWait() {
        if ((_socketHandler == null) || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        try {
            _socketHandler.write(new MessageWaitMessage());
            _statusPane.setKeyboardLocked(true);
        } catch (IOException ex) {
            disconnect(false);
            IO.println("Cannot send message wait: " + ex.getMessage());
        }
    }

    /**
     * Activates the print function, according to the current print mode
     */
    public void kbPrint() {
        if (_statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        sendToPrinter(_displayPane.getPrintStream(_printMode));
    }

    /**
     * If the area under the cursor is not protected, place the character on-screen
     * at the cursor, and advance the cursor to the next unprotected location.
     * We clear any special emphasis IFF the cell we are in is not emphasis-protected.
     * We also observe right-justification.
     */
    public void kbPutCharacter(final byte ch) {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.kbPutCharacter(ch)) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbScanDown() {
        if (_statusPane.isKeyboardLocked() || !_activeDisplayPane.scanDown()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbScanLeft() {
        if (_statusPane.isKeyboardLocked() || !_activeDisplayPane.scanLeft()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbScanRight() {
        if (_statusPane.isKeyboardLocked() || !_activeDisplayPane.scanRight()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbScanUp() {
        if (_statusPane.isKeyboardLocked() || !_activeDisplayPane.scanUp()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbSendFunctionKey(final int fKey) {
        if ((_socketHandler == null) || _statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        try {
            var msg = new FunctionKeyMessage(fKey);
            _socketHandler.write(msg);
            _statusPane.setKeyboardLocked(true);
        } catch (IOException ex) {
            disconnect(false);
            IO.println("Cannot send function key: " + ex.getMessage());
        }
    }

    public void kbSetTab() {
        if (_statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        kbPutCharacter(ASCII_HT);
    }

    public void kbSOE() {
        if (_statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        kbPutCharacter(ASCII_SOE);
    }

    public void kbTabBackward() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.tabBackward()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbTabForward() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!_activeDisplayPane.tabForward()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbToggleColumnSeparator() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        var e = new Emphasis();
        e.setColumnSeparator(true);
        if (!_activeDisplayPane.toggleEmphasis(e)) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbToggleControlPage() {
        if (!toggleControlPage()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbToggleStrikeThrough() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        var e = new Emphasis();
        e.setStrikeThrough(true);
        if (!_activeDisplayPane.toggleEmphasis(e)) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbToggleUnderScore() {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        var e = new Emphasis();
        e.setUnderscore(true);
        if (!_activeDisplayPane.toggleEmphasis(e)) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void kbTransfer() {
        // Not currently implemented
        Toolkit.getDefaultToolkit().beep();
    }

    // This is invoked either by the Return host keyboard being pressed AND kbReturnIsXmit being true,
    // OR the transmit control button being pressed.
    // If the keyboard is locked or there is already pending output we cannot send anything.
    // Otherwise, queue a UTS stream to be sent on the next poll, and lock the keyboard.
    public void kbTransmit() {
        if ((_socketHandler == null) || _statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        transmit(_transmitMode);
    }

    public void kbUnlock() {
        _statusPane.setKeyboardLocked(false);
    }

    // Print content of the screen from the SOE (non-inclusive) or the home position, to the cursor.
    // Trailing blanks are removed from the ends of rows, and each row is terminated by a CR
    // excepting the row with the cursor.
    private void printAll() {
        if (controlPageIsActive()) {
            return;
        }

        sendToPrinter(_activeDisplayPane.getPrintStream(PrintMode.PRINT));
    }

    // Print content of the screen from the SOE (non-inclusive) or the home position, to the cursor.
    // Protected content is replaced by blanks.
    // Trailing blanks are removed from the ends of rows, and each row is terminated by a CR
    // excepting the row with the cursor.
    private void printForm() {
        if (controlPageIsActive()) {
            return;
        }

        sendToPrinter(_activeDisplayPane.getPrintStream(PrintMode.FORM));
    }

    // Print everything from the SOE most-previous to the cursor (non-inclusive)
    // up to the cursor (inclusive) - do not translate anything, do not send CRs at the end
    // of display lines. Ignore FCCs.
    private void printTransparent() {
        if (controlPageIsActive()) {
            return;
        }

        sendToPrinter(_activeDisplayPane.getPrintStream(PrintMode.TRANSPARENT));
    }

    private boolean toggleControlPage() {
        boolean result = true;
        if (controlPageIsActive()) {
            _activeDisplayPane = _displayPane;
            _statusPane.setCursorPosition(_displayPane.getCursorPosition());
            getChildren().remove(_controlPagePane);
            _controlPagePane.close();

            var cpSettings = _controlPagePane.evaluate();
            if (cpSettings != null) {
                _printMode = cpSettings.printMode();
                _transferMode = cpSettings.transferMode();
                _transmitMode = cpSettings.transmitMode();
            } else {
                result = false;
            }

            _controlPagePane = null;
            _displayPane.dimDisplay(false);
            _activeDisplayPane.scheduleDrawDisplay();
        } else {
            if (_settings.getDisplayGeometry().getColumns() < 80) {
                Toolkit.getDefaultToolkit().beep();
            } else {
                var settings = new ControlPagePane.Settings(_printMode, _transferMode, _transmitMode);
                _controlPagePane = new ControlPagePane(_displayPane.getGeometry(),
                                                       _displayPane.getFontInfo(),
                                                       _displayPane.getColorSet(),
                                                       _statusPane,
                                                       settings);

                getChildren().add(_controlPagePane);
                _activeDisplayPane = _controlPagePane;
                adjustLayout();
                _displayPane.dimDisplay(true);
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------------------------------
    // Methods which handle input from the host
    // ---------------------------------------------------------------------------------------------

    private void ingestAddEmphasis(final UTSByteBuffer input)
        throws InvalidEscapeSequenceException, BufferOverflowException {
        // ESC Y code - we've already ingested ESC and Y
        var code = input.getNext();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new InvalidEscapeSequenceException("Invalid emphasis code");
        }

        _emphasis = new Emphasis(code);
        _emphasisAction = EmphasisAction.ADD;
    }

    // TODO remove
//    private boolean ingestEscapeSequence(final UTSByteBuffer input)
//        throws InvalidEscapeSequenceException,
//               BufferOverflowException,
//               CoordinateException,
//               IncompleteEscapeSequenceException {
//        if (input.peekNext() != ASCII_ESC) {
//            return false;
//        }
//
//        // cursor position?
//        var coord = input.getCursorPosition();
//        if (coord != null) {
//            _activeDisplayPane.setCursorPosition(coord);
//            return true;
//        }
//
//        input.skipNext();// skip ESC character
//        var ch2 = input.getNext();
//        if ((ch2 == 0x20) && (_emphasisAction != EmphasisAction.NONE)) {
//            _emphasis = null;
//            _emphasisAction = EmphasisAction.NONE;
//            return true;
//        } else if ((ch2 >= 0x20) && (ch2 <= 0x2F)) {
//            _emphasis = new Emphasis(ch2);
//            _emphasisAction = EmphasisAction.SET;
//            return true;
//        }
//
//        switch (ch2) {
//            case ASCII_HT -> _activeDisplayPane.putCharacter(ASCII_HT, _emphasisAction, _emphasis);
//            case ASCII_DC1 -> transmit(TransmitMode.ALL);
//            case ASCII_DC2 -> printTransparent();
//            case ASCII_DC4 -> _statusPane.setKeyboardLocked(true);
//            case 'C' -> _activeDisplayPane.deleteInDisplay();
//            case 'D' -> _activeDisplayPane.insertInDisplay();
//            case 'E' -> {} // transfer changed fields - not implemented
//            case 'F' -> {} // transfer variable fields - not implemented
//            case 'G' -> {} // transfer all fields - not implemented
//            case 'H' -> printForm();
//            case 'K' -> _activeDisplayPane.eraseToEndOfField();
//            case 'L' -> _statusPane.setKeyboardLocked(false);
//            case 'M' -> _activeDisplayPane.eraseDisplay();
//            case 'T' -> { // Tell the terminal to respond with the cursor position
//                try {
//                    if (!controlPageIsActive()) {
//                        var msg = new CursorPositionMessage(_activeDisplayPane.getCursorPosition());
//                        _socketHandler.write(msg);
//                        _statusPane.setKeyboardLocked(true);
//                    }
//                } catch (IOException ex) {
//                    disconnect();
//                    IO.println("Cannot send cursor position: " + ex.getMessage());
//                }
//            }
//            case 'X' -> ingestPutCharacterHex(input);
//            case 'Y' -> ingestAddEmphasis(input);
//            case 'Z' -> ingestRemoveEmphasis(input);
//            case '[' -> _activeDisplayPane.putCharacter(ASCII_ESC, _emphasisAction, _emphasis);
//            case '{' -> ingestPutCharacterDecimal(input);
//            case 'a' -> _activeDisplayPane.eraseUnprotectedData();
//            case 'b' -> _activeDisplayPane.eraseToEndOfLine();
//            case 'c' -> _activeDisplayPane.deleteInLine();
//            case 'd' -> _activeDisplayPane.insertInLine();
//            case 'e' -> _activeDisplayPane.cursorToHome();
//            case 'f' -> _activeDisplayPane.scanUp();
//            case 'g' -> _activeDisplayPane.scanLeft();
//            case 'h' -> _activeDisplayPane.scanRight();
//            case 'i' -> _activeDisplayPane.scanDown();
//            case 'j' -> _activeDisplayPane.insertLine();
//            case 'k' -> _activeDisplayPane.deleteLine();
//            case 'o' -> toggleControlPage();
//            case 't' -> transmit(TransmitMode.CHANGED);
//            case 'u' -> _activeDisplayPane.clearChangedBits();
//            case 'w' -> _activeDisplayPane.fccClear();
//            case 'y' -> _activeDisplayPane.duplicateLine();
//            case 'z' -> _activeDisplayPane.tabBackward();
//            default -> throw new InvalidEscapeSequenceException(ch2);
//        }
//
//        // TODO there are a couple of special things that have to be the last thing before ETX - impose that.
//        return true;
//    }

    /**
     * Ingests a message from a UTS stream.
     * Calling code strips leading SOH - STX and ending ETX before calling us.
     * There will be no NUL or SYN characters.
     */
    private void ingestMessage(final UTSByteBuffer input) throws StreamException, CoordinateException, BufferOverflowException {
        _emphasis.clear();
        _emphasisAction = EmphasisAction.NONE;
        _displayPane.setDeferred(true);

        var forceDone = false;
        while (!input.atEnd()) {
            // look for ETX here - we don't expect one, but just in case...
            if (input.peekNext() == ASCII_ETX) {
                break;
            }

            if (forceDone) {
                // do not accept any more input
                _statusPane.setErrorIndicator(true);
                break;
            }

            // Look for a UTS primitive
            var prim = UTSPrimitive.deserializePrimitive(input);
            if (prim != null) {
                switch (prim.getType()) {
                    case ADD_EMPHASIS -> {}//TODO
                    case BACKWARD_TAB -> _activeDisplayPane.tabBackward();
                    case CLEAR_CHANGED_BITS -> _activeDisplayPane.clearChangedBits();
                    case CLEAR_FCC -> _activeDisplayPane.fccClear();
                    case CONTROL_PAGE_ACCESS -> toggleControlPage();
                    case CREATE_REPLACE_EMPHASIS -> {
                        var ePrim = (UTSCreateEmphasisPrimitive) prim;
                        if (ePrim.allFlagsClear() && (_emphasisAction != EmphasisAction.NONE)) {
                            _emphasis = null;
                            _emphasisAction = EmphasisAction.NONE;
                        } else {
                            _emphasis = new Emphasis(ePrim.getColumnSeparator(), ePrim.getStrikeThrough(), ePrim.getUnderscore());
                            _emphasisAction = EmphasisAction.SET;
                        }
                    }
                    case CURSOR_POSITION -> {
                        var cpPrim = (UTSCursorPositionPrimitive) prim;
                        _activeDisplayPane.setCursorPosition(new Coordinates(cpPrim.getRow(), cpPrim.getColumn()));
                    }
                    case CURSOR_TO_HOME -> _activeDisplayPane.cursorToHome();
                    case DELETE_EMPHASIS -> {}//TODO
                    case DELETE_IN_DISPLAY -> _activeDisplayPane.deleteInDisplay();
                    case DELETE_IN_LINE -> _activeDisplayPane.deleteInLine();
                    case DELETE_LINE -> _activeDisplayPane.deleteLine();
                    case ERASE_DISPLAY -> _activeDisplayPane.eraseDisplay();
                    case ERASE_TO_END_OF_FIELD -> _activeDisplayPane.eraseToEndOfField();
                    case ERASE_TO_END_OF_LINE -> _activeDisplayPane.eraseToEndOfLine();
                    case ERASE_UNPROTECTED_DATA -> _activeDisplayPane.eraseUnprotectedData();
                    case FCC_SEQUENCE -> {
                        var fPrim = (UTSFCCSequencePrimitive) prim;
                        var coord = new Coordinates(fPrim.getRow(), fPrim.getColumn());
                        _activeDisplayPane.setCursorPosition(coord);
                        _activeDisplayPane.putFCC(fPrim.getAttributes());
                    }
                    case IMMEDIATE_FCC_SEQUENCE -> {
                        var fPrim = (UTSImmediateFCCSequencePrimitive) prim;
                        _activeDisplayPane.putFCC(fPrim.getAttributes());
                    }
                    case INSERT_IN_DISPLAY -> _activeDisplayPane.insertInDisplay();
                    case INSERT_IN_LINE -> _activeDisplayPane.insertInLine();
                    case INSERT_LINE -> _activeDisplayPane.insertLine();
                    case LINE_DUPLICATION -> _activeDisplayPane.duplicateLine();
                    case LOCK_KEYBOARD -> _statusPane.setKeyboardLocked(true);
                    case PRINT_ALL -> printAll();
                    case PRINT_FORM -> printForm();
                    case PRINT_TRANSPARENT -> printTransparent();
                    case PUT_DECIMAL -> {}//TODO
                    case PUT_ESCAPE -> _activeDisplayPane.putCharacter(ASCII_ESC, _emphasisAction, _emphasis);
                    case PUT_HEXADECIMAL -> {}//TODO
                    case SCAN_DOWN -> _activeDisplayPane.scanDown();
                    case SCAN_LEFT -> _activeDisplayPane.scanLeft();
                    case SCAN_RIGHT -> _activeDisplayPane.scanRight();
                    case SCAN_UP -> _activeDisplayPane.scanUp();
                    case SEND_CURSOR_ADDRESS -> {
                        try {
                            if (!controlPageIsActive()) {
                                var msg = new CursorPositionMessage(_activeDisplayPane.getCursorPosition());
                                _socketHandler.write(msg);
                                _statusPane.setKeyboardLocked(true);
                                forceDone = true;
                            }
                        } catch (IOException ex) {
                            disconnect(false);
                            IO.println("Cannot send cursor position: " + ex.getMessage());
                        }
                    }
                    case TAB_SET -> _activeDisplayPane.putCharacter(ASCII_HT, _emphasisAction, _emphasis);
                    case TRANSFER_CHANGED_FIELDS -> {}//TODO
                    case TRANSFER_ALL_FIELDS -> {}//TODO
                    case TRANSFER_VARIABLE_FIELDS -> {}//TODO
                    case TRANSMIT_ALL_FIELDS -> transmit(TransmitMode.ALL);
                    case TRANSMIT_CHANGED_FIELDS -> transmit(TransmitMode.CHANGED);
                    case TRANSMIT_VARIABLE_FIELDS -> transmit(TransmitMode.VARIABLE);
                    case UNLOCK_KEYBOARD -> _statusPane.setKeyboardLocked(false);
                }
            } else {
                // Not a primitive - it's probably some basic printable (or translatable) character
                var ch = input.getNext();
                switch (ch) {
                    case ASCII_HT -> _activeDisplayPane.tabForward();
                    case ASCII_CR -> _activeDisplayPane.cursorReturn();
                    case ASCII_SUB -> _activeDisplayPane.putCharacter(ASCII_SUB, _emphasisAction, _emphasis);
                    case ASCII_FS -> _activeDisplayPane.putCharacter(ASCII_FS, _emphasisAction, _emphasis);
                    case ASCII_GS -> _activeDisplayPane.putCharacter(ASCII_GS, _emphasisAction, _emphasis);
                    case ASCII_SOE -> _activeDisplayPane.putCharacter(ASCII_SOE, _emphasisAction, _emphasis);
                    default -> {
                        if (ch >= ASCII_SP) {
                            _activeDisplayPane.putCharacter(ch, _emphasisAction, _emphasis);
                        } else {
                            throw new InvalidCharacterException(ch);
                        }
                    }
                }
            }
        }

        _activeDisplayPane.setDeferred(false);
    }

    // Wrapper around putCharacter(), but we have to get the character from the stream as decimal digits
    // representing a value from 0 to 127 inclusive, followed by a '}' character
    private void ingestPutCharacterDecimal(final UTSByteBuffer input)
        throws IncompleteEscapeSequenceException,
               InvalidEscapeSequenceException,
               BufferOverflowException {
        var value = 0;
        boolean digits = false;
        boolean done = false;
        while (!input.atEnd()) {
            var ch = input.getNext();
            if (ch == '}') {
                done = true;
                break;
            }

            if (!Character.isDigit(ch)) {
                throw new InvalidEscapeSequenceException();
            }
            value = value * 10 + (ch - '0');
            digits = true;
        }

        if (!done) {
            throw new IncompleteEscapeSequenceException();
        }

        if (!digits || (value > 127)) {
            throw new InvalidEscapeSequenceException();
        }

        _activeDisplayPane.putCharacter((byte)value, _emphasisAction, _emphasis);
    }

    // As above, but we have to get the character from the stream as exactly two hex digits
    // representing a value from 0 to 255 inclusive.
    private void ingestPutCharacterHex(final UTSByteBuffer input)
        throws InvalidEscapeSequenceException,
               BufferOverflowException {
        var hex1 = input.getNext();
        var hex2 = input.getNext();
        var hexStr = String.format("%c%c", hex1, hex2);
        var value = 0;
        for (int i = 0; i < hexStr.length(); i++) {
            var ch = hexStr.charAt(i);
            if (Character.isDigit(ch)) {
                value = value * 16 + (ch - '0');
            } else if (ch >= 'a' && ch <= 'f') {
                value = value * 16 + (ch - 'a' + 10);
            } else if (ch >= 'A' && ch <= 'F') {
                value = value * 16 + (ch - 'A' + 10);
            } else {
                throw new InvalidEscapeSequenceException((byte)ch);
            }
        }

        _activeDisplayPane.putCharacter((byte)value, _emphasisAction, _emphasis);
    }

    private void ingestRemoveEmphasis(final UTSByteBuffer input)
        throws InvalidEscapeSequenceException,
               BufferOverflowException {
        // ESC Z code - we've already ingested ESC and Y
        var code = input.getNext();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new InvalidEscapeSequenceException("Invalid emphasis code");
        }

        _emphasis = new Emphasis(code);
        _emphasisAction = EmphasisAction.REMOVE;
    }

    /**
     * Socket handler notifies us that the socket has been closed.
     * @param handler SocketChannelHandler that sent the notification
     */
    @Override
    public void socketClosed(final UTSSocketHandler handler) {
        disconnect(false);
    }

    /**
     * Handles UTS traffic from socket, wrapped in a particular subclass of Message.
     * The only subclasses we expect are MessageWaitMessage and TextMessage.
     */
    @Override
    public synchronized void socketTrafficReceived(final UTSSocketHandler handler,
                                                   final UTSMessage message) {
        // TODO we may need to synchronize every possible path from the keyboard as well
        switch (message) {
            case MessageWaitMessage _ -> _statusPane.setMessageWaiting(true);
            case StatusPollMessage _ -> _statusPane.setPollIndicator();
            case StatusMessage statusMessage -> {
                // ignore this
            }
            case TextMessage tm -> {
                try {
                    ingestMessage(tm.unwrap());
                } catch (StreamException | CoordinateException | BufferOverflowException ex) {
                    System.out.println("Error in input stream:" + ex.getMessage());
                    _statusPane.setErrorIndicator(true);
                }
            }
            case null, default -> {
                // Anything else is an error. Handle it.
                System.out.println("Rejected message from host:" + message);
                _statusPane.setErrorIndicator(true);
            }
        }
    }

    @Override
    public void socketTrafficTraced(final UTSSocketHandler source) {
        _statusPane.notifyTrace();
    }

    // ---------------------------------------------------------------------------------------------
    // Methods which create or contribute content to the output stream, or otherwise relate to
    // transmit or print functionality.
    // ---------------------------------------------------------------------------------------------

    /*
     * Find the region which begins with the SOE to the left of the cursor,
     * up to and including the cursor. We always transmit the SOE if it exists.
     */
    private ScreenRegion determineTransmitRegion() {
        var end = _displayPane.getCursorPosition().copy();
        var start = _displayPane.getCursorPosition().copy();
        var extent = 1;
        while (_displayPane.getCharacterCell(start).getCharacter() != ASCII_SOE) {
            if (start.atHome()) {
                return new ScreenRegion(start, end, extent);
            }
            _displayPane.backupCoordinates(start);
            extent++;
        }

        return new ScreenRegion(start, end, extent);
    }

    /**
     * Creates a UTS stream to be sent to the host, observing transmit mode (all, var, or changed).
     * Encode the stream from the first SOE preceding the cursor up to the cursor itself.
     * If no SOE is found, the stream begins with the home position.
     * Format is STX ESC VT Y X NUL SI [SOE] text ETX
     * @param transmitMode mode which controls the data to be transmitted
     */
    private void transmit(final TransmitMode transmitMode) {
        var output = new UTSByteBuffer(4096);

        try {
            output.put(ASCII_SOH).put(ASCII_STX);
            var region = determineTransmitRegion();
            var coord = region.getStartingPosition();
            new UTSCursorPositionPrimitive(coord.getRow(), coord.getColumn()).serialize(output);

            var field = _displayPane.getCharacterCell(coord)
                                    .getField();
            var blankCounter = 0;
            while (coord.compareTo(region.getEndingPosition()) <= 0) {
                if (transmitMode == TransmitMode.ALL
                    || (transmitMode == TransmitMode.VARIABLE && !field.isProtected())
                    || (transmitMode == TransmitMode.CHANGED && field.isChanged())) {

                    // Serialize FCC sequence if we're at the first character of a field
                    if (coord.equals(field.getCoordinates()) && field instanceof ExplicitField eField) {
                        var prim = new UTSFCCSequencePrimitive(eField.getCoordinates().getRow(),
                                                               eField.getCoordinates().getColumn(),
                                                               eField.getAttributes());
                        prim.setSendExpanded(_settings.getSendExpandedFCCs());
                        prim.setSendExpandedColor(_settings.getSendColorFCCs());
                        prim.serialize(output);
                    }

                    // Grab the character - if it's a blank, just increment the blank counter so we can
                    // avoid including field-trailing or line-trailing blanks - else add the character
                    // to the stream.
                    var ch = _displayPane.getCharacterCell(coord)
                                         .getCharacter();
                    if (ch == ASCII_SP) {
                        blankCounter++;
                    } else {
                        output.putSpaces(blankCounter);
                        blankCounter = 0;
                        output.put(ch);
                    }

                    // If we're at the end of a line, dispense with pending blanks and
                    // insert a CR (but not if we're at the end of the display).
                    if (_displayPane.coordinatesAtEndOfLine(coord)) {
                        if (!_displayPane.coordinatesAtEndOfDisplay(coord)) {
                            output.put(ASCII_CR);
                        }
                        blankCounter = 0;
                    }

                    // Move to the next cell and update the field.
                    // If we're still in the same field, this has no consequence.
                    _displayPane.advanceCoordinates(coord);
                    field = _displayPane.getCharacterCell(coord)
                                        .getField();
                } else {
                    // The characters in this field should not be transmitted - move to the next field.
                    field = _displayPane.getNextField(field);
                    coord = field.getCoordinates();
                }
            }

            // Serialize any remaining trailing blanks and ETX
            output.putSpaces(blankCounter).put(ASCII_ETX);
        } catch (CoordinateException ex) {
            // This should never happen.
        }

        try {
            _socketHandler.write(new TextMessage(output.setPointer(0).getBuffer()));
            _statusPane.setKeyboardLocked(true);
        } catch (IOException ex) {
            disconnect(false);
            IO.println("Cannot send data: " + ex.getMessage());
        }
    }
}

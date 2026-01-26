/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.exceptions.*;
import com.bearsnake.komodo.kutelib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.kutelib.messages.MessageWaitMessage;
import com.bearsnake.komodo.kutelib.messages.TextMessage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 * Implements a display with backing memory, cursor and protocol handling, and a status line.
 */
public class Terminal extends Pane implements SocketChannelListener {

    private static final byte[] MESSAGE_POLL = {ASCII_SOH, ASCII_ETX};
    private static final byte[] STATUS_POLL = {ASCII_SOH, ASCII_ENQ, ASCII_ETX};
    private static final byte[] MESSAGE_WAITING_NOTIFICATION = {ASCII_SOH, ASCII_BEL, ASCII_STX, ASCII_ETX};
    private static final byte[] DISCONNECT_REQUEST = {ASCII_SOH, ASCII_DLE, ASCII_EOT, ASCII_STX, ASCII_ETX};

    private DisplayPane _displayPane;
    private StatusPane _statusPane;
    private ControlPagePane _controlPagePane;
    private DisplayPane _activeDisplayPane;

    private boolean _returnKeyIsTransmit;
    private boolean _sendExpandedFCCs;
    private boolean _sendColorFCCs;

    private String _hostName;
    private int _hostPort;
    private SocketChannelHandler _socketHandler;

    private PrintMode _printMode;
    private TransferMode _transferMode;
    private TransmitMode _transmitMode;

    private Emphasis _emphasis;
    private EmphasisAction _emphasisAction;

    public Terminal(final DisplayGeometry initialGeometry,
                    final FontInfo initialFontInfo,
                    final UTSColorSet colorSet,
                    final boolean returnKeyIsTransmit,
                    final boolean sendExpandedFCCs,
                    final boolean sendColorFCCs) {
        _statusPane = new StatusPane(initialGeometry, initialFontInfo, colorSet);
        _displayPane = new DisplayPane(initialGeometry, initialFontInfo, colorSet, _statusPane);
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

        _returnKeyIsTransmit = returnKeyIsTransmit;
        _sendExpandedFCCs = sendExpandedFCCs;
        _sendColorFCCs = sendColorFCCs;

        getChildren().addAll(_displayPane, _statusPane);
        setMinHeight(_displayPane.getHeight() + _statusPane.getHeight());
        setMinWidth(_displayPane.getWidth());
        setPrefHeight(_displayPane.getHeight() + _statusPane.getHeight());
        setPrefWidth(_displayPane.getWidth());
        reset();

        _hostName = "127.0.0.1";//TODO remove
        _hostPort = 2200;//TODO remove
    }

    /**
     * Closes the terminal and cleans up resources
     */
    public void close() {
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

        if (_socketHandler != null) {
            _socketHandler.close();
        }
    }

    /**
     * Connects to the given host
     */
    public void connect() {
        if (_socketHandler == null) {
            try {
                InetSocketAddress address = new InetSocketAddress(_hostName, _hostPort);
                SocketChannel channel = SocketChannel.open(address);
                _socketHandler = new SocketChannelHandler(channel);
                _socketHandler.setListener(this);
                _statusPane.setConnected(true);
                reset();
            } catch (IOException ex) {
                System.err.println("Error creating socket: " + ex.getMessage());
            }
        }
    }

    /**
     * Disconnects from the host
     */
    public void disconnect() {
        if (_socketHandler != null) {
            _socketHandler.close();
            _socketHandler = null;
            // TODO display alert message box
            _statusPane.setConnected(false);
        }
    }

    private boolean controlPageIsActive() {
        return _controlPagePane != null;
    }

    /**
     * Handles a keypress event, redirecting execution according to the key.
     * This fires multiple times when the key is held down.
     * @param keyCode keycode representing the key which was pressed
     */
    public void handleKeyPressed(final KeyCode keyCode) {
        if (_statusPane.isKeyboardLocked()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        switch (keyCode) {
            case DELETE -> kbDeleteInLine();
            case INSERT -> kbInsertInLine();
            case BACK_SPACE -> kbBackSpace();
            case HOME -> kbCursorToHome();
            case DOWN -> kbScanDown();
            case LEFT -> kbScanLeft();
            case RIGHT -> kbScanRight();
            case UP -> kbScanUp();
            case ENTER -> {
                if (_returnKeyIsTransmit) {
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
            default -> System.out.println(keyCode); // TODO remove, this is for development only
        }
    }

    /**
     * Special handling for certain keystrokes.
     * We translate them to desired actions.
     * @param str the character sequence represented by the keystroke.
     */
    public void handleKeyTyped(final String str) {
        if (!str.isEmpty()) {
            var ch = str.charAt(0);
            switch (ch) {
                case 0x02 -> /* ctrl b */ {
                    // affects the display pane even if control page is active...
                    // control page fields have fixed colors, so they don't react to this anyway.
                    _displayPane.cycleBackgroundColor();
                }
                case 0x03 -> /* ctrl c */ {
                    // affects the display pane even if control page is active...
                    // control page fields have fixed colors, so they don't react to this anyway.
                    _displayPane.cycleTextColor();
                }
                case 0x08 -> /* ctrl h */ kbBackSpace();
                //TODO requires reference back to TerminalStack or hosting application
                // case 0x14 -> /* ctrl t */ com.bearsnake.komodo.kute.Kute.getInstance().cycleTabs();
                case ASCII_LF -> kbPutCharacter(ASCII_LF);
                case ASCII_FF -> kbPutCharacter(ASCII_FF);
                default -> {
                    if ((ch >= ASCII_SP) && (ch <= ASCII_DEL)) {
                        kbPutCharacter((byte) (ch & 0xFF));
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
        if ((_socketHandler == null) || _statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        try {
            _socketHandler.writeMessage(new MessageWaitMessage());
            _statusPane.setKeyboardLocked(true);
        } catch (IOException ex) {
            // TODO what do do?
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
            _socketHandler.writeMessage(msg);
            _statusPane.setKeyboardLocked(true);
        } catch (IOException ex) {
            // TODO what do do?
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
            var settings = new ControlPagePane.Settings(_printMode, _transferMode, _transmitMode);
            _controlPagePane = new ControlPagePane(_displayPane.getGeometry(),
                                                   _displayPane.getFontInfo(),
                                                   _displayPane.getColorSet(),
                                                   _statusPane,
                                                   settings);

            getChildren().add(_controlPagePane);
            _activeDisplayPane = _controlPagePane;
            _displayPane.dimDisplay(true);
        }

        return result;
    }

    // ---------------------------------------------------------------------------------------------
    // Methods which handle input from the host
    // ---------------------------------------------------------------------------------------------

    private void ingestAddEmphasis(final UTSInputStream stream)
        throws InvalidEscapeSequenceException,
               IncompleteEscapeSequenceException {
        // ESC Y code - we've already ingested ESC and Y
        if (stream.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }

        var code = (byte) stream.read();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new InvalidEscapeSequenceException("Invalid emphasis code");
        }

        _emphasis = new Emphasis(code);
        _emphasisAction = EmphasisAction.ADD;
    }

    private void ingestEscape(final UTSInputStream stream)
        throws InvalidEscapeSequenceException,
               IncompleteEscapeSequenceException {
        if (stream.atEnd()) {
            throw new InvalidEscapeSequenceException("Incomplete escape sequence");
        }

        var ch2 = (byte) stream.read();
        if ((ch2 == 0x20) && (_emphasisAction != EmphasisAction.NONE)) {
            _emphasis = null;
            _emphasisAction = EmphasisAction.NONE;
            return;
        } else if ((ch2 >= 0x20) && (ch2 <= 0x2F)) {
            _emphasis = new Emphasis(ch2);
            _emphasisAction = EmphasisAction.SET;
            return;
        }

        switch (ch2) {
            case ASCII_HT -> _activeDisplayPane.putCharacter(ASCII_HT, _emphasisAction, _emphasis);
            case ASCII_DC1 -> transmit(TransmitMode.ALL);
            case ASCII_DC2 -> printTransparent();
            case ASCII_DC4 -> _statusPane.setKeyboardLocked(true);
            case 'C' -> _activeDisplayPane.deleteInDisplay();
            case 'D' -> _activeDisplayPane.insertInDisplay();
            case 'E' -> {} // transfer changed fields - not implemented
            case 'F' -> {} // transfer variable fields - not implemented
            case 'G' -> {} // transfer all fields - not implemented
            case 'H' -> printForm();
            case 'K' -> _activeDisplayPane.eraseToEndOfField();
            case 'L' -> _statusPane.setKeyboardLocked(false);
            case 'M' -> _activeDisplayPane.eraseDisplay();
            case 'T' -> { // Tell the terminal to respond with the cursor position
                if (!controlPageIsActive()) {
                    try {
                        var outStrm = new UTSOutputStream(16).writeCursorPosition(_activeDisplayPane.getCursorPosition());
                        _socketHandler.writeMessage(new TextMessage(outStrm.getBuffer()));
                        _statusPane.setKeyboardLocked(true);
                    } catch (IOException ex) {
                        // TODO what to do?
                        IO.println("Error sending cursor position request: " + ex.getMessage());
                    }
                }
            }
            case 'X' -> ingestPutCharacterHex(stream);
            case 'Y' -> ingestAddEmphasis(stream);
            case 'Z' -> ingestRemoveEmphasis(stream);
            case '[' -> _activeDisplayPane.putCharacter(ASCII_ESC, _emphasisAction, _emphasis);
            case '{' -> ingestPutCharacterDecimal(stream);
            case 'a' -> _activeDisplayPane.eraseUnprotectedData();
            case 'b' -> _activeDisplayPane.eraseToEndOfLine();
            case 'c' -> _activeDisplayPane.deleteInLine();
            case 'd' -> _activeDisplayPane.insertInLine();
            case 'e' -> _activeDisplayPane.cursorToHome();
            case 'f' -> _activeDisplayPane.scanUp();
            case 'g' -> _activeDisplayPane.scanLeft();
            case 'h' -> _activeDisplayPane.scanRight();
            case 'i' -> _activeDisplayPane.scanDown();
            case 'j' -> _activeDisplayPane.insertLine();
            case 'k' -> _activeDisplayPane.deleteLine();
            case 'o' -> toggleControlPage();
            case 't' -> transmit(TransmitMode.CHANGED);
            case 'u' -> _activeDisplayPane.clearChangedBits();
            case 'w' -> _activeDisplayPane.fccClear();
            case 'y' -> _activeDisplayPane.duplicateLine();
            case 'z' -> _activeDisplayPane.tabBackward();
            default -> throw new InvalidEscapeSequenceException(ch2);
        }
    }

    /**
     * Ingests a message from a UTS stream - the portion between STX and ETX.
     */
    private void ingestMessage(final UTSInputStream stream) throws StreamException {
        _emphasis.clear();
        _emphasisAction = EmphasisAction.NONE;

        try {
            while (stream.available() > 0) {
                var coord = stream.readCursorPosition();
                if (coord != null) {
                    _activeDisplayPane.setCursorPosition(coord);
                    continue;
                }

                var field = stream.readFCC();
                if (field != null) {
                    if (field.getCoordinates() == null) {
                        field.setCoordinates(_activeDisplayPane.getCursorPosition());
                    } else {
                        _activeDisplayPane.setCursorPosition(field.getCoordinates());
                    }
                    _activeDisplayPane.putFCC(field);
                    continue;
                }

                var ch = (byte) stream.read();
                switch (ch) {
                    case ASCII_HT -> _activeDisplayPane.tabForward();
                    case ASCII_CR -> _activeDisplayPane.cursorReturn();
                    case ASCII_DC1 -> transmit(TransmitMode.VARIABLE);
                    case ASCII_DC2 -> printAll();
                    case ASCII_DC4 -> { // lock keyboard (same as ESC DC4)
                        _statusPane.setKeyboardLocked(true);
                    }
                    case ASCII_SUB -> _activeDisplayPane.putCharacter(ASCII_SUB, _emphasisAction, _emphasis);
                    case ASCII_ESC -> ingestEscape(stream);
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
        } finally {
            _activeDisplayPane.scheduleDrawDisplay();
        }
    }

    // Wrapper around putCharacter(), but we have to get the character from the stream as decimal digits
    // representing a value from 0 to 127 inclusive, followed by a '}' character
    private void ingestPutCharacterDecimal(final UTSInputStream stream)
        throws IncompleteEscapeSequenceException,
               InvalidEscapeSequenceException {
        var value = 0;
        boolean digits = false;
        boolean done = false;
        while (!stream.atEnd()) {
            var ch = (byte) stream.read();
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
    private void ingestPutCharacterHex(final UTSInputStream stream)
        throws IncompleteEscapeSequenceException,
               InvalidEscapeSequenceException {
        if (stream.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }
        var hex1 = (char)stream.read();

        if (stream.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }
        var hex2 = (char)stream.read();

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

    private void ingestRemoveEmphasis(final UTSInputStream stream)
        throws InvalidEscapeSequenceException,
               IncompleteEscapeSequenceException {
        // ESC Z code - we've already ingested ESC and Y
        if (stream.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }

        var code = (byte) stream.read();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new InvalidEscapeSequenceException("Invalid emphasis code");
        }

        _emphasis = new Emphasis(code);
        _emphasisAction = EmphasisAction.REMOVE;
    }

    /**
     * Handles UTS traffic from socket.
     */
    @Override
    public void trafficReceived(final byte[] data) {
        // Conventional messages from the host follow this pattern:
        //  SOH RID SID DID * ETX BCC
        // We have no need for RID/SID/DID, nor for BCC. The Komodo host does not send RID/SID/DID, nor BCC.

        // Messages we can get from the host:
        //  SOH ETX             - message poll: requests any message the terminal has queued
        //  SOH ENQ ETX         - status poll: requests any non-text message the terminal has queued
        //  SOH STX message ETX - message to terminal
        //  SOH BEL STX ETX     - set message waiting on terminal
        //  SOH DLE EOT STX ETX - causes terminal to drop the session (host could just drop the TCP session anyway...)

        // If the message is empty, this is a poll for any messages which the terminal has queued up for us
        // NOTE THAT WE DO NOT DO POLLING FOR TERMINAL INPUT - Communications partner should not do this.
        if (data.length == 0) {
            return;
        }

        if (Arrays.equals(data, STATUS_POLL)) {
            IO.println("Ignoring status poll");
            return;
        }

        if (Arrays.equals(data, MESSAGE_POLL)) {
            IO.println("Ignoring message poll");
            return;
        }

        if (Arrays.equals(data, MESSAGE_WAITING_NOTIFICATION)) {
            _statusPane.setMessageWaiting(true);
            return;
        }

        if (Arrays.equals(data, DISCONNECT_REQUEST)) {
            disconnect();
            return;
        }

        // If the message begins with SOH-STX, it is a text message to the terminal
        if ((data[0] == ASCII_SOH) && (data[1] == ASCII_STX)) {
            var len = data.length - 3;
            var stream = new UTSInputStream(data, 2, len);
            try {
                ingestMessage(stream);
            } catch (StreamException se) {
                System.out.printf("Error at byte %d\n", len - stream.available() + 1);
                _statusPane.setErrorIndicator(true);
            }
            return;
        }

        // Anything else is an error. Handle it.
        System.out.println("Invalid stream from host");
        _statusPane.setErrorIndicator(true);
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

    private void pendCoordinate(final ByteArrayOutputStream strm,
                                final int coordinate) {
        if (coordinate <= 80) {
            strm.write((byte) (coordinate + 31));
        } else {
            var slop = coordinate - 81;
            strm.write((slop >> 4) + 0x75);
            strm.write((byte) (slop & 0x0F));
        }
    }

    private void pendCoordinates(final ByteArrayOutputStream strm,
                                 final Coordinates coordinates) {
        strm.write(ASCII_ESC);
        strm.write(ASCII_VT);
        pendCoordinate(strm, coordinates.getRow());
        pendCoordinate(strm, coordinates.getColumn());
        strm.write(ASCII_NUL);
        strm.write(ASCII_SI);
    }

    private void pendFCC(final ByteArrayOutputStream strm,
                         final Field field) {
        strm.write(ASCII_US);
        pendCoordinates(strm, field.getCoordinates());
        if (_sendColorFCCs) {
            if (field.getTextColor() != null) {
                if (field.getBackgroundColor() != null) {
                    // write both colors in a single color byte after O character
                    strm.write(0x20);
                    byte b = 0x40;
                    b |= field.getTextColor().getByteValue();
                    b |= (byte) (field.getBackgroundColor().getByteValue() << 3);
                    strm.write(b);
                } else {
                    // write text color after O character
                    strm.write(0x21);
                    strm.write(0x40 | field.getTextColor().getByteValue());
                }
            } else if (field.getBackgroundColor() != null) {
                // write bg color after O character
                strm.write(0x22);
                strm.write(0x40 | field.getBackgroundColor().getByteValue());
            }
        }
        if (_sendExpandedFCCs || _sendColorFCCs) {
            // pend expanded FCC M and N characters
            byte m = 0x40;
            m |= (byte)(field.getIntensity() == Intensity.NONE ? 0x01 : 0x00);
            m |= (byte)(field.getIntensity() == Intensity.LOW ? 0x02 : 0x00);
            m |= (byte)(field.isChanged() ? 0x04 : 0x00);
            m |= (byte)(field.isTabStop() ? 0x08 : 0x00);

            byte n = 0x40;
            n |= (byte)(field.isAlphabeticOnly() ? 0x01 : 0x00);
            n |= (byte)(field.isNumericOnly() ? 0x02 : 0x00);
            n |= (byte)(field.isProtected() ? 0x03 : 0x00);
            n |= (byte)(field.isRightJustified() ? 0x04 : 0x00);
            n |= (byte)(field.isBlinking() ? 0x08 : 0x00);
            n |= (byte)(field.isReverseVideo() ? 0x10 : 0x00);

            strm.write(m);
            strm.write(n);
        } else {
            // pend UTS400-compatible FCC M and N characters
            byte m = 0x30;
            m |= field.isBlinking() ? 0x03 :
                 (byte) switch (field.getIntensity()) {
                     case NONE -> 0x01;
                     case LOW -> 0x02;
                     case NORMAL -> 0x00;
                 };
            m |= (byte)(field.isChanged() ? 0x04 : 0x00);
            m |= (byte)(field.isTabStop() ? 0x08 : 0x00);

            byte n = 0x30;
            n |= (byte)(field.isAlphabeticOnly() ? 0x01 : 0x00);
            n |= (byte)(field.isNumericOnly() ? 0x02 : 0x00);
            n |= (byte)(field.isProtected() ? 0x03 : 0x00);
            n |= (byte)(field.isRightJustified() ? 0x04 : 0x00);

            strm.write(m);
            strm.write(n);
        }
    }

    /**
     * Creates a UTS stream to be sent to the host, observing transmit mode (all, var, or changed).
     * Encode the stream from the first SOE preceding the cursor up to the cursor itself.
     * If no SOE is found, the stream begins with the home position.
     * Format is STX ESC VT Y X NUL SI [SOE] text ETX
     * @param xmitMode mode which controls the data to be transmitted
     */
    private void transmit(final TransmitMode xmitMode) {
        IO.println("Transmitting...");//TODO
        var strm = new ByteArrayOutputStream(1024);
        strm.write(ASCII_STX);

        var region = determineTransmitRegion();
        var coord = region.getStartingPosition();
        pendCoordinates(strm, coord);

        var field = _displayPane.getCharacterCell(coord).getField();
        var blankCounter = 0;
        while (coord.compareTo(region.getEndingPosition()) <= 0) {
            if (xmitMode == TransmitMode.ALL
                    || (xmitMode == TransmitMode.VARIABLE && !field.isProtected())
                    || (xmitMode == TransmitMode.CHANGED && field.isChanged())) {

                // Serialize FCC sequence if we're at the first character of a field
                if (coord.equals(field.getCoordinates())) {
                    pendFCC(strm, field);
                }

                // Grab the character - if it's a blank, just increment the blank counter so we can
                // avoid including field-trailing or line-trailing blanks - else add the character
                // to the stream.
                var ch = _displayPane.getCharacterCell(coord).getCharacter();
                if (ch == ASCII_SP) {
                    blankCounter++;
                } else {
                    for (int i = 0; i < blankCounter; i++) {
                        strm.write(ASCII_SP);
                        blankCounter = 0;
                    }
                    strm.write(ch);
                }

                // If we're at the end of a line, dispense with pending blanks and
                // insert a CR (but not if we're at the end of the display).
                if (_displayPane.coordinatesAtEndOfLine(coord)) {
                    if (!_displayPane.coordinatesAtEndOfDisplay(coord)) {
                        strm.write(ASCII_CR);
                    }
                    blankCounter = 0;
                }

                // Move to the next cell and update the field.
                // If we're still in the same field, this has no consequence.
                _displayPane.advanceCoordinates(coord);
                field = _displayPane.getCharacterCell(coord).getField();
            } else {
                // The characters in this field should not be transmitted - move to the next field.
                field = _displayPane.getNextField(field);
                coord = field.getCoordinates();
            }
        }

        // Serialize any remaining trailing blanks, then send the stream.
        for (int i = 0; i < blankCounter; i++) {
            strm.write(ASCII_SP);
        }

        try {
            var msg = new TextMessage(strm.toByteArray(), 0, strm.size());
            _socketHandler.writeMessage(msg);
            _statusPane.setKeyboardLocked(true);
        } catch (IOException ex) {
            // TODO do something here... not sure what though
            IO.println("Failed to send message: " + ex.getMessage());
        }
    }
}

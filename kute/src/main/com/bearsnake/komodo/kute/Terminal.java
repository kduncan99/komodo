/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import com.bearsnake.komodo.kute.exceptions.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.awt.*;
import java.io.*;

import static com.bearsnake.komodo.kute.Constants.*;
import static com.bearsnake.komodo.kute.Intensity.*;
import static com.bearsnake.komodo.kute.TransmitMode.*;

/**
 * Implements a display with backing memory, cursor and protocol handling, and a status line.
 */
public class Terminal extends Pane {

    private static final byte[] STATUS_MESSAGE = { ASCII_SOH, ASCII_DLE, 0x3B, ASCII_ETX };
    private static final byte[] MESSAGE_WAIT_MESSAGE = { ASCII_SOH, ASCII_BEL, ASCII_ETX };
    private static final byte[] NO_TRAFFIC_MESSAGE = { ASCII_EOT, ASCII_EOT, ASCII_ETX };

    private DisplayPane _displayPane;
    private StatusPane _statusPane;
    private ControlPagePane _controlPagePane;
    private DisplayPane _activeDisplayPane;

    private boolean _returnKeyIsTransmit;
    private boolean _sendExpandedFCCs;
    private boolean _sendColorFCCs;

    private String _hostName;
    private int _hostPort;
    private SocketHandler _socketHandler;

    private PrintMode _printMode;
    private TransferMode _transferMode;
    private TransmitMode _transmitMode;

    private Emphasis _emphasis;
    private EmphasisAction _emphasisAction;

    private ByteArrayOutputStream _pendingToHost;
    private boolean _sendCursorPosition;
    private boolean _sendStatus;
    private Integer _sendFunctionKey;   // Either this can be non-null, or _sendMessageWait can be true, but not both.
    private boolean _sendMessageWait;   // The one would over-ride the other, except kb lock generally prevents that.

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
                _socketHandler = new SocketHandler(this, _hostName, _hostPort);
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
                    _displayPane.cycleBackgroundColor();
                    if (_controlPagePane != null) {
                        _controlPagePane.cycleBackgroundColor();
                    }
                }
                case 0x03 -> /* ctrl c */ {
                    _displayPane.cycleTextColor();
                    if (_controlPagePane != null) {
                        _controlPagePane.cycleTextColor();
                    }
                }
                case 0x08 -> /* ctrl h */ kbBackSpace();
                case 0x14 -> /* ctrl t */ Kute.getInstance().cycleTabs();
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
     * The host wants us to send something. We do so according to:
     *      Priority 1: Status message
     *      Priority 2: Text or host-initiated transmit messages (only if includeText is set)
     *      Priority 3: MsgWait or Function Key messages
     * Terminal should send:
     *      for status:             SOH DLE 0x3B ETX (always ready)
     *      for text:               STX text ETX
     *          (include host-initiated XMIT)
     *      for cursor position:    STX ESC VT row col NUL SI ETX
     *      for function keys:      SOH code ETX (see 2-2 pg 2-12 for codes)
     *      for no-traffic:         EOT EOT ETX
     * @param includeText true if host will accept a text message
     */
    public void poll(final boolean includeText) {
        _statusPane.pulsePollIndicator();

        if (_sendStatus) {
            _socketHandler.send(new StreamBuffer(STATUS_MESSAGE, 0, STATUS_MESSAGE.length));
            _sendStatus = false;
            return;
        }

        if (includeText) {
            if (_pendingToHost != null) {
                var sb = new StreamBuffer(_pendingToHost.toByteArray(), 0, _pendingToHost.size());
                _socketHandler.send(sb);
                _pendingToHost = null;
                _statusPane.setKeyboardLocked(false);
                return;
            }

            if (_sendCursorPosition) {
                var strm = new ByteArrayOutputStream(16);
                strm.write(ASCII_STX);
                pendCoordinates(strm, _activeDisplayPane.getCursorPosition());
                strm.write(ASCII_ETX);
                _socketHandler.send(new StreamBuffer(strm.toByteArray(), 0, strm.size()));
                _sendCursorPosition = false;
                _statusPane.setKeyboardLocked(false);
                return;
            }
        }

        if (_sendMessageWait) {
            _socketHandler.send(new StreamBuffer(MESSAGE_WAIT_MESSAGE, 0, MESSAGE_WAIT_MESSAGE.length));
            _sendMessageWait = false;
            _statusPane.setKeyboardLocked(false);
            return;
        }

        if (_sendFunctionKey != null) {
            byte code = switch (_sendFunctionKey) {
                case 1 -> 0x37;
                case 2 -> 0x47;
                case 3 -> 0x57;
                case 4 -> 0x67;
                default -> (byte)(_sendFunctionKey - 5 + 0x20);
            };

            var bb = new byte[]{ASCII_SOH, code, ASCII_ETX};
            _socketHandler.send(new StreamBuffer(bb, 0, bb.length));
            _sendFunctionKey = null;
            _statusPane.setKeyboardLocked(false);
            return;
        }

        _socketHandler.send(new StreamBuffer(NO_TRAFFIC_MESSAGE, 0, NO_TRAFFIC_MESSAGE.length));
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
        _pendingToHost = null;
        _sendCursorPosition = false;
        _sendStatus = true;
        _sendFunctionKey = null;
        _sendMessageWait = false;
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

        var cPos = _activeDisplayPane.getCursorPosition();
        var dialog = new FieldDialog(Kute.getInstance().getScene().getWindow(), cPos);
        var opt = dialog.showDialog();
        opt.ifPresent(field -> _activeDisplayPane.putFCC(field));
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
        if (_statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        _statusPane.setKeyboardLocked(true);
        _sendMessageWait = true;
        _sendFunctionKey = null;
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
        if (_statusPane.isKeyboardLocked() || controlPageIsActive()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if ((fKey >= 1) && (fKey <= 22)) {
            _sendMessageWait = false;
            _sendFunctionKey = fKey;
            _statusPane.setKeyboardLocked(true);
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
        if (_statusPane.isKeyboardLocked() || controlPageIsActive()) {
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

    private void ingestAddEmphasis(final StreamBuffer strm)
        throws InvalidEscapeSequenceException,
               IncompleteEscapeSequenceException {
        // ESC Y code - we've already ingested ESC and Y
        if (strm.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }

        var code = strm.get();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new InvalidEscapeSequenceException("Invalid emphasis code");
        }

        _emphasis = new Emphasis(code);
        _emphasisAction = EmphasisAction.ADD;
    }

    private int ingestCoordinate(final StreamBuffer strm) throws CoordinateException {
        if (strm.atEnd()) {
            throw new CoordinateException("Incomplete or missing coordinate");
        }
        var ch = strm.get();
        if ((ch >= 0x20) && (ch <= 0x6f)) {
            return (ch - 0x20 + 1);
        } else if (ch >= 0x75) {
            if (strm.atEnd()) {
                throw new CoordinateException("Incomplete or missing coordinate");
            }
            var ch2 = strm.get();
            if (ch2 >= 70) {
                return 81 + ((ch - 0x75) << 4) + (ch2 & 0x0F);
            }
        }
        throw new CoordinateException("Invalid coordinate");
    }

    private void ingestCursorPosition(final StreamBuffer strm) throws CoordinateException, InvalidEscapeSequenceException {
        // ESC VT row column SI - we've already parsed ESC and VT
        var row = ingestCoordinate(strm);
        var column = ingestCoordinate(strm);
        if (strm.atEnd()) {
            throw new InvalidEscapeSequenceException("Incomplete ESC VT sequence");
        }
        if (strm.get() != ASCII_SI) {
            throw new InvalidEscapeSequenceException("Missing SI at end of ESC VT sequence");
        }

        _activeDisplayPane.setCursorPosition(row, column);
    }

    private void ingestEscape(final StreamBuffer strm)
        throws InvalidEscapeSequenceException,
               CoordinateException,
               IncompleteEscapeSequenceException {
        if (strm.atEnd()) {
            throw new InvalidEscapeSequenceException("Incomplete escape sequence");
        }

        var ch2 = strm.get();
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
            case ASCII_VT -> ingestCursorPosition(strm);
            case ASCII_DC1 -> transmit(ALL);
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
                    _sendCursorPosition = true;
                    _statusPane.setKeyboardLocked(true);
                }
            }
            case 'X' -> ingestPutCharacterHex(strm);
            case 'Y' -> ingestAddEmphasis(strm);
            case 'Z' -> ingestRemoveEmphasis(strm);
            case '[' -> _activeDisplayPane.putCharacter(ASCII_ESC, _emphasisAction, _emphasis);
            case '{' -> ingestPutCharacterDecimal(strm);
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
            case 't' -> transmit(CHANGED);
            case 'u' -> _activeDisplayPane.clearChangedBits();
            case 'w' -> _activeDisplayPane.fccClear();
            case 'y' -> _activeDisplayPane.duplicateLine();
            case 'z' -> _activeDisplayPane.tabBackward();
            default -> throw new InvalidEscapeSequenceException(ch2);
        }
    }

    // Ingests UTS stream which defines the O (optionally), M and N attribute bytes,
    // creates a Field representing those bytes at the cursor position, and establishes the field
    // in the display at the top of the display stack.
    private void ingestFCC(final StreamBuffer strm) throws FCCSequenceException, CoordinateException {
        // EM [ O ... ] M N -- We've already ingested EM
        if (strm.atEnd()) {
            throw new FCCIncompleteSequenceException();
        }

        var field = new ExplicitField(_activeDisplayPane.getCursorPosition());
        var ch = strm.get();
        byte m;
        byte n;
        if ((ch >= 0x20) && (ch <= 0x2F)) {
            // This is an 'O'
            if (ch == 0x20) {
                // next char is 0b01gggttt ggg=background color, ttt=text color
                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch2 = strm.get();
                field.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
                field.setBackgroundColor(UTSColor.fromByte((byte)((ch2 >> 3) & 0x07)));
            } else if (ch == 0x21) {
                // next char is text color in lower 3 bits
                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch2 = strm.get();
                field.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
            } else if (ch == 0x22) {
                // next char is bg color in lower 3 bits
                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch2 = strm.get();
                field.setBackgroundColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
            } else if (ch == 0x23) {
                // next chars are text color in lower 3 bits, then bg color in lower 3 bits
                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch2 = strm.get();
                field.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));

                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch3 = strm.get();
                field.setBackgroundColor(UTSColor.fromByte((byte)(ch3 & 0x07)));
            } else {
                // reserved color code - error for now
                throw new FCCSequenceException("Invalid O byte", ch);
            }

            if (strm.atEnd()) {
                throw new FCCIncompleteSequenceException();
            }
            m = strm.get();
        } else {
            m = ch;
        }

        if (strm.atEnd()) {
            throw new FCCIncompleteSequenceException();
        }
        n = strm.get();

        if ((m >= 0x30) && (m <= 0x3f) && (n >= 0x30) && (n <= 0x3f)) {
            // UTS400 compatible FCC sequence
            switch (m & 0x03) {
                case 0x00 -> field.setIntensity(NORMAL);
                case 0x01 -> field.setIntensity(NONE);
                case 0x02 -> field.setIntensity(LOW);
                case 0x03 -> field.setBlinking(true);
            }
            field.setChanged((m & 0x04) == 0x00);
            field.setTabStop((m & 0x08) == 0x00);
            switch (n & 0x03) {
                case 0x00 -> {}
                case 0x01 -> field.setAlphabeticOnly(true);
                case 0x02 -> field.setNumericOnly(true);
                case 0x03 -> field.setProtected(true);
            }
            field.setRightJustified((n & 0x04) == 0x04);
        } else if ((m >= 0x40) && (n >= 0x40)) {
            // Expanded FCC sequence
            if ((m & 0x01) == 0x01) { field.setIntensity(NONE); }
            if ((m & 0x02) == 0x02) { field.setIntensity(LOW); }
            field.setChanged((m & 0x04) == 0x00);
            field.setTabStop((m & 0x08) == 0x00);
            field.setProtectedEmphasis((m & 0x20) == 0x20);
            switch (n & 0x03) {
                case 0x00 -> {}
                case 0x01 -> field.setAlphabeticOnly(true);
                case 0x02 -> field.setNumericOnly(true);
                case 0x03 -> field.setProtected(true);
            }
            field.setRightJustified((n & 0x04) == 0x04);
            field.setBlinking((n & 0x08) == 0x08);
            field.setReverseVideo((n & 0x10) == 0x10);
        } else {
            throw new FCCSequenceException(m, n);
        }

        _activeDisplayPane.putFCC(field);
    }

    private void ingestFCCWithPosition(final StreamBuffer strm) throws FCCSequenceException, CoordinateException {
        // US row col [ O ... ] M N -- We've already ingested US
        int row = ingestCoordinate(strm);
        int column = ingestCoordinate(strm);
        _activeDisplayPane.setCursorPosition(row, column);
        ingestFCC(strm);
    }

    /**
     * Ingests a message from a UTS stream - the portion between STX and ETX.
     */
    private void ingestMessage(final StreamBuffer strm) throws StreamException {
        _emphasis.clear();
        _emphasisAction = EmphasisAction.NONE;

        try {
            while (!strm.atEnd()) {
                var ch = strm.get();
                switch (ch) {
                    case ASCII_HT -> _activeDisplayPane.tabForward();
                    case ASCII_CR -> _activeDisplayPane.cursorReturn();
                    case ASCII_DC1 -> transmit(VARIABLE);
                    case ASCII_DC2 -> printAll();
                    case ASCII_DC4 -> { // lock keyboard (same as ESC DC4)
                        _statusPane.setKeyboardLocked(true);
                    }
                    case ASCII_EM -> ingestFCC(strm);
                    case ASCII_SUB -> _activeDisplayPane.putCharacter(ASCII_SUB, _emphasisAction, _emphasis);
                    case ASCII_ESC -> ingestEscape(strm);
                    case ASCII_US -> ingestFCCWithPosition(strm);
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
    private void ingestPutCharacterDecimal(final StreamBuffer strm)
        throws IncompleteEscapeSequenceException,
               InvalidEscapeSequenceException {
        var value = 0;
        boolean digits = false;
        boolean done = false;
        while (!strm.atEnd()) {
            var ch = strm.get();
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
    private void ingestPutCharacterHex(final StreamBuffer strm)
        throws IncompleteEscapeSequenceException,
               InvalidEscapeSequenceException {
        if (strm.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }
        var hex1 = (char)strm.get();

        if (strm.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }
        var hex2 = (char)strm.get();

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

    private void ingestRemoveEmphasis(final StreamBuffer strm)
        throws InvalidEscapeSequenceException,
               IncompleteEscapeSequenceException {
        // ESC Z code - we've already ingested ESC and Y
        if (strm.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }

        var code = strm.get();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new InvalidEscapeSequenceException("Invalid emphasis code");
        }

        _emphasis = new Emphasis(code);
        _emphasisAction = EmphasisAction.REMOVE;
    }

    /**
     * Handles UTS traffic from socket.
     */
    public void ingestTraffic(
        final byte[] data,
        final int length
    ) {
        // Conventional messages from the host follow this pattern:
        //  SOH RID SID DID * ETX BCC
        // We have no need for RID/SID/DID, nor for BCC. The Komodo host does not send RID/SID/DID, nor BCC.
        // The calling routine strips SOH and ETX, so we are left with the '*' portion of the message.

        // Messages we can get from the host:
        //  SOH ETX             - message poll: requests any message the terminal has queued
        //  SOH ENQ ETX         - status poll: requests any non-text message the terminal has queued
        //  SOH STX message ETX - message to terminal
        //  SOH BEL STX ETX     - set message waiting on terminal
        //  SOH DLE EOT STX ETX - causes terminal to drop the session (host could just drop the TCP session anyway...)

        // If the message is empty, this is a poll for any messages which the terminal has queued up for us
        if (length == 0) {
            poll(true);
            return;
        }

        // If the message has a single ENQ byte, this is a poll for non-text messages.
        if ((length == 1) && (data[0] == ASCII_ENQ)) {
            poll(false);
            return;
        }

        // If the message begins with STX, it is a text message to the terminal
        if ((length > 0) && (data[0] == ASCII_STX)) {
            var strm = new StreamBuffer(data, 1, length - 1);
            try {
                ingestMessage(strm);
            } catch (StreamException se) {
                System.out.printf("Error at byte %d\n", strm.getPosition());
                _statusPane.setErrorIndicator(true);
            }
            return;
        }

        // If the content is BEL STX, set message waiting
        if ((length == 2) && (data[0] == ASCII_BEL) && (data[1] == ASCII_STX)) {
            _statusPane.setMessageWaiting(true);
            return;
        }

        // If the content is DLE EOT STX then the host is asking us to drop the session
        if ((length == 3) && (data[0] == ASCII_DLE) && (data[1] == ASCII_EOT) && (data[2] == ASCII_STX)) {
            disconnect();
            return;
        }

        // Anything else is an error. Handle it.
        System.out.println("Invalid stream from host");
        _statusPane.setErrorIndicator(true);
    }

    // ---------------------------------------------------------------------------------------------
    // Methods which create or contribute content to the output stream
    // ---------------------------------------------------------------------------------------------

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
                         final Coordinates coordinates,
                         final FieldAttributes attr) {
        strm.write(ASCII_US);
        pendCoordinates(strm, coordinates);
        if (_sendColorFCCs) {
            if (attr.getTextColor() != null) {
                if (attr.getBackgroundColor() != null) {
                    // write both colors in a single color byte after O character
                    strm.write(0x20);
                    byte b = 0x40;
                    b |= attr.getTextColor().getByteValue();
                    b |= (byte) (attr.getBackgroundColor().getByteValue() << 3);
                    strm.write(b);
                } else {
                    // write text color after O character
                    strm.write(0x21);
                    strm.write(0x40 | attr.getTextColor().getByteValue());
                }
            } else if (attr.getBackgroundColor() != null) {
                // write bg color after O character
                strm.write(0x22);
                strm.write(0x40 | attr.getBackgroundColor().getByteValue());
            }
        }
        if (_sendExpandedFCCs || _sendColorFCCs) {
            // pend expanded FCC M and N characters
            byte m = 0x40;
            m |= (byte)(attr.getIntensity() == NONE ? 0x01 : 0x00);
            m |= (byte)(attr.getIntensity() == LOW ? 0x02 : 0x00);
            m |= (byte)(attr.isChanged() ? 0x04 : 0x00);
            m |= (byte)(attr.isTabStop() ? 0x08 : 0x00);

            byte n = 0x40;
            n |= (byte)(attr.isAlphabeticOnly() ? 0x01 : 0x00);
            n |= (byte)(attr.isNumericOnly() ? 0x02 : 0x00);
            n |= (byte)(attr.isProtected() ? 0x03 : 0x00);
            n |= (byte)(attr.isRightJustified() ? 0x04 : 0x00);
            n |= (byte)(attr.isBlinking() ? 0x08 : 0x00);
            n |= (byte)(attr.isReverseVideo() ? 0x10 : 0x00);

            strm.write(m);
            strm.write(n);
        } else {
            // pend UTS400-compatible FCC M and N characters
            byte m = 0x30;
            m |= attr.isBlinking() ? 0x03 :
                 (byte) switch (attr.getIntensity()) {
                     case NONE -> 0x01;
                     case LOW -> 0x02;
                     case NORMAL -> 0x00;
                 };
            m |= (byte)(attr.isChanged() ? 0x04 : 0x00);
            m |= (byte)(attr.isTabStop() ? 0x08 : 0x00);

            byte n = 0x30;
            n |= (byte)(attr.isAlphabeticOnly() ? 0x01 : 0x00);
            n |= (byte)(attr.isNumericOnly() ? 0x02 : 0x00);
            n |= (byte)(attr.isProtected() ? 0x03 : 0x00);
            n |= (byte)(attr.isRightJustified() ? 0x04 : 0x00);

            strm.write(m);
            strm.write(n);
        }
    }

    /**
     * Does not actually transmit anything, but it does put together a UTS stream to be sent to the host
     * upon the next pull. Observes transmit mode (all, var, or changed).
     * Encode the stream from the first SOE preceding the cursor up to the cursor itself.
     * If no SOE is found, the stream begins with the home position.
     * Format is STX ESC VT Y X NUL SI [SOE] text ETX
     * @param xmitMode mode which controls the data to be transmitted
     */
    private void transmit(final TransmitMode xmitMode) {
        /* TODO
        var region = determineTransmitRegion();
        var coord = region.getStartingCoordinates();

        boolean isChanged = false;
        boolean isProtected = false;
        var ctlAttr = getControllingAttributes(coord);
        if (ctlAttr != null) {
            isChanged = ctlAttr.isChanged();
            isProtected = ctlAttr.isProtected();
        }

        var strm = new ByteArrayOutputStream(1024);
        strm.write(ASCII_STX);
        pendCoordinates(strm, coord);
        var blanks = new ByteArrayOutputStream(_template.getColumns());
        for (int cx = 0; cx < region.getExtent(); cx++) {
            var cell = getCharacterCell(coord);
            var cellAttr = cell.getAttributes();
            if (cellAttr != null) {
                // this is the beginning of a new field. set up new attributes.
                isChanged = cellAttr.isChanged();
                isProtected = cellAttr.isProtected();
                blanks.reset();
            }

            if ( (xmitMode == ALL)
                || ((xmitMode == VARIABLE) && !isProtected)
                || ((xmitMode == CHANGED) && isChanged) ) {
                if (cellAttr != null) {
                    pendFCC(strm, coord, cellAttr);
                }

                if (cell.getCharacter() == ASCII_SP) {
                    blanks.write(ASCII_SP);
                } else {
                    strm.write(blanks.toByteArray(), 0, blanks.size());
                    blanks.reset();
                    strm.write(cell.getCharacter());
                }
            }

            if (coordinateIsEndOfLine(coord)) {
                blanks.reset();
                strm.write(ASCII_CR);
            }

            advanceCoordinates(coord);
        }

        strm.write(blanks.toByteArray(), 0, blanks.size());
        strm.write(ASCII_ETX);

        _keyboardLocked = true;
        _pendingToHost = strm;
        draw(false, true);
         */
    }
}

//    // ---------------------------------------------------------------------------------------------
//    // Generally-useful functionality
//    // ---------------------------------------------------------------------------------------------
//
//    private boolean controlPageIsActive() {
//        return _displayStack.size() > 1;
//    }
//
//    // Find the region which begins with the SOE to the left of the cursor,
//    // up to and including the cursor. We always transmit the SOE if it exists.
////    private ScreenRegion determineTransmitRegion() {
////        var end = _cursorPosition.copy();
////        var start = _cursorPosition.copy();
////        var extent = 1;
////        while (getCharacterCell(start).getCharacter() != ASCII_SOE) {
////            if (coordinateIsHomePosition(start)) {
////                return new ScreenRegion(start, end, extent);
////            }
////            backupCoordinates(start);
////            extent++;
////        }
////
////        return new ScreenRegion(start, end, extent);
////    }
//
//    // Retrieves the character cell at the indicated coordinate
////    private CharacterCell getCharacterCell(final Coordinates coordinates) {
////        return _characterCells[coordinates.getRow() - 1][coordinates.getColumn() - 1];
////    }
//
//    // Returns the controlling attributes for the given coordinates.
//    // If there are none, we return null.
////    private FieldAttributes getControllingAttributes(final Coordinates coordinates) {
////        var coord = getControllingAttributesCoordinates(coordinates);
////        return getCharacterCell(coord).getAttributes();
////    }
//
//    // Returns the coordinates of the cell which contains the field attributes which govern the given coordinates.
//    // Generally, this would be the FCC at that cell, or the first previous cell thereto.
//    // If there is no controlling FCC, we return the home position.
////    private Coordinates getControllingAttributesCoordinates(final Coordinates coordinates) {
////        var coord = coordinates.copy();
////        do {
////            var cell = getCharacterCell(coord);
////            var attr = cell.getAttributes();
////            if (attr != null) {
////                break;
////            }
////            backupCoordinates(coord);
////        } while (!coordinateIsHomePosition(coord));
////
////        return coord;
////    }
//
////    private boolean isCellProtected(final Coordinates coordinates) {
////        // Checks the cell to see if it is located in a protected field
////        var attr = getControllingAttributes(coordinates);
////        return (attr != null) && (attr.isProtected());
////    }

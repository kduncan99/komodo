/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import com.bearsnake.komodo.kute.exceptions.*;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.awt.*;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

import static com.bearsnake.komodo.kute.Constants.*;
import static com.bearsnake.komodo.kute.Intensity.*;
import static com.bearsnake.komodo.kute.TransmitMode.*;
import static com.bearsnake.komodo.kute.UTSColor.*;

/**
 * Implements a display with backing memory.
 * For a terminal with multiple displays, there will be one containing virtual terminal,
 * one keyboard handler, one TCP connection, etc... and there will be 1 Display per display.
 */
public class Terminal extends VBox {

    private static final Font FONT = Font.font("Courier New", FontWeight.BOLD, 16);
    private static final byte[] STATUS_MESSAGE = { ASCII_SOH, ASCII_DLE, 0x3B, ASCII_ETX };
    private static final byte[] MESSAGE_WAIT_MESSAGE = { ASCII_SOH, ASCII_BEL, ASCII_ETX };
    private static final byte[] NO_TRAFFIC_MESSAGE = { ASCII_EOT, ASCII_EOT, ASCII_ETX };

    private final Template _template;
    private SocketHandler _socketHandler;

    private CharacterCell[][] _characterCells;
    private Coordinates _cursorPosition;

    private final Timer _timer = new Timer();
    private int _blinkCounter;
    private boolean _blinkCharacter; // drives character blinking
    private boolean _blinkCursor;    // drives cursor blinking

    private final int _characterHeight;
    private final int _characterWidth;
    private final Canvas _displayPane;
    private final Canvas _statusPane;

    private PrintMode _printMode;
    private TransmitMode _transmitMode;
    private boolean _keyboardLocked = false;
    private boolean _messageWaiting = false;
    private boolean _errorFlag = false;
    private int _pollCountdown = 0;

    private Emphasis _ingestEmphasis;
    private boolean _ingestSetEmphasis = false;
    private boolean _ingestAddEmphasis = false;
    private boolean _ingestRemoveEmphasis = false;

    private ByteArrayOutputStream _pendingToHost;
    private boolean _sendCursorPosition;
    private boolean _sendStatus;
    private Integer _sendFunctionKey;   //  Either this can be non-null, or _sendMessageWait can be true, but not both.
    private boolean _sendMessageWait;   //  The one would over-ride the other, except kb lock generally prevents that.

    // ---------------------------------------------------------------------------------------------
    // Basic API for other classes to invoke
    // ---------------------------------------------------------------------------------------------

    public Terminal(final Template template) throws KuteException {
        _template = template;
        _displayPane = new Canvas();
        _statusPane = new Canvas();

        _printMode = PrintMode.ALL;
        _transmitMode = TransmitMode.ALL;

        var gcDisplay = _displayPane.getGraphicsContext2D();
        var gcStatus = _statusPane.getGraphicsContext2D();
        gcDisplay.setFont(FONT);
        gcStatus.setFont(FONT);

        Text text = new Text("ABCDEFGHIJ");
        text.setFont(FONT);
        _characterHeight = (int)text.getLayoutBounds().getHeight();
        _characterWidth = (int)(text.getLayoutBounds().getWidth() / 10.0) + 1;

        setSpacing(1.0f);
        getChildren().add(_displayPane);
        getChildren().add(_statusPane);

        reconfigure(_template.getRows(), _template.getColumns());
        draw(true, true);

        var blinkPollTask = new TimerTask() {
            @Override
            public void run() {
                blinkPoll();
            }
        };

        connect();
        _timer.scheduleAtFixedRate(blinkPollTask, 0, 250);
    }

    public void close() {
        _timer.cancel();
        if (_socketHandler != null) {
            _socketHandler.close();
        }
    }

    public void connect() {
        if (_socketHandler == null) {
            try {
                _socketHandler = new SocketHandler(this, _template.getHostName(), _template.getPort());
                reset();
            } catch (IOException ex) {
                System.err.println("Error creating socket: " + ex.getMessage());
            }
        }
    }

    public void disconnect() {
        if (_socketHandler != null) {
            _socketHandler.close();
            _socketHandler = null;
            draw(false, true);
        }
    }

    public void handleKeyPressed(final KeyCode keyCode) {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        switch (keyCode) {
            case DELETE -> kbDeleteInLine();
            case INSERT -> kbInsertInLine();
            case BACK_SPACE -> kbBackSpace();
            case HOME -> cursorToHome();
            case DOWN -> scanDown();
            case LEFT -> scanLeft();
            case RIGHT -> scanRight();
            case UP -> scanUp();
            case ENTER -> {
                if (_template.getReturnKeyIsXMIT()) {
                    kbTransmit();
                } else {
                    carriageReturn();
                }
            }
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

    public void handleKeyTyped(final String str) {
        if (!str.isEmpty()) {
            var ch = str.charAt(0);
            switch (ch) {
                case 0x02 -> { // ctrl-b
                    // cycle through default background colors
                    _template.setBackgroundColor(nextUTSColor(_template.getBackgroundColor()));
                    draw(true, true);
                }
                case 0x03 -> { // ctrl-c
                    // cycle through default text colors
                    _template.setTextColor(nextUTSColor(_template.getTextColor()));
                    draw(true, true);
                }
                case 0x08 -> { // ctrl-h
                    kbBackSpace();
                }
                case ASCII_HT -> {
                    kbPutCharacter(ASCII_HT);
                    draw(true, true);
                }
                case ASCII_LF -> {
                    kbPutCharacter(ASCII_LF);
                    draw(true, true);
                }
                case ASCII_FF -> {
                    kbPutCharacter(ASCII_FF);
                    draw(true, true);
                }
                default -> {
                    if ((ch >= ASCII_SP) && (ch <= ASCII_DEL)) {
                        kbPutCharacter((byte) (ch & 0xFF));
                    }
                }
            }
        }
    }

    public void handleKeyReleased(final KeyCode keyCode) {
        // We don't resolve protected cells until after a key is released...
        // at least not for cursor scanning key input which we get from handleKeyPressed().
        // This strategy allows for scanning through protected cells.
        resolveProtectedCell();
    }

    public void reconfigure(final int rows, final int columns) throws KuteException {
        if ((rows < 16) || (rows > 256)) {
            throw new ParameterException("Display.reconfigure", "rows", rows);
        }
        if ((columns < 64) || (columns > 256)) {
            throw new ParameterException("Display.reconfigure", "columns", columns);
        }

        _template.setRows(rows);
        _template.setColumns(columns);

        _characterCells = new CharacterCell[rows][columns];
        for (int rx = 0; rx < rows; rx++) {
            for (int cx = 0; cx < columns; cx++) {
                _characterCells[rx][cx] = new CharacterCell();
            }
        }
        _cursorPosition = Coordinates.HOME_POSITION.copy();

        var w = columns * _characterWidth;
        var h = rows * _characterHeight;
        _displayPane.setHeight(h);
        _displayPane.setWidth(w);
        _statusPane.setHeight(_characterHeight);
        _statusPane.setWidth(w);
        draw(true, true);
    }

    public void setDefaultBackgroundColor(final UTSColor color) {
        _template.setBackgroundColor(color);
        draw(true, true);
    }

    public void setDefaultTextColor(final UTSColor color) {
        _template.setTextColor(color);
        draw(true, true);
    }

    // ---------------------------------------------------------------------------------------------
    // Methods which handle input from the host
    // ---------------------------------------------------------------------------------------------

    private void ingestAddEmphasis(final StreamBuffer strm) throws EscapeSequenceException {
        // ESC Y code - we've already ingested ESC and Y
        if (strm.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }

        var code = strm.get();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new EscapeSequenceException("Invalid emphasis code");
        }

        _ingestEmphasis = new Emphasis(code);
        _ingestAddEmphasis = true;
        _ingestRemoveEmphasis = false;
        _ingestSetEmphasis = false;
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

    private void ingestCursorPosition(final StreamBuffer strm) throws CoordinateException, EscapeSequenceException {
        // ESC VT row column SI - we've already parsed ESC and VT
        var row = ingestCoordinate(strm);
        var column = ingestCoordinate(strm);
        if (strm.atEnd()) {
            throw new EscapeSequenceException("Incomplete ESC VT sequence");
        }
        if (strm.get() != ASCII_SI) {
            throw new EscapeSequenceException("Missing SI at end of ESC VT sequence");
        }

        setCursorPosition(new Coordinates(row, column));
    }

    private void ingestEscape(final StreamBuffer strm) throws EscapeSequenceException, CoordinateException {
        if (strm.atEnd()) {
            throw new EscapeSequenceException("Incomplete escape sequence");
        }

        var ch2 = strm.get();
        if ((ch2 == 0x20) && (_ingestAddEmphasis || _ingestRemoveEmphasis || _ingestSetEmphasis)) {
            _ingestEmphasis = null;
            _ingestAddEmphasis = false;
            _ingestRemoveEmphasis = false;
            _ingestSetEmphasis = false;
            return;
        } else if ((ch2 >= 0x20) && (ch2 <= 0x2F)) {
            _ingestEmphasis = new Emphasis(ch2);
            _ingestAddEmphasis = false;
            _ingestRemoveEmphasis = false;
            _ingestSetEmphasis = true;
            return;
        }

        switch (ch2) {
            case ASCII_HT -> putCharacter(ASCII_HT);
            case ASCII_VT -> ingestCursorPosition(strm);
            case ASCII_DC1 -> transmit(ALL);
            case ASCII_DC2 -> printTransparent();
            case ASCII_DC4 -> {
                _keyboardLocked = true;
                draw(false, true);
            }
            case 'C' -> deleteInDisplay();
            case 'D' -> insertInDisplay();
            case 'E' -> {} // transfer changed fields - not implemented
            case 'F' -> {} // transfer variable fields - not implemented
            case 'G' -> {} // transfer all fields - not implemented
            case 'H' -> printForm();
            case 'K' -> eraseToEndOfField();
            case 'L' -> {
                _keyboardLocked = false;
                draw(false, true);
            }
            case 'M' -> eraseDisplay();
            case 'T' -> {
                _sendCursorPosition = true;
                _keyboardLocked = true;
                draw(false, true);
            }
            case 'X' -> putCharacterHex(strm);
            case 'Y' -> ingestAddEmphasis(strm);
            case 'Z' -> ingestRemoveEmphasis(strm);
            case '[' -> putCharacter(ASCII_ESC);
            case '{' -> putCharacterDecimal(strm);
            case 'a' -> eraseUnprotectedData();
            case 'b' -> eraseToEndOfLine();
            case 'c' -> deleteInLine();
            case 'd' -> insertInLine();
            case 'e' -> cursorToHome();
            case 'f' -> scanUp();
            case 'g' -> scanLeft();
            case 'h' -> scanRight();
            case 'i' -> scanDown();
            case 'j' -> insertLine();
            case 'k' -> deleteLine();
            case 'o' -> toggleControlPage();
            case 't' -> transmit(CHANGED);
            case 'u' -> clearChangedBits();
            case 'w' -> clearFCC();
            case 'y' -> duplicateLine();
            case 'z' -> tabBackward();
            default -> throw new EscapeSequenceException(ch2);
        }
    }

    private void ingestFCC(final StreamBuffer strm) throws FCCSequenceException, CoordinateException {
        // EM [ O ... ] M N -- We've already ingested EM
        if (strm.atEnd()) {
            throw new FCCIncompleteSequenceException();
        }

        var attr = new FieldAttributes();
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
                attr.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
                attr.setBackgroundColor(UTSColor.fromByte((byte)((ch2 >> 3) & 0x07)));
            } else if (ch == 0x21) {
                // next char is text color in lower 3 bits
                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch2 = strm.get();
                attr.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
            } else if (ch == 0x22) {
                // next char is bg color in lower 3 bits
                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch2 = strm.get();
                attr.setBackgroundColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
            } else if (ch == 0x23) {
                // next chars are text color in lower 3 bits, then bg color in lower 3 bits
                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch2 = strm.get();
                attr.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));

                if (strm.atEnd()) {
                    throw new FCCIncompleteSequenceException();
                }
                byte ch3 = strm.get();
                attr.setBackgroundColor(UTSColor.fromByte((byte)(ch3 & 0x07)));
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
                case 0x00 -> attr.setIntensity(NORMAL);
                case 0x01 -> attr.setIntensity(NONE);
                case 0x02 -> attr.setIntensity(LOW);
                case 0x03 -> attr.setBlinking(true);
            }
            attr.setChanged((m & 0x04) == 0x00);
            attr.setTabStop((m & 0x08) == 0x00);
            switch (n & 0x03) {
                case 0x00 -> {}
                case 0x01 -> attr.setAlphabeticOnly(true);
                case 0x02 -> attr.setNumericOnly(true);
                case 0x03 -> attr.setProtected(true);
            }
            attr.setRightJustified((n & 0x04) == 0x04);
        } else if ((m >= 0x40) && (n >= 0x40)) {
            // Expanded FCC sequence
            if ((m & 0x01) == 0x01) { attr.setIntensity(NONE); }
            if ((m & 0x02) == 0x02) { attr.setIntensity(LOW); }
            attr.setChanged((m & 0x04) == 0x00);
            attr.setTabStop((m & 0x08) == 0x00);
            attr.setProtectedEmphasis((m & 0x20) == 0x20);
            switch (n & 0x03) {
                case 0x00 -> {}
                case 0x01 -> attr.setAlphabeticOnly(true);
                case 0x02 -> attr.setNumericOnly(true);
                case 0x03 -> attr.setProtected(true);
            }
            attr.setRightJustified((n & 0x04) == 0x04);
            attr.setBlinking((n & 0x08) == 0x08);
            attr.setReverseVideo((n & 0x10) == 0x10);
        } else {
            throw new FCCSequenceException(m, n);
        }

        getCharacterCell(_cursorPosition).setAttributes(attr);
    }

    private void ingestFCCWithPosition(final StreamBuffer strm) throws FCCSequenceException, CoordinateException {
        // US row col [ O ... ] M N -- We've already ingested US
        int row = ingestCoordinate(strm);
        int column = ingestCoordinate(strm);
        setCursorPosition(new Coordinates(row, column));
        ingestFCC(strm);
    }

    /**
     * Ingests a message from a UTS stream - the portion between STX and ETX.
     */
    private void ingestMessage(final StreamBuffer strm) throws StreamException {
        _ingestEmphasis.clear();
        _ingestAddEmphasis = false;
        _ingestRemoveEmphasis = false;
        _ingestSetEmphasis = false;

        try {
            while (!strm.atEnd()) {
                var ch = strm.get();
                switch (ch) {
                    case ASCII_HT -> tabForward();
                    case ASCII_CR -> cursorReturn();
                    case ASCII_DC1 -> transmit(VARIABLE);
                    case ASCII_DC2 -> printAll();
                    case ASCII_DC4 -> { // lock keyboard (same as ESC DC4)
                        _keyboardLocked = true;
                        draw(false, true);
                    }
                    case ASCII_EM -> ingestFCC(strm);
                    case ASCII_SUB -> putCharacter(ASCII_SUB);
                    case ASCII_ESC -> ingestEscape(strm);
                    case ASCII_US -> ingestFCCWithPosition(strm);
                    case ASCII_FS -> putCharacter(ASCII_FS);
                    case ASCII_GS -> putCharacter(ASCII_GS);
                    case ASCII_SOE -> putCharacter(ASCII_SOE);
                    default -> {
                        if (ch >= ASCII_SP) {
                            putCharacter(ch);
                        } else {
                            throw new InvalidCharacterException(ch);
                        }
                    }
                }
            }
        } finally {
            draw(true, true);
        }
    }

    private void ingestRemoveEmphasis(final StreamBuffer strm) throws EscapeSequenceException {
        // ESC Z code - we've already ingested ESC and Y
        if (strm.atEnd()) {
            throw new IncompleteEscapeSequenceException();
        }

        var code = strm.get();
        if ((code < 0x20) || (code > 0x2F)) {
            throw new EscapeSequenceException("Invalid emphasis code");
        }

        _ingestEmphasis = new Emphasis(code);
        _ingestAddEmphasis = false;
        _ingestRemoveEmphasis = true;
        _ingestSetEmphasis = false;
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
                _errorFlag = true;
            }
            draw(true, true);
            return;
        }

        // If the content is BEL STX, set message waiting
        if ((length == 2) && (data[0] == ASCII_BEL) && (data[1] == ASCII_STX)) {
            setMessageWaiting();
            return;
        }

        // If the content is DLE EOT STX then the host is asking us to drop the session
        if ((length == 3) && (data[0] == ASCII_DLE) && (data[1] == ASCII_EOT) && (data[2] == ASCII_STX)) {
            disconnect();
            return;
        }

        // Anything else is an error. Handle it.
        System.out.println("Invalid stream from host");
        _errorFlag = true;
        draw(false, true);
    }

    public void setMessageWaiting() {
        _messageWaiting = true;
        draw(false, true);
    }

    public void poll(final boolean includeText) {
        // Host wants us to send something. We do so according to:
        //      Priority 1: Status message
        //      Priority 2: Text or host-initiated transmit messages (only if includeText is set)
        //      Priority 3: MsgWait or Function Key messages
        // Terminal should send:
        //      for status:             SOH DLE 0x3B ETX (always ready)
        //      for text:               STX text ETX
        //          (include host-initiated XMIT)
        //      for cursor position:    STX ESC VT row col NUL SI ETX
        //      for function keys:      SOH code ETX (see 2-2 pg 2-12 for codes)
        //      for no-traffic:         EOT EOT ETX
        _pollCountdown = 2;

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
                _keyboardLocked = false;
                draw(false, true);
                return;
            }

            if (_sendCursorPosition) {
                var strm = new ByteArrayOutputStream(16);
                strm.write(ASCII_STX);
                pendCoordinates(strm, _cursorPosition);
                strm.write(ASCII_ETX);
                _socketHandler.send(new StreamBuffer(strm.toByteArray(), 0, strm.size()));
                _sendCursorPosition = false;
                _keyboardLocked = false;
                draw(false, true);
                return;
            }
        }

        if (_sendMessageWait) {
            _socketHandler.send(new StreamBuffer(MESSAGE_WAIT_MESSAGE, 0, MESSAGE_WAIT_MESSAGE.length));
            _sendMessageWait = false;
            _keyboardLocked = false;
            draw(false, true);
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
            _keyboardLocked = false;
            draw(false, true);
            return;
        }

        _socketHandler.send(new StreamBuffer(NO_TRAFFIC_MESSAGE, 0, NO_TRAFFIC_MESSAGE.length));
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
        if (_template.getSendColorFCCs()) {
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
        if (_template.getSendExpandedFCCs() || _template.getSendColorFCCs()) {
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

    private void transmit(final TransmitMode xmitMode) {
        // Does not actually transmit anything, but it does put together a UTS stream to be sent to the host
        // upon the next pull. Observes transmit mode (all, var, or changed).
        // Encode the stream from the first SOE preceding the cursor up to the cursor itself.
        // If no SOE is found, the stream begins with the home position.
        // Format is STX ESC VT Y X NUL SI [SOE] text ETX
        Coordinates coord = findPreviousSOE();
        boolean isChanged = false;
        boolean isProtected = false;
        boolean isRightJustified = false;
        var ctlAttr = getControllingAttributes(coord);
        if (ctlAttr != null) {
            isChanged = ctlAttr.isChanged();
            isProtected = ctlAttr.isProtected();
            isRightJustified = ctlAttr.isRightJustified();
        }

        var strm = new ByteArrayOutputStream(1024);
        strm.write(ASCII_STX);
        pendCoordinates(strm, coord);
        var blanks = new ByteArrayOutputStream(_template.getColumns());
        while (coord.compareTo(_cursorPosition) <= 0) {
            var cell = getCharacterCell(coord);
            var cellAttr = cell.getAttributes();
            if (cellAttr != null) {
                // this is the beginning of a new field. set up new attributes.
                isChanged = cellAttr.isChanged();
                isProtected = cellAttr.isProtected();
                isRightJustified = cellAttr.isRightJustified();
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

            if (coord.getColumn() == _template.getColumns()) {
                blanks.reset();
                strm.write(ASCII_CR);
            }

            advanceCoordinates(coord);
            if ((coord.getRow() == 1) && isRightJustified) {
                isChanged = false;
                isProtected = false;
                isRightJustified = false;
            }
        }

        strm.write(blanks.toByteArray(), 0, blanks.size());
        strm.write(ASCII_ETX);

        _keyboardLocked = true;
        _pendingToHost = strm;
        draw(false, true);
    }

    // ---------------------------------------------------------------------------------------------
    // Generally-useful functionality
    // ---------------------------------------------------------------------------------------------

    private void advanceCoordinates(final Coordinates coordinates) {
        coordinates.setColumn(coordinates.getColumn() + 1);
        if (coordinates.getColumn() > _template.getColumns()) {
            coordinates.setColumn(1);
            coordinates.setRow(coordinates.getRow() + 1);
            if (coordinates.getRow() > _template.getRows()) {
                coordinates.setRow(1);
            }
        }
    }

    private void backupCoordinates(final Coordinates coordinates) {
        coordinates.setColumn(coordinates.getColumn() - 1);
        if (coordinates.getColumn() == 0) {
            coordinates.setColumn(_template.getColumns());
            coordinates.setRow(coordinates.getRow() - 1);
            if (coordinates.getRow() == 0) {
                coordinates.setRow(_template.getRows());
            }
        }
    }

    private void blinkPoll() {
        _blinkCounter = (_blinkCounter + 1) & 0x3;
        _blinkCursor = (_blinkCounter & 0x01) != 0;
        _blinkCharacter = (_blinkCounter & 0x02) != 0;
        if (_pollCountdown > 0) {
            _pollCountdown--;
        }
        draw(true, true);
    }

    private char convertByteToCharacter(final byte b, final boolean atCursor) {
        char ch = ' ';
        if (atCursor && _blinkCursor) {
            ch = '█';
        } else {
            if (b == ASCII_SOE) {
                ch = '▷';
            } else if (b == ASCII_HT) {
                ch = '⇥';
            } else if (b == ASCII_LF) {
                ch = '↓';
            } else if (b == ASCII_FF) {
                ch = '↖';
            } else if (b == ASCII_CR) {
                ch = '↲';
            } else if (b == ASCII_DEL) {
                ch = '░';
            } else if (b == ASCII_ESC) {
                ch = '∙';
            } else if (b == ASCII_FS) {
                ch = '«';
            } else if (b == ASCII_GS) {
                ch = '»';
            } else if (b >= ASCII_SP) {
                ch = (char)b;
            }
        }
        return ch;
    }

    private Coordinates findPreviousSOE() {
        // Find the coordinates of the first previous SOE before the cursor position.
        // If none exists, return null. Most callers will just use the home position.
        var coord = _cursorPosition.copy();
        while (getCharacterCell(coord).getCharacter() != ASCII_SOE) {
            if (coord.equals(Coordinates.HOME_POSITION)) {
                return null;
            }
            backupCoordinates(coord);
        }
        return coord;
    }

    private CharacterCell getCharacterCell(final Coordinates coordinates) {
        return _characterCells[coordinates.getRow() - 1][coordinates.getColumn() - 1];
    }

    private FieldAttributes getControllingAttributes(final Coordinates coordinates) {
        var coord = getControllingAttributesCoordinates(coordinates);
        return (coord == null) ? null : getCharacterCell(coord).getAttributes();
    }

    private Coordinates getControllingAttributesCoordinates(final Coordinates coordinates) {
        var coord = coordinates.copy();

        var cell = getCharacterCell(coord);
        var attr = cell.getAttributes();
        if (attr != null) {
            return coord;
        }

        while (!coord.equals(_cursorPosition)) {
            cell = getCharacterCell(coord);
            attr = cell.getAttributes();
            if (attr != null) {
                if (attr.isRightJustified() && (coord.getRow() != coordinates.getRow())) {
                    return null;
                } else {
                    return coord;
                }
            }

            if (coord.equals(Coordinates.HOME_POSITION)) {
                break;
            }
            backupCoordinates(coord);
        }

        return null;
    }

    private Color getDarkerColor(final Color color) {
        return new Color(color.getRed() * 0.75, color.getGreen() * 0.75, color.getBlue() * 0.75, color.getOpacity());
    }

    private boolean isCellProtected(final Coordinates coordinates) {
        // Checks the cell to see if it is located in a protected field
        var attr = getControllingAttributes(coordinates);
        return (attr != null) && (attr.isProtected());
    }

    private UTSColor nextUTSColor(final UTSColor base) {
        return switch (base) {
            case BLACK -> RED;
            case RED -> GREEN;
            case GREEN -> YELLOW;
            case YELLOW -> BLUE;
            case BLUE -> MAGENTA;
            case MAGENTA -> CYAN;
            case CYAN -> WHITE;
            case WHITE -> BLACK;
        };
    }

    private void print(final boolean printAll) {
        // Print all characters from the character following the SOE which most closely precedes the cursor,
        // up to the character at the cursor. If an SOE is under the cursor, nothing is printed.
        // If printAll is not true, we replace all protected characters with spaces.
        // We do not send spaces at the end of lines; We DO send a CR at the end of each line.
        var coord = findPreviousSOE();
        if (coord == null) {
            coord = _cursorPosition.copy();
        } else {
            advanceCoordinates(coord);
        }

        var attr = getControllingAttributes(coord);
        var isProtected = (attr != null) && attr.isProtected();
        var isRightJustified = (attr != null) && attr.isRightJustified();
        var pending = new ByteArrayOutputStream();
        var blanks = new ByteArrayOutputStream();

        while (coord.compareTo(_cursorPosition) <= 0) {
            var cell = getCharacterCell(coord);
            attr = cell.getAttributes();
            if (attr != null) {
                isProtected = attr.isProtected();
                isRightJustified = attr.isRightJustified();
            } else if (isRightJustified && (coord.getColumn() == 1)) {
                isProtected = false;
                isRightJustified = false;
            }

            var ch = (!isProtected || printAll) ? cell.getCharacter() : ASCII_SP;
            if (ch != ASCII_SP) {
                pending.write(blanks.toByteArray(), 0, blanks.size());
                blanks.reset();
                pending.write(cell.getCharacter());
            } else {
                blanks.write(ASCII_SP);
            }

            advanceCoordinates(coord);
            if (coord.getColumn() == 1) {
                pending.write(ASCII_CR);
                blanks.reset();
            }
        }

        pending.write(blanks.toByteArray(), 0, blanks.size());
        sendToPrinter(pending.toByteArray(), pending.size());
    }

    private void reset() {
        for (int rx = 0; rx < _template.getRows(); rx++) {
            for (int cx = 0; cx < _template.getColumns(); cx++) {
                _characterCells[rx][cx].setCharacter(ASCII_SP);
                _characterCells[rx][cx].getEmphasis().clear();
                _characterCells[rx][cx].setAttributes(null);
            }
        }
        _cursorPosition = Coordinates.HOME_POSITION;

        _keyboardLocked = false;
        _messageWaiting = false;
        _errorFlag = false;
        _pollCountdown = 0;

        _pendingToHost = null;
        _sendCursorPosition = false;
        _sendStatus = true;
        _sendFunctionKey = null;
        _sendMessageWait = false;

        draw(true, true);
    }

    private void resolveProtectedCell() {
        // Move cursor to the next unprotected cell if the current cell is protected.
        // If there are no unprotected cells, we probably shouldn't be here, but we'll go to
        // the home position just in case.  Does NOT redraw the display - caller must do this.
        var oldCoord = _cursorPosition.copy();
        var isProtected = isCellProtected(oldCoord);
        while (isProtected) {
            advanceCoordinates(_cursorPosition);
            if (_cursorPosition.equals(oldCoord)) {
                break;
            }
            isProtected = isCellProtected(_cursorPosition);
        }
        if (isProtected) {
            _cursorPosition = Coordinates.HOME_POSITION.copy();
        }
    }

    private void sendToPrinter(final byte[] data,
                               final int length) {
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
        // TODO some gnarly stuff, isProtected, isProtectedEmphasis, isRightJustified? ... etc
    }

    public void kbClearChanged() {
        clearChangedBits();
    }

    public void kbDeleteInDisplay() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        deleteInDisplay();
    }

    public void kbDeleteInLine() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        deleteInLine();
    }

    public void kbDeleteLine() {
        deleteLine();
    }

    public void kbDuplicateLine() {
        duplicateLine();
    }

    public void kbEraseCharacter() {
        // Used when the space bar does not erase the character under it
        // (which we do not support now, but might later).
        kbPutCharacter(ASCII_SP);
    }

    public void kbEraseDisplay() {
        // Erases all data from the cursor to the end of the screen - protected or unprotected
        // (including FCCs) - NOT the same as host-initiated function.
        var coord = _cursorPosition.copy();
        do {
            var cell = getCharacterCell(coord);
            cell.setCharacter(ASCII_SP);
            cell.getEmphasis().clear();
            cell.setAttributes(null);
            advanceCoordinates(coord);
        } while (!coord.equals(Coordinates.HOME_POSITION));
        draw(true, true);
    }

    public void kbEraseToEndOfDisplay() {
        // Erases unprotected data from the cursor to the end of the screen,
        // setting (unprotected) changed field bits to false.
        // There is no host-initiated analog for this function.
        var coord = _cursorPosition.copy();
        do {
            var cell = getCharacterCell(coord);
            var attr = getControllingAttributes(coord);
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
        } while (!coord.equals(Coordinates.HOME_POSITION));
        draw(true, true);
    }

    public void kbEraseToEndOfField() {
        // Erases unprotected data from the cursor to the end of the field (or display),
        // setting (unprotected) changed field bits to false.
        // NOT the same as host-initiated function.
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
        draw(true, true);
    }

    public void kbEraseToEndOfLine() {
        // Erases unprotected data from the cursor to the end of the field (or line),
        // setting (unprotected) changed field bits to false.
        // NOT the same as host-initiated function.
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
        draw(true, true);
    }

    public void kbFCCClear() {
        var coord = _cursorPosition.copy();
        var cell = getCharacterCell(coord);
        var attr = cell.getAttributes();
        while (attr != null) {
            if (coord.equals(Coordinates.HOME_POSITION)) {
                return;
            }
            backupCoordinates(coord);
            cell = getCharacterCell(coord);
            attr = cell.getAttributes();
        }

        cell.setAttributes(null);
        draw(true, true);
    }

    /**
     * Re-enables all FCCs on the display.
     * This enables protect, emphasis protect, right-justify, alpha-only, and numeric-only
     * behavior for the affected fields.
     */
    public void kbFCCEnable() {
        for (int rx = 0; rx < _template.getRows(); rx++) {
            for (int cx = 0; cx < _template.getColumns(); cx++) {
                var attr = _characterCells[rx][cx].getAttributes();
                if (attr != null) {
                    attr.setEnabled();
                }
            }
        }
    }

    /**
     * Pulls up the FCC Generate dialog
     */
    public void kbFCCGenerate() {
        var caption = String.format("Create FCC at Row=%d Col=%d",
                                    _cursorPosition.getRow(),
                                    _cursorPosition.getColumn());
        var dialog = new FieldAttributesDialog(caption, Kute.getInstance().getScene().getWindow());
        var opt = dialog.showDialog();
        if (opt.isPresent()) {
            getCharacterCell(_cursorPosition.copy()).setAttributes(opt.get());
            draw(true, true);
        }
    }

    /**
     * Locates the first character following the cursor which contains an FCC character,
     * and moves the cursor to that position. If there are none from the cursor to the
     * end of the display, move the cursor to the home position.
     * Also, set all FCCs on the display to disabled.
     * This inhibits protect, emphasis protect, right-justify, alpha-only, and numeric-only
     * behavior for the affected fields.
     */
    public void kbFCCLocate() {
        do {
            advanceCoordinates(_cursorPosition);
            var attr = getCharacterCell(_cursorPosition).getAttributes();
            if (attr != null) {
                break;
            }
        } while (!_cursorPosition.equals(Coordinates.HOME_POSITION));

        for (int rx = 0; rx < _template.getRows(); rx++) {
            for (int cx = 0; cx < _template.getColumns(); cx++) {
                var attr = _characterCells[rx][cx].getAttributes();
                if (attr != null) {
                    attr.setEnabled();
                }
            }
        }

        draw(true, true);
    }

    public void kbHome() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        cursorToHome();
        resolveProtectedCell();
    }

    public void kbInsertInDisplay() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        insertInDisplay();
    }

    public void kbInsertInLine() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        insertInLine();
    }

    public void kbInsertLine() {
        insertLine();
    }

    /**
     * MessageWait key was pressed
     */
    public void kbMessageWait() {
        _sendMessageWait = true;
        _sendFunctionKey = null;
        draw(false, true);
    }

    /**
     * Activates the print function, according to the current print mode
     */
    public void kbPrint() {
        switch (_printMode) {
            case ALL -> printAll();
            case FORM -> printForm();
            case TRANSPARENT -> printTransparent();
        }
    }

    /**
     * Places a character on the display at the cursor location, advancing the cursor to the
     * next unprotected location. To be used for all keyboard-initiated character placement.
     */
    public void kbPutCharacter(final byte ch) {
        // If the area under the cursor is not protected, place the character on-screen
        // at the cursor, and advance the cursor to the next unprotected location.
        // We clear any special emphasis IFF the cell we are in is not emphasis-protected.

        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        var attr = getControllingAttributes(_cursorPosition);
        var isProtected = (attr != null) && attr.isProtected();
        var isProtectedEmphasis = (attr != null) && attr.isProtectedEmphasis();

        if (!isProtected) {
            var cell = getCharacterCell(_cursorPosition);
            cell.setCharacter(ch);
            if (!isProtectedEmphasis) {
                cell.getEmphasis().clear();
            }
            advanceCoordinates(_cursorPosition);
            resolveProtectedCell();
            draw(true, true);
        }
    }

    public void kbScanDown() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        scanDown();
        resolveProtectedCell();
    }

    public void kbScanLeft() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        scanLeft();
        resolveProtectedCell();
    }

    public void kbScanRight() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        scanRight();
        resolveProtectedCell();
    }

    public void kbScanUp() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        scanUp();
        resolveProtectedCell();
    }

    public void kbSendFunctionKey(final int fKey) {
        if ((fKey >= 1) && (fKey <= 22)) {
            _sendMessageWait = false;
            _sendFunctionKey = fKey;
            _keyboardLocked = true;
            draw(false, true);
        }
    }

    public void kbSetTab() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        kbPutCharacter(ASCII_HT);
        resolveProtectedCell();
    }

    public void kbSOE() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        kbPutCharacter(ASCII_SOE);
        resolveProtectedCell();
    }

    public void kbTabBackward() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        tabForward();
    }

    public void kbTabForward() {
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        tabBackward();
    }

    public void kbToggleColumnSeparator() {
        var attr = getControllingAttributes(_cursorPosition);
        if ((attr == null) || !attr.isProtectedEmphasis()) {
            var emph = getCharacterCell(_cursorPosition).getEmphasis();
            emph.setColumnSeparator(!emph.isColumnSeparator());
            advanceCoordinates(_cursorPosition);
            draw(true, true);
        }
    }

    public void kbToggleControlPage() {
        toggleControlPage();
    }

    public void kbToggleStrikeThrough() {
        var attr = getControllingAttributes(_cursorPosition);
        if ((attr == null) || !attr.isProtectedEmphasis()) {
            var emph = getCharacterCell(_cursorPosition).getEmphasis();
            emph.setStrikeThrough(!emph.isStrikeThrough());
            advanceCoordinates(_cursorPosition);
            draw(true, true);
        }
    }

    public void kbToggleUnderScore() {
        var attr = getControllingAttributes(_cursorPosition);
        if ((attr == null) || !attr.isProtectedEmphasis()) {
            var emph = getCharacterCell(_cursorPosition).getEmphasis();
            emph.setUnderscore(!emph.isUnderscore());
            advanceCoordinates(_cursorPosition);
            draw(true, true);
        }
    }

    public void kbTransfer() {
        // Not currently implemented
    }

    public void kbTransmit() {
        // This is invoked either by the Return host keyboard being pressed AND kbReturnIsXmit being true,
        // OR the transmit control button being pressed.
        // If the keyboard is locked or there is already pending output we cannot send anything.
        // Otherwise, queue a UTS stream to be sent on the next poll, and lock the keyboard.
        if (_keyboardLocked) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            transmit(_transmitMode);
        }
    }

    public void kbUnlock() {
        _keyboardLocked = false;
        draw(true, true);
    }

    // ---------------------------------------------------------------------------------------------
    // The following methods are intended as responses to input from the host. It may be that they
    // are useful in responding to keyboard input as well, but that is not guaranteed.
    // Anything keyboard-specific should use separate but similar functionality (see above).
    // ---------------------------------------------------------------------------------------------

    private void carriageReturn() {
        _cursorPosition.setColumn(1);
        _cursorPosition.setRow(_cursorPosition.getRow() + 1);
        if (_cursorPosition.getRow() > _template.getRows()) {
            _cursorPosition.setRow(1);
        }
    }

    private void clearChangedBits() {
        // Oddly-named, but no easy other name fits either.
        // Clears changed status of all FCCs on the display
        for (int rx = 0; rx < _template.getRows(); rx++) {
            for (int cx = 0; cx < _template.getColumns(); cx++) {
                var attr = _characterCells[rx][cx].getAttributes();
                if (attr != null) {
                    attr.setChanged(false);
                }
            }
        }
    }

    private void clearFCC() {
        // Erases the FCC controlling the cursor position, if any
        var coord = getControllingAttributesCoordinates(_cursorPosition);
        if (coord != null) {
            getCharacterCell(coord).setAttributes(null);
            draw(true, true);
        }
    }

    private void cursorToHome() {
        _cursorPosition = Coordinates.HOME_POSITION.copy();
        draw(true, true);
    }

    private void cursorReturn() {
        _cursorPosition.setColumn(1);
        _cursorPosition.setRow(_cursorPosition.getRow() + 1);
        if (_cursorPosition.getRow() > _template.getRows()) {
            _cursorPosition.setRow(1);
        }
        draw(true, true);
    }

    private void deleteInDisplay() {
        // If the cursor is not protected, delete the character under the cursor
        // and shift subsequent characters left, up to the end of the field or the end of the display.
        var ctlAttr = getControllingAttributes(_cursorPosition);
        boolean isProtected = false;
        boolean isProtectedEmphasis = false;
        boolean isRightJustified = false;
        if (ctlAttr != null) {
            isProtected = ctlAttr.isProtected();
            isProtectedEmphasis = ctlAttr.isProtectedEmphasis();
            isRightJustified = ctlAttr.isRightJustified();
        }

        if (!isProtected) {
            var coord = _cursorPosition.copy();
            var nextCoord = coord.copy();
            advanceCoordinates(nextCoord);

            while (!nextCoord.equals(Coordinates.HOME_POSITION) && (getCharacterCell(nextCoord).getAttributes() == null)) {
                if (isRightJustified && (nextCoord.getColumn() == 1)) {
                    break;
                }
                var cell = getCharacterCell(coord);
                var nextCell = getCharacterCell(nextCoord);
                cell.setCharacter(nextCell.getCharacter());
                if (!isProtectedEmphasis) {
                    cell.getEmphasis().set(nextCell.getEmphasis());
                }

                advanceCoordinates(coord);
                advanceCoordinates(nextCoord);
            }

            var cell = getCharacterCell(coord);
            cell.setCharacter(ASCII_SP);
            if (!isProtectedEmphasis) {
                cell.getEmphasis().clear();
            }

            draw(true, true);
        }
    }

    private void deleteInLine() {
        // If the cursor is not protected, delete the character under the cursor
        // and shift subsequent characters left, up to the end of the field or the end of the line.
        var ctlAttr = getControllingAttributes(_cursorPosition);
        boolean isProtected = false;
        boolean isProtectedEmphasis = false;
        if (ctlAttr != null) {
            isProtected = ctlAttr.isProtected();
            isProtectedEmphasis = ctlAttr.isProtectedEmphasis();
        }

        if (!isProtected) {
            var coord = _cursorPosition.copy();
            var nextCoord = coord.copy();
            advanceCoordinates(nextCoord);

            while ((nextCoord.getColumn() != 1) && (getCharacterCell(nextCoord).getAttributes() == null)) {
                var cell = getCharacterCell(coord);
                var nextCell = getCharacterCell(nextCoord);
                cell.setCharacter(nextCell.getCharacter());
                if (!isProtectedEmphasis) {
                    cell.getEmphasis().set(nextCell.getEmphasis());
                }

                advanceCoordinates(coord);
                advanceCoordinates(nextCoord);
            }

            var cell = getCharacterCell(coord);
            cell.setCharacter(ASCII_SP);
            if (!isProtectedEmphasis) {
                cell.getEmphasis().clear();
            }

            draw(true, true);
        }
    }

    private void deleteLine() {
        // Deletes the line under the cursor, shifting all subsequent lines (if any) up by one row.
        // The bottom row is initialized with blanks and no FCCs
        for (int rx = _cursorPosition.getRow() - 1; rx < _template.getRows() - 1; rx++) {
            _characterCells[rx] = _characterCells[rx + 1];
        }
        for (int cx = 0; cx < _template.getColumns(); cx++) {
            _characterCells[_template.getRows() - 1][cx] = new CharacterCell();
        }
        draw(true, true);
    }

    private void duplicateLine() {
        // Duplicates the line containing the cursor to the line below,
        // then moves the cursor to that line.
        // Ineffective if the cursor is already on the last line.
        if (_cursorPosition.getRow() < _template.getRows()) {
            for (int cx = 0; cx < _template.getColumns(); cx++) {
                _characterCells[_cursorPosition.getRow()][cx] = _characterCells[_cursorPosition.getRow() - 1][cx].copy();
            }
            _cursorPosition.setRow(_cursorPosition.getRow() + 1);
            draw(true, true);
        }
    }

    private void eraseDisplay() {
        // Erases all characters and FCCs from the cursor to the end of the display
        var coord = _cursorPosition.copy();
        while (!coord.equals(Coordinates.HOME_POSITION)) {
            _characterCells[coord.getRow() - 1][coord.getColumn() - 1] = new CharacterCell();
        }
        draw(true, true);
    }

    private void eraseToEndOfField() {
        // Only allowed if the cursor is not in a protected field...
        // Erases all characters to the end of the field, or to the end of the display
        var attr = getControllingAttributes(_cursorPosition);
        if ((attr == null) || !attr.isProtected()) {
            var coord = _cursorPosition.copy();
            var newAttr = attr;
            while (newAttr == attr) {
                int rx = coord.getRow() - 1;
                int cx = coord.getColumn() - 1;
                _characterCells[rx][cx].setCharacter(ASCII_SP);

                advanceCoordinates(coord);
                if (coord.equals(Coordinates.HOME_POSITION)) {
                    break;
                }
                newAttr = getControllingAttributes(coord);
            }
            draw(true, true);
        }
    }

    private void eraseToEndOfLine() {
        // Only allowed if the cursor is not in a protected field...
        // Erases all characters to the end of the field, or to the end of the line
        var attr = getControllingAttributes(_cursorPosition);
        if ((attr == null) || !attr.isProtected()) {
            var coord = _cursorPosition.copy();
            var newAttr = attr;
            while (newAttr == attr) {
                int rx = coord.getRow() - 1;
                int cx = coord.getColumn() - 1;
                _characterCells[rx][cx].setCharacter(ASCII_SP);

                advanceCoordinates(coord);
                if (coord.getColumn() == 1) {
                    break;
                }
                newAttr = getControllingAttributes(coord);
            }
            draw(true, true);
        }
    }

    private void eraseUnprotectedData() {
        // Erases all unprotected data (not including FCCs) from the cursor to the end of the display
        var coord = _cursorPosition.copy();
        do {
            if (!isCellProtected(coord)) {
                int rx = coord.getRow() - 1;
                int cx = coord.getColumn() - 1;
                _characterCells[rx][cx].setCharacter(ASCII_SP);
            }
            advanceCoordinates(coord);
        } while (!coord.equals(Coordinates.HOME_POSITION));

        draw(true, true);
    }

    private void insertInDisplay() {
        // If the cursor is not in a protected field, the characters from the cursor to the end of the field
        // or the end of the display, are shifted right and a blank is placed under the cursor.
        // The last character in the field or display is lost.
        // If we are in an emphasis-protected field, the emphasis characters are not affected.
        // FCCs are not affected, and the cursor does not move.
        var cursorCell = getCharacterCell(_cursorPosition);
        var cursorAttr = cursorCell.getAttributes();
        var isProtected = (cursorAttr != null) && cursorAttr.isProtected();

        if (!isProtected) {
            var isProtectedEmphasis = (cursorAttr == null) || !cursorAttr.isProtectedEmphasis();
            var isRightJustified = (cursorAttr == null) || !cursorAttr.isRightJustified();

            // find cell at end of field or display
            var coord = _cursorPosition.copy();
            advanceCoordinates(coord);
            var attr = getCharacterCell(coord).getAttributes();
            while (!coord.equals(Coordinates.HOME_POSITION)
                && (attr == null)
                && (!isRightJustified || (coord.getColumn() != 1))) {
                advanceCoordinates(coord);
                attr = getCharacterCell(coord).getAttributes();
            }
            backupCoordinates(coord);

            // Now start shifting cells to the right, backing up one at a time until
            // we're back at the cursor.
            var prevCoord = coord.copy();
            while (!coord.equals(_cursorPosition)) {
                backupCoordinates(prevCoord);
                var cell = getCharacterCell(coord);
                var prevCell = getCharacterCell(prevCoord);
                cell.setCharacter(prevCell.getCharacter());
                if (!isProtectedEmphasis) {
                    cell.getEmphasis().set(prevCell.getEmphasis());
                }
                coord.set(prevCoord);
            }

            // Blank the character under the cursor
            var cell = getCharacterCell(_cursorPosition);
            cell.setCharacter(ASCII_SP);
            if (!isRightJustified) {
                cell.getEmphasis().clear();
            }

            draw(true, true);
        }
    }

    private void insertInLine() {
        // If the cursor is not in a protected field, the characters from the cursor to the end of the field
        // or the end of the line, are shifted right and a blank is placed under the cursor.
        // The last character in the field or line is lost.
        // If we are in an emphasis-protected field, the emphasis characters are not affected.
        // FCCs are not affected, and the cursor does not move.
        var cursorCell = getCharacterCell(_cursorPosition);
        var cursorAttr = cursorCell.getAttributes();
        var isProtected = (cursorAttr != null) && cursorAttr.isProtected();

        if (!isProtected) {
            var isProtectedEmphasis = (cursorAttr == null) || !cursorAttr.isProtectedEmphasis();
            var isRightJustified = (cursorAttr == null) || !cursorAttr.isRightJustified();

            // find cell at end of field or line
            var coord = _cursorPosition.copy();
            advanceCoordinates(coord);
            var attr = getCharacterCell(coord).getAttributes();
            while ((coord.getColumn() != 1) && (attr == null)) {
                advanceCoordinates(coord);
                attr = getCharacterCell(coord).getAttributes();
            }
            backupCoordinates(coord);

            // Now start shifting cells to the right, backing up one at a time until
            // we're back at the cursor.
            var prevCoord = coord.copy();
            while (!coord.equals(_cursorPosition)) {
                backupCoordinates(prevCoord);
                var cell = getCharacterCell(coord);
                var prevCell = getCharacterCell(prevCoord);
                cell.setCharacter(prevCell.getCharacter());
                if (!isProtectedEmphasis) {
                    cell.getEmphasis().set(prevCell.getEmphasis());
                }
                coord.set(prevCoord);
            }

            // Blank the character under the cursor
            var cell = getCharacterCell(_cursorPosition);
            cell.setCharacter(ASCII_SP);
            if (!isRightJustified) {
                cell.getEmphasis().clear();
            }

            draw(true, true);
        }
    }

    private void insertLine()
    {
        // Shifts all the lines starting at the cursor down by one line,
        // with the last line on the display simply being dropped.
        // The line under the cursor is then overwritten with blanks.
        var row = _template.getRows();
        while (row > _cursorPosition.getRow()) {
            _characterCells[row] = _characterCells[row - 1];
        }
        for (int cx = 0; cx < _template.getColumns(); cx++) {
            _characterCells[row][cx] = new CharacterCell();
        }
        draw(true, true);
    }

    private void printAll() {
        print(true);
    }

    private void printForm() {
        print(false);
    }

    private void printTransparent() {
        // Print everything from the SOE most-previous to the cursor (non-inclusive)
        // up to the cursor (inclusive) - do not translate anything, do not send CRs at the end
        // of display lines. Ignore FCCs.
        var coord = findPreviousSOE();
        if (coord == null) {
            coord = _cursorPosition.copy();
        } else {
            advanceCoordinates(coord);
        }

        var strm = new ByteArrayOutputStream(2048);
        while (coord.compareTo(_cursorPosition) <= 0) {
            strm.write(getCharacterCell(coord).getCharacter());
            advanceCoordinates(coord);
        }

        sendToPrinter(strm.toByteArray(), strm.size());
    }

    private void putCharacter(final byte ch) {
        // For character placement initiated by the host.
        // We place the character (excepting in the case of SUB, which merely moves the cursor),
        // and apply the current emphasis setting if the target cell is not emphasis-protected.
        var cell = getCharacterCell(_cursorPosition);
        if (ch != ASCII_SUB) {
            cell.setCharacter(ch);
        }

        var attr = cell.getAttributes();
        if ((attr == null) || !attr.isProtectedEmphasis()) {
            if (_ingestAddEmphasis) {
                cell.getEmphasis().add(_ingestEmphasis);
            } else if (_ingestRemoveEmphasis) {
                cell.getEmphasis().remove(_ingestEmphasis);
            } else if (_ingestSetEmphasis) {
                cell.getEmphasis().set(_ingestEmphasis);
            }
        }

        advanceCoordinates(_cursorPosition);
        draw(true, true);
    }

    private void putCharacterDecimal(final StreamBuffer strm) {
        // As above, but we have to get the character from the stream as decimal digits
        // representing a value from 0 to 127 inclusive, followed by a '}' character
        // TODO
    }

    private void putCharacterHex(final StreamBuffer strm) {
        // As above, but we have to get the character from the stream as exactly two hex digits
        // TODO
    }

    private void scanDown() {
        _cursorPosition.setRow(_cursorPosition.getRow() + 1);
        if (_cursorPosition.getRow() > _template.getRows()) {
            _cursorPosition.setRow(1);
        }
        draw(true, true);
    }

    private void scanLeft() {
        _cursorPosition.setColumn(_cursorPosition.getColumn() - 1);
        if (_cursorPosition.getColumn() == 0) {
            _cursorPosition.setColumn(_template.getColumns());
            _cursorPosition.setRow(_cursorPosition.getRow() - 1);
            if (_cursorPosition.getRow() == 0) {
                _cursorPosition.setRow(_template.getRows());
            }
        }
        draw(true, true);
    }

    private void scanRight() {
        _cursorPosition.setColumn(_cursorPosition.getColumn() + 1);
        if (_cursorPosition.getColumn() > _template.getColumns()) {
            _cursorPosition.setColumn(1);
            _cursorPosition.setRow(_cursorPosition.getRow() + 1);
            if (_cursorPosition.getRow() > _template.getRows()) {
                _cursorPosition.setRow(1);
            }
        }
        draw(true, true);
    }

    private void scanUp() {
        _cursorPosition.setRow(_cursorPosition.getRow() - 1);
        if (_cursorPosition.getRow() == 0) {
            _cursorPosition.setRow(_template.getRows());
        }
        draw(true, true);
    }

    private void setCursorPosition(final Coordinates coordinates) {
        _cursorPosition.setRow(Math.min(coordinates.getRow(), _template.getRows()));
        _cursorPosition.setColumn(Math.min(coordinates.getColumn(), _template.getColumns()));
        draw(true, true);
    }

    private void tabBackward() {
        // TODO
    }

    private void tabForward() {
        // TODO
    }

    private void toggleControlPage() {
        // TODO
    }

    // ---------------------------------------------------------------------------------------------
    // Methods which handle drawing on the display
    // ---------------------------------------------------------------------------------------------

    private void draw(final boolean drawDisplay, final boolean drawStatus) {
        if (drawDisplay) {
            Platform.runLater(this::drawDisplay);
        }
        if (drawStatus) {
            Platform.runLater(this::drawStatus);
        }
    }

    private void drawDisplay() {
        // set current display attributes governing the appearance of characters,
        // and then update them as we progress.

        var gcDisplay = _displayPane.getGraphicsContext2D();
        UTSColor utsBgColor = _template.getBackgroundColor();
        UTSColor utsTextColor = _template.getTextColor();
        var intensity = Intensity.NORMAL;
        boolean blink = false;
        boolean reverse = false;
        boolean rightJustified = false;

        for (int row = 1; row <= _template.getRows(); row++) {
            for (int column = 1; column <= _template.getColumns(); column++) {
                var cell = _characterCells[row - 1][column - 1];
                var attr = cell.getAttributes();
                if (attr != null) {
                    // Attributes have changed - we need to change some things
                    utsBgColor = attr.getBackgroundColor() == null ? _template.getBackgroundColor() : attr.getBackgroundColor();
                    utsTextColor = attr.getTextColor() == null ? _template.getTextColor() : attr.getTextColor();
                    intensity = attr.getIntensity();
                    blink = attr.isBlinking();
                    reverse = attr.isReverseVideo();
                    rightJustified = attr.isRightJustified();
                } else if (rightJustified && (column == 1)) {
                    // We have exited a right-justified field by virtue of being on a new row.
                    // Set things back to defaults (since we are not on a new FCC).
                    utsBgColor = _template.getBackgroundColor();
                    utsTextColor = _template.getTextColor();
                    intensity = Intensity.NORMAL;
                    blink = false;
                    reverse = false;
                    rightJustified = false;
                }

                var byteChar = cell.getCharacter();
                var atCursor = (row == _cursorPosition.getRow()) && (column == _cursorPosition.getColumn());
                var ch = convertByteToCharacter(byteChar, atCursor);
                var effectiveBlink = blink || (byteChar == ASCII_FS) || (byteChar == ASCII_GS);

                var jfxBgColor = utsBgColor.getFxTextColor();
                var jfxTextColor = utsTextColor.getFxTextColor();
                if (effectiveBlink && _blinkCharacter) {
                    jfxTextColor = jfxBgColor;
                }

                if (intensity == Intensity.LOW) {
                    jfxBgColor = getDarkerColor(jfxBgColor);
                    jfxTextColor = getDarkerColor(jfxTextColor);
                }

                if (reverse) {
                    var temp = jfxTextColor;
                    jfxTextColor = jfxBgColor;
                    jfxBgColor = temp;
                }

                // draw background first
                var x = (column - 1) * _characterWidth;
                var yRect = ((row - 1) * _characterHeight);
                gcDisplay.setFill(jfxBgColor);
                gcDisplay.fillRect(x, yRect, _characterWidth, _characterHeight);

                // now draw text
                var yText = yRect + _characterHeight - 4;
                gcDisplay.setFill(jfxTextColor);// text color
                gcDisplay.fillText(String.valueOf(ch), x, yText);

                // now draw emphasis (if any)
                if (cell.getEmphasis().isColumnSeparator()) {
                    gcDisplay.setStroke(jfxTextColor);// text color
                    gcDisplay.setLineWidth(1.0);
                    var y = yRect + _characterHeight - 1;
                    gcDisplay.strokeLine(x, y, x, yRect);
                }
                if (cell.getEmphasis().isStrikeThrough()) {
                    gcDisplay.setStroke(jfxTextColor);// text color
                    gcDisplay.setLineWidth(1.0);
                    var y = yRect + (_characterHeight / 2);
                    gcDisplay.strokeLine(x, y, x + _characterWidth - 1, y);
                }
                if (cell.getEmphasis().isUnderscore()) {
                    gcDisplay.setStroke(jfxTextColor);// text color
                    gcDisplay.setLineWidth(1.0);
                    var y = yRect + _characterHeight - 1;
                    gcDisplay.strokeLine(x, y, x + _characterWidth - 1, y);
                }
            }
        }
    }

    private void drawStatus() {
        // Display the status line - ROW=XXX COL=XXX     ERR  CONN WAIT MSGW POLL
        var gcDisplay = _displayPane.getGraphicsContext2D();
        var gcStatus = _statusPane.getGraphicsContext2D();

        var jfxBgColor = _template.getBackgroundColor().getFxTextColor();
        var jfxTextColor = _template.getTextColor().getFxTextColor();
        var jfxTextDimColor = jfxTextColor.darker().darker();
        gcStatus.setFill(jfxBgColor);
        gcStatus.fillRect(0, 0, _statusPane.getWidth(), _statusPane.getHeight());

        gcStatus.setFill(jfxTextColor);
        gcStatus.fillText(String.format("ROW=%03d COL=%03d", _cursorPosition.getRow(), _cursorPosition.getColumn()),
                          0,
                          _characterHeight - 3);

        gcStatus.setFill(_errorFlag ? jfxTextColor : jfxTextDimColor);
        gcStatus.fillText("ERR ", 56 * _characterWidth, _characterHeight - 3);

        gcStatus.setFill((_socketHandler != null) ? jfxTextColor : jfxTextDimColor);
        gcStatus.fillText("CONN", 61 * _characterWidth, _characterHeight - 3);

        gcStatus.setFill(_keyboardLocked ? jfxTextColor : jfxTextDimColor);
        gcStatus.fillText("WAIT", 66 * _characterWidth, _characterHeight - 3);

        gcStatus.setFill(_messageWaiting ? jfxTextColor : jfxTextDimColor);
        gcStatus.fillText("MSGW", 71 * _characterWidth, _characterHeight - 3);

        gcStatus.setFill(_pollCountdown > 0 ? jfxTextColor : jfxTextDimColor);
        gcStatus.fillText("POLL", 76 * _characterWidth, _characterHeight - 3);
    }
}

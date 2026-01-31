/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.TransmitMode;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.messages.CursorPositionMessage;
import com.bearsnake.komodo.kutelib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.kutelib.messages.Message;
import com.bearsnake.komodo.kutelib.messages.StatusPollMessage;
import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.panes.Coordinates;
import com.bearsnake.komodo.kutelib.panes.DisplayGeometry;
import com.bearsnake.komodo.kutelib.panes.ExplicitField;
import com.bearsnake.komodo.kutelib.panes.UTSColor;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class ClockApp extends Application implements Runnable {

    private static final int INTER_DIGIT_MARGIN = 1;
    private static final int DIGIT_HEIGHT = 7;
    private static final int DIGIT_WIDTH = 5;

    private static final String[] ZERO = {" XXX ",
                                          "X   X",
                                          "X   X",
                                          "X   X",
                                          "X   X",
                                          "X   X",
                                          " XXX "};

    private static final String[] ONE = {"  X  ",
                                         " XX  ",
                                         "  X  ",
                                         "  X  ",
                                         "  X  ",
                                         "  X  ",
                                         "XXXXX"};

    private static final String[] TWO = {" XXX ",
                                         "X   X",
                                         "    X",
                                         "   X ",
                                         "  X  ",
                                         " X   ",
                                         "XXXXX"};

    private static final String[] THREE = {" XXX ",
                                           "X   X",
                                           "    X",
                                           "  XX ",
                                           "    X",
                                           "X   X",
                                           " XXX "};

    private static final String[] FOUR = {"X  X ",
                                          "X  X ",
                                          "X  X ",
                                          "XXXXX",
                                          "   X ",
                                          "   X ",
                                          "   X "};

    private static final String[] FIVE = {"XXXXX",
                                          "X    ",
                                          "X    ",
                                          " XXX ",
                                          "    X",
                                          "    X",
                                          "XXXXX"};

    private static final String[] SIX = {" XXX ",
                                         "X   X",
                                         "X    ",
                                         "XXXX ",
                                         "X   X",
                                         "X   X",
                                         " XXX "};

    private static final String[] SEVEN = {"XXXXX",
                                           "    X",
                                           "    X",
                                           "   X ",
                                           "  X  ",
                                           " X   ",
                                           "X    "};

    private static final String[] EIGHT = {" XXX ",
                                           "X   X",
                                           "X   X",
                                           " XXX ",
                                           "X   X",
                                           "X   X",
                                           " XXX "};

    private static final String[] NINE = {" XXX ",
                                          "X   X",
                                          "X   X",
                                          " XXXX",
                                          "    X",
                                          "    X",
                                          " XXX "};

    private static final String[] BLANK = {"     ",
                                           "     ",
                                           "     ",
                                           "     ",
                                           "     ",
                                           "     ",
                                           "     "};

    private static final String[] COLON = {"     ",
                                           "  X  ",
                                           "  X  ",
                                           "     ",
                                           "  X  ",
                                           "  X  ",
                                           "     "};

    private static HashMap<Character, String[]> CHARACTER_MAPS = new HashMap<>();
    static {
        CHARACTER_MAPS.put('0', ZERO);
        CHARACTER_MAPS.put('1', ONE);
        CHARACTER_MAPS.put('2', TWO);
        CHARACTER_MAPS.put('3', THREE);
        CHARACTER_MAPS.put('4', FOUR);
        CHARACTER_MAPS.put('5', FIVE);
        CHARACTER_MAPS.put('6', SIX);
        CHARACTER_MAPS.put('7', SEVEN);
        CHARACTER_MAPS.put('8', EIGHT);
        CHARACTER_MAPS.put('9', NINE);
        CHARACTER_MAPS.put(' ', BLANK);
        CHARACTER_MAPS.put(':', COLON);
    }

    private static class ColorScheme {

        public UTSColor _textColor;
        public UTSColor _backgroundColor;

        public ColorScheme(final UTSColor textColor,
                           final UTSColor backgroundColor) {
            _textColor = textColor;
            _backgroundColor = backgroundColor;
        }
    }

    private static final ColorScheme[] COLOR_SCHEMES = new ColorScheme[]{
        new ColorScheme(UTSColor.YELLOW, UTSColor.BLUE),
        new ColorScheme(UTSColor.CYAN, UTSColor.BLUE),
        new ColorScheme(UTSColor.RED, UTSColor.YELLOW),
        new ColorScheme(UTSColor.GREEN, UTSColor.BLACK),
        new ColorScheme(UTSColor.BLACK, UTSColor.CYAN),
        new ColorScheme(UTSColor.BLUE, UTSColor.WHITE),
        new ColorScheme(UTSColor.WHITE, UTSColor.RED),
        new ColorScheme(UTSColor.BLACK, UTSColor.MAGENTA),
    };

    private DisplayGeometry _geometry;
    private final LinkedList<Message> _inputMessages = new LinkedList<>();
    private Coordinates[] _digitCoordinates = new Coordinates[8];
    private Coordinates _hintCoordinates;
    private int _colorSchemeIndex;
    private boolean mode24Hour = true;

    public ClockApp(final KuteTestServer server) {
        super(server);
    }

    private void cycleColorScheme() {
        synchronized (this) {
            try {
                _colorSchemeIndex++;
                if (_colorSchemeIndex == COLOR_SCHEMES.length) {
                    _colorSchemeIndex = 0;
                }
                displayFCCs();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    private void cycleMode() {
        synchronized (this) {
            try {
                mode24Hour = !mode24Hour;
                displayFullTime();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    private void determineCoordinates() {
        var digitMarqueeWidth = 8 * DIGIT_WIDTH + 7 * INTER_DIGIT_MARGIN;
        var leftMargin = (_geometry.getColumns() - digitMarqueeWidth) / 2;
        var topMargin = (_geometry.getRows() - DIGIT_HEIGHT) / 2;

        var row = topMargin;
        var column = leftMargin + 1;
        for (int dx = 0; dx < 8; dx++) {
            _digitCoordinates[dx] = new Coordinates(row, column);
            column += DIGIT_WIDTH + INTER_DIGIT_MARGIN;
        }

        _hintCoordinates = new Coordinates(topMargin + DIGIT_HEIGHT + 1, leftMargin + 1);
    }

    private void displayFCCs() throws IOException {
        synchronized (this) {
            try {
                var fgColor = COLOR_SCHEMES[_colorSchemeIndex]._textColor;
                var bgColor = COLOR_SCHEMES[_colorSchemeIndex]._backgroundColor;
                var stream = new UTSByteBuffer(2048);
                stream.put(ASCII_SOH)
                      .put(ASCII_STX);
                for (var coord : _digitCoordinates) {
                    var column = coord.getColumn();
                    for (int rx = 0; rx < DIGIT_HEIGHT; rx++) {
                        var row = coord.getRow() + rx;
                        var startPos = new Coordinates(row, column);
                        var endPos = new Coordinates(row, column + DIGIT_WIDTH);
                        var startField = new ExplicitField(startPos).setTextColor(fgColor)
                                                                    .setBackgroundColor(bgColor);
                        var endField = new ExplicitField(endPos);
                        stream.putFCCSequence(startField, false, true, true);
                        stream.putFCCSequence(endField, false, true, false);
                    }
                }
                stream.put(ASCII_ETX);
                _server.sendMessage(this, stream);
            } catch (CoordinateException ex) {
                // should not happen
            }
        }
    }

    private void displayFullTime() throws IOException {
        synchronized (this) {
            var time = LocalDateTime.now();
            var hour = time.getHour();
            if (!mode24Hour) {
                if (hour == 0) {
                    hour = 12;
                } else if (hour > 12) {
                    hour -= 12;
                }
            }
            var minute = time.getMinute();
            var second = time.getSecond();
            var str = String.format("%2d:%02d:%02d", hour, minute, second);

            var stream = new UTSByteBuffer(2048);
            stream.put(ASCII_SOH)
                  .put(ASCII_STX);
            for (var pos = 0; pos < 8; pos++) {
                putCharacter(stream, pos, str.charAt(pos));
            }
            stream.putCursorToHome()
                  .put(ASCII_ETX);
            _server.sendMessage(this, stream);
        }
    }

    private void displayHints() throws IOException {
        synchronized (this) {
            try {
                var stream = new UTSByteBuffer(1024);
                stream.put(ASCII_SOH)
                      .put(ASCII_STX);
                var row = _hintCoordinates.getRow();
                var column = _hintCoordinates.getColumn();
                stream.putCursorPositionSequence(new Coordinates(row++, column), false)
                      .putString("F1 - Cycle Colors");
                stream.putCursorPositionSequence(new Coordinates(row++, column), false)
                      .putString("F2 - Toggle Mode");
                stream.putCursorPositionSequence(new Coordinates(row, column), false)
                      .putString("F9 - Return to Menu");
                stream.putCursorToHome();
                stream.put(ASCII_ETX);
                _server.sendMessage(this, stream);
            } catch (CoordinateException ex) {
                // cannot happen (I hope)
            }
        }
    }

    private Message getNextInput() {
        synchronized (_inputMessages) {
            return _inputMessages.pollFirst();
        }
    }

    @Override
    public void handleInput(final Message message) {
        if (message instanceof StatusPollMessage) {
            // ignore this
        } else {
            // anything else gets queued
            synchronized (_inputMessages) {
                _inputMessages.addLast(message);
            }
            sendUnlockKeyboard();
        }
    }

    private void putCharacter(final UTSByteBuffer stream, final int position, final char character) {
        try {
            var topRow = _digitCoordinates[position].getRow();
            var leftColumn = _digitCoordinates[position].getColumn();
            var map = CHARACTER_MAPS.get(character);
            if (map != null) {
                for (int mx = 0; mx < map.length; mx++) {
                    var coord = new Coordinates(topRow++, leftColumn);
                    stream.putCursorPositionSequence(coord, false);
                    stream.putString(map[mx]);
                }
            }
        } catch (CoordinateException ex) {
            // cannot happen
        }
    }

    @Override
    public void returnFromTransfer() {
        // nothing to do
    }

    public void run() {
        // send text to cause an auto-transmit to determine size of screen
        try {
            UTSByteBuffer stream = new UTSByteBuffer(1024);
            stream.put(ASCII_SOH)
                  .put(ASCII_STX)
                  .putCursorToHome()
                  .putEraseDisplay()
                  .putCursorScanLeft()
                  .putSendCursorPosition(TransmitMode.ALL)
                  .put(ASCII_ETX);
            _server.sendMessage(this, stream);
        } catch (IOException ex) {
            IO.println("Cannot send initial message: " + ex.getMessage());
            return;
        }

        // Wait for response to the above
        Coordinates coord = null;
        while ((coord == null) && !_terminate) {
            try {
                var msg = getNextInput();
                if (msg == null) {
                    Thread.sleep(25);
                } else {
                    if (msg instanceof CursorPositionMessage cpm) {
                        coord = cpm.getCoordinates();
                    } else if (msg instanceof FunctionKeyMessage fkm) {
                        if (fkm.getKey() == 9) {
                            close();
                        }
                    }
                }
            } catch (InterruptedException ex) {
                // do nothing
            }
        }

        _geometry = new DisplayGeometry(coord.getRow(), coord.getColumn());

        try {
            determineCoordinates();
            displayFCCs();
            displayHints();
        } catch (IOException ex) {
            IO.println("Cannot perform initial display: " + ex.getMessage());
            close();
        }

        var lastInstant = Instant.now().minusMillis(1000);
        while (!_terminate) {
            try {
                var thisInstant = Instant.now();
                var elapsed = thisInstant.toEpochMilli() - lastInstant.toEpochMilli();
                if (elapsed > 1000) {
                    synchronized(this) {
                        displayFullTime();
                        lastInstant = thisInstant;
                    }
                }
                synchronized (_inputMessages) {
                    while (!_inputMessages.isEmpty()) {
                        var msg = _inputMessages.pollFirst();
                        if (msg instanceof FunctionKeyMessage fkm) {
                            switch (fkm.getKey()) {
                                case 1 -> cycleColorScheme();
                                case 2 -> cycleMode();
                                case 9 -> close();
                            }
                        }
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException ex) {
                // nothing really to do here
            } catch (IOException ex) {
                IO.println("Cannot send time message: " + ex.getMessage());
                close();
            }
        }

        IO.println("MenuApp terminated");
    }
}

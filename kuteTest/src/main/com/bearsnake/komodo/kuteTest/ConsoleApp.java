/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.FieldAttributes;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.messages.UTSMessage;
import com.bearsnake.komodo.kutelib.messages.MessageWaitMessage;
import com.bearsnake.komodo.kutelib.messages.TextMessage;
import com.bearsnake.komodo.kutelib.uts.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.uts.UTSCursorPositionPrimitive;
import com.bearsnake.komodo.kutelib.uts.UTSFCCSequencePrimitive;
import com.bearsnake.komodo.kutelib.panes.Coordinates;
import com.bearsnake.komodo.kutelib.panes.UTSColor;
import com.bearsnake.komodo.kutelib.uts.UTSPrimitiveType;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class ConsoleApp extends Application implements Runnable {

    private static final UTSColor INPUT_FG_COLOR = UTSColor.WHITE;
    private static final UTSColor INPUT_BG_COLOR = UTSColor.BLACK;
    private static final UTSColor READ_ONLY_FG_COLOR = UTSColor.GREEN;
    private static final UTSColor READ_ONLY_BG_COLOR = UTSColor.BLACK;
    private static final UTSColor READ_REPLY_FG_COLOR = UTSColor.WHITE;
    private static final UTSColor READ_REPLY_BG_COLOR = UTSColor.RED;
    private static final UTSColor SYSTEM_FG_COLOR = UTSColor.CYAN;
    private static final UTSColor SYSTEM_BG_COLOR = UTSColor.BLACK;

    private static final long MILLIS_PER_SECOND = 1000;
    private static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;

    private static class ReadReplyMessage {

        private final String _text;
        private Integer _id;
        private String _response;
        private Integer _row = 0;

        public ReadReplyMessage(final String text) {
            _text = text;
        }

        public int getId() { return _id; }
        public String getResponse() { return _response; }
        public int getRow() { return _row; }
        public String getText() { return _text; }

        public boolean hasId() { return _id != null; }
        public boolean hasResponse() { return _response != null; }
        public boolean hasRow() { return _row != null; }

        public void setId(final int id) { _id = id; }
        public void setResponse(final String response) { _response = response; }
        public void setRow(final int row) { _row = row; }
    }

    private final LinkedList<String> _readOnlyMessages = new LinkedList<>();
    private final LinkedList<ReadReplyMessage> _readReplyMessages = new LinkedList<>();
    private Instant _messageWaitStartTime = null;

    public ConsoleApp(final KuteTestServer server) {
        super(server);
    }

    private synchronized void displaySystemMessages(final String string1,
                                                    final String string2) throws IOException {
        if (!waitingOnInput()) {
            try {
                var coord1 = Coordinates.HOME_POSITION;
                var coord2 = new Coordinates(coord1.getRow() + 1, coord1.getColumn());

                var attr = new FieldAttributes().setTextColor(SYSTEM_FG_COLOR).setBackgroundColor(SYSTEM_BG_COLOR).setProtected(true);
                var prim1 = new UTSFCCSequencePrimitive(coord1.getRow(), coord1.getColumn(), attr);
                var prim2 = new UTSFCCSequencePrimitive(coord2.getRow(), coord2.getColumn(), attr);

                var fmtString = String.format("%%-%ds", _geometry.getColumns());
                var str1 = String.format(fmtString, string1);
                var str2 = String.format(fmtString, string2);

                var stream = new UTSByteBuffer(256);
                stream.put(ASCII_SOH)
                      .put(ASCII_STX);
                prim1.serialize(stream);
                stream.putString(str1);
                prim2.serialize(stream);
                stream.putString(str2)
                      .putLockKeyboard()
                      .put(ASCII_ETX);

                _server.sendMessage(this, stream);
            } catch (CoordinateException ex) {
                // cannot happen
            }
        }
    }

    private Integer getNextAvailableId() {
        for (int id = 0; id < 10; id++) {
            boolean found = false;
            for (var rrm : _readReplyMessages) {
                if (rrm.hasId() && (rrm.getId() == id)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return id;
            }
        }
        return null;
    }

    private synchronized void postReadOnlyMessage(final String message) {
        _readOnlyMessages.add(message);
    }

    private synchronized void postReadReplyMessage(final String text) {
        _readReplyMessages.add(new ReadReplyMessage(text));
    }

    public synchronized boolean processInput(final UTSMessage msg) throws IOException {
        if (msg instanceof MessageWaitMessage) {
            if (!waitingOnInput()) {
                try {
                    _messageWaitStartTime = Instant.now();

                    var attr = new FieldAttributes().setTextColor(INPUT_FG_COLOR)
                                                    .setBackgroundColor(INPUT_BG_COLOR)
                                                    .setProtected(true)
                                                    .setTabStop(true);
                    var prim = new UTSFCCSequencePrimitive(_geometry.getRows(), 1, attr);
                    var stream = new UTSByteBuffer(256);
                    stream.put(ASCII_SOH).put(ASCII_STX);
                    scrollDisplay(stream);
                    prim.serialize(stream);
                    stream.put(ASCII_SOE)
                          .putUnlockKeyboard()
                          .put(ASCII_ETX);
                    _server.sendMessage(this, stream);
                    return true;
                } catch (CoordinateException ex) {
                    // cannot happen
                }
            }
        } else if (msg instanceof TextMessage tm) {
            // TODO
        }
        return false;
    }

    public synchronized boolean processReadOnlyMessage() throws IOException {
        if (!waitingOnInput()) {
            var msg = _readOnlyMessages.poll();
            if (msg != null) {
                try {
                    var fmtString = String.format("  %%-%ds", _geometry.getColumns() - 2);
                    var choppedText = String.format(fmtString, msg);

                    var attr = new FieldAttributes().setTextColor(READ_ONLY_FG_COLOR)
                                                    .setBackgroundColor(READ_ONLY_BG_COLOR)
                                                    .setProtected(true);
                    var prim = new UTSFCCSequencePrimitive(_geometry.getRows(), 1, attr);

                    var stream = new UTSByteBuffer(256);
                    stream.put(ASCII_SOH)
                          .put(ASCII_STX);
                    scrollDisplay(stream);
                    prim.serialize(stream);
                    stream.putString(choppedText)
                          .putCursorToHome()
                          .putLockKeyboard()
                          .put(ASCII_ETX);

                    _server.sendMessage(this, stream);
                    return true;
                } catch (CoordinateException ex) {
                    // cannot happen
                }
            }
        }
        return false;
    }

    private synchronized boolean processReadReplyMessage() throws IOException {
        if (!waitingOnInput()) {
            var iter = _readReplyMessages.iterator();
            while (iter.hasNext()) {
                var msg = iter.next();
                if (!msg.hasId()) {
                    var id = getNextAvailableId();
                    if (id == null) {
                        continue;
                    }

                    try {
                        var attr = new FieldAttributes().setTextColor(READ_REPLY_FG_COLOR).setBackgroundColor(READ_REPLY_BG_COLOR).setProtected(true);
                        var prim = new UTSFCCSequencePrimitive(_geometry.getRows(), 1, attr);

                        var fmtString = String.format("%%d-%%-%ds", _geometry.getColumns() - 2);
                        var str = String.format(fmtString, id, msg.getText());

                        var stream = new UTSByteBuffer(256);
                        stream.put(ASCII_SOH)
                              .put(ASCII_STX);
                        scrollDisplay(stream);
                        prim.serialize(stream);
                        stream.putString(str)
                              .putCursorToHome()
                              .putLockKeyboard()
                              .put(ASCII_ETX);

                        _server.sendMessage(this, stream);
                    } catch (CoordinateException ex) {
                        // cannot happen
                    }

                    msg.setId(id);
                    msg.setRow(_geometry.getRows());

                    return true;
                }

                if (msg.hasResponse()) {
                    iter.remove();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void returnFromTransfer() {
        // nothing to do
    }

    private void scrollDisplay(final UTSByteBuffer stream) {
        int scrollBaseRow = 3;
        var checkAgain = true;
        while (checkAgain) {
            checkAgain = false;
            for (var rrm : _readReplyMessages) {
                if (rrm.hasRow() && (rrm.getRow() == scrollBaseRow)) {
                    scrollBaseRow++;
                    checkAgain = true;
                    break;
                }
            }
        }

        try {
            var prim = new UTSCursorPositionPrimitive(scrollBaseRow, 1);
            prim.serialize(stream);
            stream.put(UTSPrimitiveType.DELETE_LINE.getPattern());
            for (var rrm : _readReplyMessages) {
                if (rrm.hasRow() && (rrm.getRow() > scrollBaseRow)) {
                    rrm.setRow(rrm.getRow() - 1);
                }
            }
        } catch (CoordinateException ex) {
            // cannot happen
        }
    }

    private boolean waitingOnInput() {
        return _messageWaitStartTime != null;
    }

    public void run() {
        if (!determineGeometry()) {
            IO.println("Cannot determine geometry");
            return;
        }

        postReadOnlyMessage("KOMODO Console Simulation for kuteTest");
        postReadOnlyMessage("Used for testing conversational mode applications");
        postReadReplyMessage("Continue? Y/N");
        postReadReplyMessage("Reply with reel number on T0");

        var lastStatus = Instant.now();
        var lastTimeOfDay = Instant.now();

        while (!_terminate) {
            try {
                var now = Instant.now();
                synchronized (this) {
                    try {
                        if (waitingOnInput()) {
                            var elapsed = now.toEpochMilli() - _messageWaitStartTime.toEpochMilli();
                            if (elapsed >= MILLIS_PER_MINUTE) {
                                var attr = new FieldAttributes().setTextColor(INPUT_FG_COLOR).setBackgroundColor(INPUT_BG_COLOR).setProtected(true);
                                var prim = new UTSFCCSequencePrimitive(_geometry.getRows(), 1, attr);

                                var stream = new UTSByteBuffer(100);
                                // field is no input field with protection and read-only color
                                stream.put(ASCII_SOH)
                                      .put(ASCII_STX);
                                prim.serialize(stream);
                                stream.putEraseDisplay()
                                      .putString("INPUT TIMEOUT")
                                      .putCursorToHome()
                                      .putLockKeyboard()
                                      .put(ASCII_ETX);
                                _server.sendMessage(this, stream);
                                _messageWaitStartTime = null;
                            }
                        }
                    } catch (CoordinateException ex) {
                        // cannot happen
                    }
                }

                synchronized (this) {
                    var elapsed = now.toEpochMilli() - lastStatus.toEpochMilli();
                    if (elapsed > 6000) {
                        var localTime = LocalDateTime.now();
                        var string1 = String.format("Status: %02d:%02d:%02d", localTime.getHour(), localTime.getMinute(), localTime.getSecond());
                        displaySystemMessages(string1, "Status Message 2");
                        lastStatus = now;
                    }
                }

                synchronized (this) {
                    var elapsed = now.toEpochMilli() - lastTimeOfDay.toEpochMilli();
                    if (elapsed > 10 * 1000) { // TODO should be 60 * 1000 (or even 6 * 60 * 1000)
                        var localTime = LocalDateTime.now();
                        StringBuilder str = new StringBuilder(String.format("T/D %d/%d/%d %02d:%02d:%02d",
                                                                            localTime.getMonthValue(), localTime.getDayOfMonth(), localTime.getYear(),
                                                                            localTime.getHour(), localTime.getMinute(), localTime.getSecond()));
                        while (str.length() < _geometry.getColumns() - 2) {
                            str.insert(0, " ");
                        }
                        postReadOnlyMessage(str.toString());
                        lastTimeOfDay = now;
                    }
                }

                boolean didSomething = processReadReplyMessage();
                while (didSomething) {
                    didSomething = processReadOnlyMessage();
                }

                didSomething = processReadOnlyMessage();
                while (didSomething) {
                    didSomething = processReadOnlyMessage();
                }

                var msg = getNextInput();
                if (msg != null) {
                    didSomething = processInput(msg);
                }
                if (!didSomething) {
                    Thread.sleep(250);
                }
            } catch (InterruptedException ex) {
                // nothing really to do here
            } catch (IOException e) {
                IO.println("Cannot send status message: " + e.getMessage());
                close();
            }
        }

        IO.println("MenuApp terminated");
    }
}

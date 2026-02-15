/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.utslib.UTSByteBuffer;
import com.bearsnake.komodo.utslib.messages.CursorPositionMessage;
import com.bearsnake.komodo.utslib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.utslib.messages.StatusPollMessage;
import com.bearsnake.komodo.utslib.messages.UTSMessage;
import com.bearsnake.komodo.utslib.primitives.UTSPrimitive;

import java.io.IOException;
import java.util.LinkedList;

import static com.bearsnake.komodo.baselib.Constants.*;

/**
 * Base class for all test applications
 */
public abstract class Application implements Runnable {

    protected DisplayGeometry _geometry;
    private final LinkedList<UTSMessage> _inputMessages = new LinkedList<>();
    protected KuteTestServer _server;
    protected volatile boolean _terminate = false;
    private final Thread _thread = new Thread(this);

    // For invoking as stand-alone
    protected Application(final KuteTestServer server) {
        _server = server;
    }

    public void close() {
        if (!_terminate) {
            _terminate = true;
        }
    }

    public String extractText(final UTSByteBuffer stream) {
        var sb = new StringBuilder();
        stream.setPointer(0);
        while (!stream.atEnd()) {
            try {
                var prim = UTSPrimitive.deserializePrimitive(stream);
                if (prim == null) {
                    var ch = stream.getNext();
                    if ((ch >= 32) && (ch < 127)) {
                        sb.append((char) (byte) ch);
                    }
                }
            } catch (Exception ex) {
                // nothing to do
            }
        }
        return sb.toString().trim();
    }

    /**
     * Retrieves the next queued input for the subclassed application.
     * @return input Message if one exists, else null
     */
    protected UTSMessage getNextInput() {
        synchronized (_inputMessages) {
            return _inputMessages.pollFirst();
        }
    }

    public void handleInput(final UTSMessage message) {
        if (message instanceof StatusPollMessage) {
            // ignore this
        } else {
            // anything else gets queued
            synchronized (_inputMessages) {
                _inputMessages.addLast(message);
            }
        }
    }

    public abstract void returnFromTransfer();

    /**
     * Sends a message which moves the cursor to the last position on the screen,
     * then initiates a send-cursor-position message from the terminal.
     * Waits for that message to arrive, then uses it to determine the screen size.
     * @return true if successful, else false
     */
    protected boolean determineGeometry() {
        // send text to cause an auto-transmit to determine size of screen
        try {
            UTSByteBuffer stream = new UTSByteBuffer(1024);
            stream.put(ASCII_SOH)
                  .put(ASCII_STX)
                  .putCursorToHome()
                  .putEraseDisplay()
                  .putCursorScanLeft()
                  .putSendCursorPosition()
                  .put(ASCII_ETX);
            _server.sendMessage(this, stream);
        } catch (IOException ex) {
            IO.println("Cannot send initial message: " + ex.getMessage());
            close();
            return false;
        }

        // Wait for response to the above
        while ((_geometry == null) && !_terminate) {
            try {
                var msg = getNextInput();
                if (msg == null) {
                    Thread.sleep(25);
                } else {
                    if (msg instanceof CursorPositionMessage cpm) {
                        var coord = cpm.getCoordinates();
                        _geometry = new DisplayGeometry(coord.getRow(), coord.getColumn());
                    } else if (msg instanceof FunctionKeyMessage fkm) {
                        if (fkm.getKey() == 22) {
                            close();
                            return false;
                        }
                    }
                }
            } catch (InterruptedException ex) {
                // do nothing
            }
        }

        if (!_terminate) {
            sendUnlockKeyboard();
        }

        return !_terminate;
    }

    public boolean isTerminated() {
        return !_thread.isAlive() && _terminate;
    }

    protected void sendUnlockKeyboard() {
        try {
            var strm = new UTSByteBuffer(16);
            strm.put(ASCII_SOH)
                .put(ASCII_STX)
                .putUnlockKeyboard()
                .put(ASCII_ETX);
            _server.sendMessage(this, strm);
        } catch (IOException ex) {
            // TODO nothing really to do here
        }
    }

    public void start() {
        _thread.start();
    }
}

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

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class ClockApp extends Application implements Runnable {

    private final LinkedList<Message> _inputMessages = new LinkedList<>();

    public ClockApp(final KuteTestServer server) {
        super(server);
    }

    private void createFCCs() {
        // TODO create fields for banner-type digits with colors indicated by _bgColor, _textColor members
    }

    private void displayHints() {
        // Display usage hints somewhere on the screen (top, bottom?)
    }

    private Message getNextInput() {
        synchronized (_inputMessages) {
            return _inputMessages.pollFirst();
        }
    }

    @Override
    public void handleInput(final Message message) {
        IO.println("ClockApp Received message: " + message);// TODO remove
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
                    // F22 terminates, otherwise grab the first cursor position message
                    if (msg instanceof CursorPositionMessage cpm) {
                        coord = cpm.getCoordinates();
                    } else if (msg instanceof FunctionKeyMessage fkm) {
                        close();
                    }
                }
            } catch (InterruptedException ex) {
                // do nothing
            }
        }

        IO.println("FOO Coords:" + coord);//TODO remove

        createFCCs();
        displayHints();
        var lastInstant = Instant.now().minusMillis(1000);
        while (!_terminate) {
            try {
                var thisInstant = Instant.now();
                var elapsed = thisInstant.toEpochMilli() - lastInstant.toEpochMilli();
                if (elapsed > 1000) {
                    var strm = new UTSByteBuffer(100);
                    strm.put(ASCII_SOH)
                        .put(ASCII_STX)
                        .putCursorToHome()
                        .putEraseDisplay()
                        .putCursorPositionSequence(new Coordinates(3, 3), false)
                        .putString(thisInstant.toString())
                        .put(ASCII_ETX);
                    _server.sendMessage(this, strm);
                    lastInstant = thisInstant;
                }
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // nothing really to do here
            } catch (CoordinateException ex) {
                // this should never happen
            } catch (IOException ex) {
                IO.println("Cannot send message: " + ex.getMessage());
                close();
            }
        }
        IO.println("MenuApp terminated");
    }
}

/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.messages.Message;
import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.panes.Coordinates;

import java.io.IOException;
import java.time.Instant;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class ClockApp extends Application implements Runnable {

    public ClockApp(final KuteTestServer server) {
        super(server);
    }

    private void createFCCs() {
        // TODO create fields for banner-type digits with colors indicated by _bgColor, _textColor members
    }

    private void displayHints() {
        // Display usage hints somewhere on the screen (top, bottom?)
    }

    @Override
    public void handleInput(final Message message) {
        sendUnlockKeyboard();
        IO.println("ClockApp Received message: " + message);// TODO remove
        // F1-F8 set text color (and complementary bg color)
        // F9 toggles 12hr/24hr mode
        // F22 terminates the app
//        data.setPointer(0);
//        var message = Message.create(data.getBuffer());
//        IO.println("Received message: " + message);// TODO remove
//        if (message instanceof FunctionKeyMessage fkm) {
//            switch (fkm.getKey()) {
//                case 1 -> {}
//                case 22 -> _terminate = true;
//                default -> {}//TODO complain about bad FKey
//            }
//        } else {
//            // TODO complain about bad message
//        }
        close();//TODO remove
    }

    @Override
    public void returnFromTransfer() {
        // nothing to do
    }

    private int counter = 5;//TODO remove
    public void run() {
        createFCCs();
        displayHints();
        var lastInstant = Instant.now();
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
                // TODO nothing really to do here
            } catch (CoordinateException ex) {
                IO.println("FOO!");//TODO something here
            } catch (IOException ex) {
                IO.println("FEE!");//TODO something here
                if (counter-- <= 0) {
                    break;
                }
            }
        }
        IO.println("MenuApp terminated");
    }
}

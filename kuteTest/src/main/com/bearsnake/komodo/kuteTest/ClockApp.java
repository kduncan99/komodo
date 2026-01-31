/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.messages.Message;

import java.time.Instant;

public class ClockApp extends Application implements Runnable {

    private final Thread _thread = new Thread(this);

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
    }

    public void run() {
        createFCCs();
        displayHints();
        // display all digits of time - from now on, we only redraw the digit(s) that change
        //   (although F9 will redraw them all)
        var lastInstant = Instant.now();
        while (!_terminate) {
            try {
                var thisInstant = Instant.now();
                var elapsed = thisInstant.toEpochMilli() - lastInstant.toEpochMilli();
                if (elapsed > 1000) {
//                    var strm = new UTSByteBuffer(100);
//                    strm.put(ASCII_SOH)
//                        .put(ASCII_STX)
//                        .putCursorToHome()
//                        .putEraseDisplay()
//                        .putCursorPositionSequence(new Coordinates(3, 3), false);
//                    _channel.send(strm);
                    lastInstant = thisInstant;
                }
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                // TODO
//            } catch (IOException ex) {
//                IO.println("ClockApp failed to send message");
//                _terminate = true;
//            } catch (CoordinateException e) {
//                throw new RuntimeException(e);
            }
        }
    }
}

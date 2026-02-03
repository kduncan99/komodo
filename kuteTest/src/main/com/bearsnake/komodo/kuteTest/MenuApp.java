/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.kutelib.messages.StatusPollMessage;
import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.panes.Coordinates;
import com.bearsnake.komodo.kutelib.panes.ExplicitField;
import com.bearsnake.komodo.kutelib.panes.UTSColor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class MenuApp extends Application implements Runnable {

    private static class ApplicationInfo {

        private final Class<?> _clazz;
        private final String _name;
        private final String _description;

        public ApplicationInfo(final Class<?> clazz,
                               final String name,
                               final String description) {
            _clazz = clazz;
            _name = name;
            _description = description;
        }

        public String getName() { return _name; }
        public String getDescription() { return _description; }
    }

    // key is fkey number (i.e., 1 for F1, 2 for F2, etc.)
    private static final HashMap<Integer, ApplicationInfo> APPLICATION_INFO_TABLE = new HashMap<>();
    static {
        APPLICATION_INFO_TABLE.put(1, new ApplicationInfo(ClockApp.class, "Clock", "Displays the current time"));
        APPLICATION_INFO_TABLE.put(2, new ApplicationInfo(ConsoleApp.class, "Console", "Displays a console simulation"));
    }

    public MenuApp(final KuteTestServer server) {
        super(server);
    }

    private void displayMessage(final String message) {
        try {
            var msgField = new ExplicitField(null).setBackgroundColor(UTSColor.RED).setTextColor(UTSColor.WHITE).setBlinking(true);
            var strm = new UTSByteBuffer(2048);
            strm.put(ASCII_SOH)
                .put(ASCII_STX)
                .putCursorToHome()
                .putCursorScanUp()
                .putEraseDisplay();
            strm.putFCCSequence(msgField, true, true, true)
                .putString(message)
                .putCursorToHome()
                .put(ASCII_ETX);
            strm.setPointer(0);
            _server.sendMessage(this, strm);
        } catch (IOException | CoordinateException ex) {
            IO.println("MenuApp failed to send message");
        }
    }

    private void displayMenu() {
        try {
            var strm = new UTSByteBuffer(2048);
            strm.put(ASCII_SOH)
                .put(ASCII_STX)
                .putCursorToHome()
                .putEraseDisplay();
            var nextRow = 3;
            for (var appInfo : APPLICATION_INFO_TABLE.entrySet()) {
                var fkNumber = appInfo.getKey();
                var app = appInfo.getValue();
                var fkField = new ExplicitField(new Coordinates(nextRow, 10)).setTextColor(UTSColor.YELLOW);
                var nameField = new ExplicitField(new Coordinates(nextRow, 15)).setTextColor(UTSColor.CYAN);
                var descField = new ExplicitField(new Coordinates(nextRow, 25)).setTextColor(UTSColor.GREEN);
                strm.putFCCSequence(fkField, false, true, true)
                    .putString("F" + fkNumber);
                strm.putFCCSequence(nameField, false, true, true)
                    .putString(app.getName());
                strm.putFCCSequence(descField, false, true, true)
                    .putString(app.getDescription());

                nextRow++;
            }

            nextRow++;
            var fkNumber = 22;
            var fkField = new ExplicitField(new Coordinates(nextRow, 10)).setTextColor(UTSColor.YELLOW);
            var nameField = new ExplicitField(new Coordinates(nextRow, 15)).setTextColor(UTSColor.CYAN);
            var descField = new ExplicitField(new Coordinates(nextRow, 25)).setTextColor(UTSColor.GREEN);
            strm.putFCCSequence(fkField, false, true, true)
                .putString("F" + fkNumber);
            strm.putFCCSequence(nameField, false, true, true)
                .putString("EXIT");
            strm.putFCCSequence(descField, false, true, true)
                .putString("Terminate Session");

            strm.putCursorToHome().put(ASCII_ETX);
            strm.setPointer(0);
            _server.sendMessage(this, strm);
        } catch (IOException | CoordinateException ex) {
            IO.println("MenuApp failed to send message");
        }
    }

    @Override
    public void returnFromTransfer() {
        displayMenu();
    }

    private void sendTerminateMessage() {
        try {
            var strm = new UTSByteBuffer(100);
            strm.put(ASCII_SOH)
                .put(ASCII_STX)
                .putCursorToHome()
                .putEraseDisplay()
                .putString("Goodbye")
                .put(ASCII_CR)
                .put(ASCII_ETX);
            strm.setPointer(0);
            _server.sendMessage(this, strm);
        } catch (IOException ex) {
            // nothing really to do here
        }
    }

    @Override
    public void run() {
        displayMenu();
        while (!_terminate) {
            try {
                var message = getNextInput();
                if (message != null) {
                    if (message instanceof FunctionKeyMessage fkm) {
                        var appInfo = APPLICATION_INFO_TABLE.get(fkm.getKey());
                        if (appInfo != null) {
                            var newApp = (Application) appInfo._clazz.getConstructor(new Class[]{KuteTestServer.class})
                                                                     .newInstance(_server);
                            _server.transferApplication(this, newApp);
                        } else if (fkm.getKey() == 22) {
                            sendTerminateMessage();
                            close();
                        } else {
                            displayMessage("Invalid Function Key");
                        }
                    } else if (message instanceof StatusPollMessage) {
                        // ignore this
                    } else {
                        displayMessage("Invalid Input:" + message);
                    }
                } else {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ex) {
                // nothing really to do here
            } catch (NoSuchMethodException
                     | InvocationTargetException
                     | InstantiationException
                     | IllegalAccessException ex) {
                displayMessage("Caught Exception:" + ex.getMessage());
            }
        }
        IO.println("MenuApp terminated");
    }
}

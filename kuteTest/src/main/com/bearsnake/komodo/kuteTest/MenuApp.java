/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.kutelib.messages.Message;
import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.panes.Coordinates;
import com.bearsnake.komodo.kutelib.panes.ExplicitField;
import com.bearsnake.komodo.kutelib.panes.UTSColor;

import java.io.IOException;
import java.util.LinkedList;

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

    private static final LinkedList<ApplicationInfo> APPLICATIONS = new LinkedList<>();
    static {
        APPLICATIONS.add(new ApplicationInfo(ClockApp.class, "Clock", "Displays the current time"));
    }

    private Thread _thread = null;

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
            var fkNumber = 1;
            for (var app : APPLICATIONS) {
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
                fkNumber++;
            }

            nextRow++;
            fkNumber = 22;
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
    public void handleInput(UTSByteBuffer data) {
        sendUnlockKeyboard();
        data.setPointer(0);
        var message = Message.create(data.getBuffer());
        IO.println("Received message: " + message);// TODO remove
        if (message instanceof FunctionKeyMessage fkm) {
            switch (fkm.getKey()) {
                case 1 -> {}
                case 22 -> close();
                default -> displayMessage("Invalid Function Key");
            }
        } else {
            displayMessage("Invalid Input");
        }
    }

    public void run() {
        displayMenu();
        while (!_terminate) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // TODO nothing really to do here
            }
        }
        close();
        IO.println("MenuApp terminated");
    }
}

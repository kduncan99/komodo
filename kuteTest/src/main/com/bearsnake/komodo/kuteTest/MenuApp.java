/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.*;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.kutelib.messages.Message;
import com.bearsnake.komodo.kutelib.messages.TextMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class MenuApp extends Application implements SocketChannelListener {

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

    private final Thread _thread = new Thread(this);

    public MenuApp(final SocketChannel channel) {
        super(channel);
        _thread.start();
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
            var msg = new TextMessage(strm.getBuffer());
            _channel.send(msg);
        } catch (IOException | CoordinateException ex) {
            // TODO do something here
            IO.println("MenuApp failed to send message");
        }
    }

    @Override
    public void trafficReceived(UTSByteBuffer data) {
        sendUnlockKeyboard();
        data.setPointer(0);
        var message = Message.create(data.getBuffer());
        IO.println("Received message: " + message);// TODO remove
        if (message instanceof FunctionKeyMessage fkm) {
            switch (fkm.getKey()) {
                case 1 -> {}
                case 22 -> _terminate = true;
                default -> {}//TODO complain about bad FKey
            }
        } else {
            // TODO complain about bad message
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

        // TODO should send an au revoir message
    }
}

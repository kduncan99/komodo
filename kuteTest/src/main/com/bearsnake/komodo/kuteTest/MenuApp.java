/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.FieldAttributes;
import com.bearsnake.komodo.kutelib.SocketChannelHandler;
import com.bearsnake.komodo.kutelib.UTSColor;
import com.bearsnake.komodo.kutelib.UTSOutputStream;
import com.bearsnake.komodo.kutelib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.kutelib.messages.TextMessage;

import java.io.IOException;
import java.time.Instant;

import static com.bearsnake.komodo.kutelib.Constants.ASCII_CR;

public class MenuApp extends Application {

    private static final FieldAttributes FKEY_ATTRIBUTES =
        new FieldAttributes().setTextColor(UTSColor.YELLOW)
                             .setBackgroundColor(UTSColor.BLACK)
                             .setTabStop(true);

    private static final FieldAttributes NAME_ATTRIBUTES =
        new FieldAttributes().setTextColor(UTSColor.CYAN)
                             .setBackgroundColor(UTSColor.BLACK)
                             .setTabStop(false);

    private static final FieldAttributes NORMAL_ATTRIBUTES =
        new FieldAttributes().setTextColor(UTSColor.GREEN)
                             .setBackgroundColor(UTSColor.BLACK)
                             .setTabStop(false);

    private final Thread _thread = new Thread(this);

    public MenuApp(final SocketChannelHandler session) {
        super(session);
        _thread.start();
    }

    private void displayMenu() {
        var strm = new UTSOutputStream(2048);
        strm.writeCursorToHome()
            .writeEraseDisplay()
            .writeFCC(3, 3, FKEY_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeString("F1")
            .writeFCC(3, 6, NORMAL_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeFCC(3, 8, NAME_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeString("Clock")
            .writeFCC(3, 20, NORMAL_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeFCC(4, 3, FKEY_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeString("F2")
            .writeFCC(4, 6, NORMAL_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeFCC(4, 8, NAME_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeString("Snake")
            .writeFCC(4, 20, NORMAL_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeFCC(6, 3, FKEY_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeString("F22")
            .writeFCC(6, 6, NORMAL_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeFCC(6, 8, NAME_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .writeString("Exit")
            .writeFCC(6, 20, NORMAL_ATTRIBUTES, UTSOutputStream.FCCFormat.FCC_COLOR_FG_BG_ONE_BYTE)
            .write(ASCII_CR)
            .writeUnlockKeyboard();
        var msg = new TextMessage(strm.getBuffer(), 0, strm.size());
        try {
            _channel.writeMessage(msg);
        } catch (IOException ex) {
            // TODO do something here
            IO.println("MenuApp failed to send message");
        }
    }

    public void run() {
        displayMenu();
        var lastInstant = Instant.now();
        while (!_terminate) {
            var msg = _channel.readNextMessage();
            if (msg instanceof FunctionKeyMessage fkm) {
                switch (fkm.getKey()) {
                    case 1 -> {}
                    case 2 -> {}
                    case 22 -> _terminate = true;
                }
            } else if (msg != null) {
                displayMenu();
            }
        }
    }
}

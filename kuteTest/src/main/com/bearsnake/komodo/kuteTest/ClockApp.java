/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.SocketChannelHandler;
import com.bearsnake.komodo.kutelib.UTSOutputStream;
import com.bearsnake.komodo.kutelib.messages.TextMessage;

import java.io.IOException;
import java.time.Instant;

public class ClockApp extends Application {

    private final Thread _thread = new Thread(this);
    public ClockApp(final SocketChannelHandler session) {
        super(session);
        _thread.start();
    }

    public void run() {
        var lastInstant = Instant.now();
        while (!_terminate) {
            try {
                var thisInstant = Instant.now();
                var elapsed = thisInstant.toEpochMilli() - lastInstant.toEpochMilli();
                if (elapsed > 1000) {
                    var strm = new UTSOutputStream();
                    strm.writeCursorToHome()
                        .writeEraseDisplay()
                        .writeCursorPosition(3, 3);
                    _channel.writeMessage(new TextMessage(strm.getBuffer()));
                    lastInstant = thisInstant;
                }
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                // TODO
            } catch (IOException ex) {
                IO.println("ClockApp failed to send message");
                _terminate = true;
            }
        }
    }
}

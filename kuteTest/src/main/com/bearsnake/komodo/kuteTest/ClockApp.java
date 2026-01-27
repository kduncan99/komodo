/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.Coordinates;
import com.bearsnake.komodo.kutelib.SocketChannelHandler;
import com.bearsnake.komodo.kutelib.SocketChannelListener;
import com.bearsnake.komodo.kutelib.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.messages.FunctionKeyMessage;
import com.bearsnake.komodo.kutelib.messages.Message;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.Instant;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class ClockApp extends Application implements SocketChannelListener, Runnable {

    private final Thread _thread = new Thread(this);

    public ClockApp(final SocketChannel channel) {
        super(channel);
        _thread.start();
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
                        .putCursorPositionSequence(new Coordinates(3, 3), false);
                    _channel.send(strm);
                    lastInstant = thisInstant;
                }
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                // TODO
            } catch (IOException ex) {
                IO.println("ClockApp failed to send message");
                _terminate = true;
            } catch (CoordinateException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.SocketChannelHandler;
import com.bearsnake.komodo.kutelib.SocketChannelListener;
import com.bearsnake.komodo.kutelib.UTSByteBuffer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 * Base class for all test applications
 */
public abstract class Application implements Runnable, SocketChannelListener {

    protected final SocketChannelHandler _channel;
    protected volatile boolean _terminate = false;

    protected Application(final SocketChannel channel) {
        _channel = new SocketChannelHandler(channel, this);
    }

    public void close() {
        _terminate = true;
        _channel.close();
    }

    protected void sendUnlockKeyboard() {
        try {
            var strm = new UTSByteBuffer(100);
            strm.put(ASCII_SOH)
                .put(ASCII_STX)
                .putUnlockKeyboard()
                .put(ASCII_ETX);
            strm.setPointer(0);
            _channel.send(strm);
        } catch (IOException ex) {
            // TODO nothing really to do here
        }
    }
}

/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.SocketChannelHandler;

/**
 * Base class for all test applications
 */
public abstract class Application implements Runnable {

    protected final SocketChannelHandler _channel;
    protected volatile boolean _terminate = false;

    protected Application(final SocketChannelHandler channel) {
        _channel = channel;
    }

    public void close() {
        _terminate = true;
        _channel.close();
    }
}

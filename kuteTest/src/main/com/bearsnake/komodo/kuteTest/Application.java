/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.messages.Message;
import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;

import java.io.IOException;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 * Base class for all test applications
 */
public abstract class Application implements Runnable {

    protected KuteTestServer _server;
    protected volatile boolean _terminate = false;
    private Thread _thread = new Thread(this);

    // For invoking as stand-alone
    protected Application(final KuteTestServer server) {
        _server = server;
    }

    public void close() {
        if (!_terminate) {
            _terminate = true;
            IO.println("Setting _terminate to true");//TODO remove
        }
    }

    public abstract void handleInput(final Message message);
    public abstract void returnFromTransfer();

    public boolean isTerminated() {
        return !_thread.isAlive() && _terminate;
    }

    protected void sendUnlockKeyboard() {
        try {
            var strm = new UTSByteBuffer(100);
            strm.put(ASCII_SOH)
                .put(ASCII_STX)
                .putUnlockKeyboard()
                .put(ASCII_ETX);
            strm.setPointer(0);
            _server.sendMessage(this, strm);
        } catch (IOException ex) {
            // TODO nothing really to do here
        }
    }

    public void start() {
        _thread.start();
    }
}

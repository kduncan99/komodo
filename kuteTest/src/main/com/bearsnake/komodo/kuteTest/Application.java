/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

/**
 * Base class for all test applications
 */
public abstract class Application implements Runnable {

    protected final Session _session;
    protected volatile boolean _terminate = false;

    protected Application(final Session session) {
        _session = session;
    }

    public void close() {
        _terminate = true;
        _session.terminate();
    }
}

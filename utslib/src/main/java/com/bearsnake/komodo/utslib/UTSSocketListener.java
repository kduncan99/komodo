/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib;

import com.bearsnake.komodo.utslib.messages.UTSMessage;

public interface UTSSocketListener {

    /**
     * Indicates that the socket has been closed.
     * @param source SocketChannelHandler that sent the notification
     */
    void socketClosed(final UTSSocketHandler source);

    /**
     * SocketChannelHandler notifies the registered listener that data has been received.
     * This includes status polls, which are automatically handled by the Handler.
     * We do the notification anyway so that the listener is aware of polls, if it cares.
     * @param source SocketChannelHandler that sent the data
     * @param message the message to be processed by the listener
     */
    void socketTrafficReceived(final UTSSocketHandler source,
                               final UTSMessage message);

    /**
     * Indicates that socket traffic was captured by the trace facility.
     * @param source SocketChannelHandler that sent the notification
     */
    default void socketTrafficTraced(final UTSSocketHandler source) {}
}

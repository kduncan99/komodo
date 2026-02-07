/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.netlib;

public interface SocketListener {

    /**
     * Indicates that the socket has been closed.
     * @param source SocketChannelHandler that sent the notification
     */
    void socketClosed(final SocketHandler source);

    /**
     * SocketChannelHandler notifies the registered listener that data has been received.
     * This includes status polls, which are automatically handled by the Handler.
     * We do the notification anyway so that the listener is aware of polls, if it cares.
     * @param source SocketChannelHandler that sent the data
     * @param message the message to be processed by the listener
     * @param offset the offset into the message buffer where the data starts
     * @param length the length of the data in the message buffer
     */
    void socketTrafficReceived(final SocketHandler source,
                               final byte[] message,
                               final int offset,
                               final int length);

    /**
     * Indicates that socket traffic was captured by the trace facility.
     * Listener is not obligated to implement this method, as it serves merely as notification.
     * @param source SocketChannelHandler that sent the notification
     */
    default void socketTrafficTraced(final SocketHandler source) {}
}

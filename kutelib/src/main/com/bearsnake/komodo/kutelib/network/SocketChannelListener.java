package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.kutelib.messages.Message;

public interface SocketChannelListener {

    /**
     * SocketChannelHandler notifies the registered listener that data has been received.
     * @param source SocketChannelHandler that sent the data
     * @param message the message to be processed by the listener
     */
    void socketTrafficReceived(final SocketChannelHandler source,
                               final Message message);

    /**
     * Indicates that the socket has been closed.
     * @param source SocketChannelHandler that sent the notification
     */
    void socketClosed(final SocketChannelHandler source);
}

package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.kutelib.messages.Message;

public interface SocketChannelListener {

    /**
     * SocketChannelHandler notifies the registered listener that data has been received.
     * This includes status polls, which are automatically handled by the Handler.
     * We do the notification anyway so that the listener is aware of polls, if it cares.
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

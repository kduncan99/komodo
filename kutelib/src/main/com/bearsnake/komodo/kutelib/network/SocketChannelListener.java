package com.bearsnake.komodo.kutelib.network;

public interface SocketChannelListener {

    /**
     * SocketChannelHandler notifies the registered listener that data has been received.
     * @param source SocketChannelHandler that sent the data
     * @param data data to be processed by the listener
     */
    void trafficReceived(final SocketChannelHandler source,
                         final UTSByteBuffer data);
}

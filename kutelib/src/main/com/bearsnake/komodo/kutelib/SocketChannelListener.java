package com.bearsnake.komodo.kutelib;

public interface SocketChannelListener {

    void trafficReceived(final byte[] data);
}

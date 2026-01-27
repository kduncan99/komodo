package com.bearsnake.komodo.kutelib;

public interface SocketChannelListener {

    void trafficReceived(final UTSByteBuffer data);
}

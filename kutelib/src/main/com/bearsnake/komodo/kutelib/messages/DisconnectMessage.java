package com.bearsnake.komodo.kutelib.messages;

import com.bearsnake.komodo.kutelib.network.SocketChannelHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 * A DisconnectMessage is sent from either end of a UTS session to indicate that the session should be terminated.
 */
public class DisconnectMessage implements Message {

    private static final byte[] PATTERN = {ASCII_SOH, ASCII_DLE, ASCII_EOT, ASCII_STX, ASCII_ETX};

    static DisconnectMessage create(final byte[] data) {
        if (Arrays.equals(data, PATTERN)) {
            return new DisconnectMessage();
        } else {
            return null;
        }
    }

    @Override
    public void write(final SocketChannel channel)
        throws IOException {
        SocketChannelHandler.dumpBuffer("Sending: ", PATTERN);//TODO remove
        channel.write(ByteBuffer.wrap(PATTERN));
    }

    @Override
    public String toString() {
        return "DisconnectMessage";
    }
}

/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.messages.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

import static com.bearsnake.komodo.kutelib.Constants.*;
import static com.bearsnake.komodo.kutelib.Constants.ASCII_BEL;
import static com.bearsnake.komodo.kutelib.Constants.ASCII_ETX;
import static com.bearsnake.komodo.kutelib.Constants.ASCII_SOH;
import static com.bearsnake.komodo.kutelib.Constants.ASCII_STX;

/**
 * Wraps a SocketChannel for simplifying the processing of handling UTS-style communication.
 */
public class SocketChannelHandler
    extends Thread {

    // Input from terminal cannot exceed this size
    private static final int INPUT_BUFFER_SIZE = 8192;

    // Simple streams
    private static final byte[] SEND_DROP_SESSION = {ASCII_SOH, ASCII_DLE, ASCII_EOT, ASCII_STX, ASCII_ETX};
    private static final byte[] SEND_MSG_WAIT = {ASCII_SOH, ASCII_BEL, ASCII_STX, ASCII_ETX};

    private final SocketChannel _channel;
    private boolean _terminate = false;
    private final Thread _thread = new Thread(this);

    private final LinkedList<Message> _messageQueue = new LinkedList<>();

    public SocketChannelHandler(SocketChannel channel) {
        _channel = channel;
        _thread.start();
    }

    public void close() {
        _terminate = true;
        try {
            _channel.close();
        } catch (IOException ex) {
            System.out.println("Error forcing socket close:" + ex.getMessage());
        }
    }

    public Message readNextMessage() {
        synchronized (_messageQueue) {
            return _messageQueue.pollFirst();
        }
    }

    public void writeMessage(final Message message) throws IOException {
        message.write(_channel);
    }

    // If the buffer contains an SOH but not at the first position, discard all bytes up to the first SOH
    // and adjust the buffer's position accordingly.
    private void discardBytesBeforeSOH(final ByteBuffer buffer) {
        if ((buffer.position() == 0) || (buffer.get(0) == ASCII_SOH)) {
            return;
        }

        int px = 1;
        while ((px < buffer.position()) && (buffer.get(px) != ASCII_SOH)) {
            ++px;
        }
        if (px == buffer.position()) {
            buffer.clear();
            return;
        }

        var oldPos = buffer.position();
        buffer.position(px);
        buffer.compact();
    }

    public void run() {
        IO.println("Session started:" + _channel.socket().getRemoteSocketAddress());

        try {
            _channel.configureBlocking(false);
            _channel.socket().setKeepAlive(true);
            _channel.socket().setReuseAddress(true);
            _channel.socket().setTcpNoDelay(true);
        } catch (IOException e) {
            IO.println("Session failed to configure channel");
            return;
        }

        var inputBuffer = ByteBuffer.allocateDirect(INPUT_BUFFER_SIZE);
        while (!_terminate) {
            try {
                if (inputBuffer.remaining() == 0) {
                    IO.println("Session input buffer full - discarding message");
                    inputBuffer.clear();
                    continue;
                }

                discardBytesBeforeSOH(inputBuffer);
                var oldPos = inputBuffer.position();
                var bytesRead = _channel.read(inputBuffer);
                if (bytesRead == 0) {
                    Thread.sleep(25);
                    continue;
                }

                if (bytesRead > 0) {
                    for (int ex = oldPos; ex < inputBuffer.position(); ++ex) {
                        if (inputBuffer.get(ex) == ASCII_ETX) {
                            var newBuffer = new byte[ex + 1];
                            inputBuffer.get(newBuffer);
                            var message = Message.create(newBuffer);
                            if (message == null) {
                                IO.println("Session failed to parse message - discarding");
                            } else {
                                synchronized (_messageQueue) {
                                    IO.println("Session received message: " + message);
                                    _messageQueue.addLast(message);
                                }
                            }
                            inputBuffer.compact();
                        }
                    }
                }
            } catch (InterruptedException ex) {
                IO.println("Session sleep interrupted");
            } catch (IOException ex) {
                IO.println("Session failed to read from channel");
                _terminate = true;
            }
        }

        try {
            _channel.close();
        } catch (IOException ex) {
            IO.println("Session failed to close channel");
        }

        IO.println("Session ended:" + _channel.socket().getRemoteSocketAddress());
    }
}

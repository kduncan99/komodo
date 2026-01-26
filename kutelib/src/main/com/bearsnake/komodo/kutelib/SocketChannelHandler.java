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
    private SocketChannelListener _listener;
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

    public void setListener(final SocketChannelListener listener) {
        _listener = listener;
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
            IO.println("Channel Handler discarding " + px + " pre-SOH bytes");//TODO remove later
            buffer.clear();
            return;
        }

        IO.println("Channel Handler discarding " + px + " pre-SOH bytes");//TODO remove later
        buffer.position(px);
        buffer.compact();
    }

    public void run() {
        IO.println("Channel Handler started:" + _channel.socket().getRemoteSocketAddress());

        try {
            _channel.configureBlocking(false);
            _channel.socket().setKeepAlive(true);
            _channel.socket().setReuseAddress(true);
            _channel.socket().setTcpNoDelay(true);
        } catch (IOException e) {
            IO.println("Channel Handler failed to configure channel");
            return;
        }

        var inputBuffer = ByteBuffer.allocateDirect(INPUT_BUFFER_SIZE);
        while (!_terminate) {
            try {
                if (inputBuffer.remaining() == 0) {
                    IO.println("Channel Handler input buffer full - discarding message");
                    inputBuffer.clear();
                    continue;
                }

                var oldPos = inputBuffer.position();
                var bytesRead = _channel.read(inputBuffer);
                if (bytesRead == 0) {
                    Thread.sleep(25);
                    continue;
                }

                if (bytesRead > 0) {
                    discardBytesBeforeSOH(inputBuffer);
                    dumpBuffer("Received:", inputBuffer, 0, inputBuffer.position());//TODO remove
                    for (int ex = oldPos; ex < inputBuffer.position(); ++ex) {
                        if (inputBuffer.get(ex) == ASCII_ETX) {
                            var newBuffer = new byte[ex + 1];
                            inputBuffer.position(0);
                            inputBuffer.get(newBuffer);
                            var message = Message.create(newBuffer);
                            if (message == null) {
                                IO.println("Channel Handler failed to parse message - discarding");
                            } else {
                                if (_listener != null) {
                                    _listener.trafficReceived(newBuffer);
                                } else {
                                    synchronized (_messageQueue) {
                                        IO.println("Channel Handler received message: " + message);
                                        _messageQueue.addLast(message);
                                    }
                                }
                            }
                            inputBuffer.compact();
                        }
                    }
                }
            } catch (InterruptedException ex) {
                IO.println("Channel Handler sleep interrupted");
            } catch (IOException ex) {
                IO.println("Channel Handler failed to read from channel");
                _terminate = true;
            }
        }

        try {
            _channel.close();
        } catch (IOException ex) {
            IO.println("Channel Handler failed to close channel");
        }

        IO.println("Channel Handler ended:" + _channel.socket().getRemoteSocketAddress());
    }

    // TODO remove this
    public static void dumpBuffer(final String caption,
                                  final ByteBuffer buffer,
                                  final int offset,
                                  final int count) {
        System.out.printf(caption);
        for (int i = 0; i < count; i++) {
            byte b = buffer.get(i + offset);
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    // TODO remove this
    public static void dumpBuffer(final String caption,
                                  final byte[] buffer,
                                  final int offset,
                                  final int count) {
        System.out.printf(caption);
        for (int i = 0; i < count; i++) {
            byte b = buffer[i + offset];
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    // TODO remove this
    public static void dumpBuffer(final String caption,
                                  final byte[] buffer) {
        System.out.printf(caption);
        for (byte b : buffer) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }
}

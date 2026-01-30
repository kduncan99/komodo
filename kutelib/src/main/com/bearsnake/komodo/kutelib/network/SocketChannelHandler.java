/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.kutelib.messages.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Wraps a SocketChannel for simplifying the processing of handling UTS-style communication.
 * TODO seriously consider using Message objects instead of byte arrays for client read/write
 */
public class SocketChannelHandler
    extends Thread {

    // Input from terminal cannot exceed this size
    private static final int INPUT_BUFFER_SIZE = 8192;

    private final SocketChannel _channel;
    private SocketChannelListener _listener;
    private boolean _terminate = false;
    private final Thread _thread = new Thread(this);

    public SocketChannelHandler(final SocketChannel channel,
                                final SocketChannelListener listener) {
        _channel = channel;
        _listener = listener;
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

    public void send(final UTSByteBuffer buffer) throws IOException {
        buffer.setPointer(0);
        _channel.write(ByteBuffer.wrap(buffer.getBuffer()));
    }

    public void send(final Message message) throws IOException {
        message.write(_channel);
    }

    public void setListener(final SocketChannelListener listener) {
        _listener = listener;
    }

    public void run() {
        IO.println("Channel Handler started:" + _channel.socket().getRemoteSocketAddress());

        try {
            _channel.socket().setKeepAlive(true);
            _channel.socket().setReuseAddress(true);
            _channel.socket().setTcpNoDelay(true);
        } catch (IOException e) {
            IO.println("Channel Handler failed to configure channel");
            return;
        }

        var inputBuffer = new UTSByteBuffer(INPUT_BUFFER_SIZE);
        while (!_terminate) {
            try {
                inputBuffer.readFromChannel(_channel);
                dumpBuffer("Received:", inputBuffer.getBuffer());//TODO remove
                _listener.trafficReceived(this, inputBuffer);
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

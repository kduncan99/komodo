/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.kutelib.messages.DisconnectMessage;
import com.bearsnake.komodo.kutelib.messages.Message;
import com.bearsnake.komodo.kutelib.messages.StatusMessage;
import com.bearsnake.komodo.kutelib.messages.StatusPollMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Wraps a SocketChannel for simplifying the processing of handling UTS-style communication.
 */
public class SocketChannelHandler extends Thread {

    // Input from terminal cannot exceed this size
    private static final int INPUT_BUFFER_SIZE = 8192;

    // Milliseconds between heartbeat polls
    private static final int POLL_TIMER_PERIODICITY_MSEC = 1000;

    private final SocketChannel _channel;
    public boolean _isClosed = false;
    private SocketChannelListener _listener;
    private boolean _terminate = false;
    private final Thread _thread = new Thread(this);
    private Timer _timer;

    public SocketChannelHandler(final SocketChannel channel,
                                final SocketChannelListener listener) {
        _channel = channel;
        _listener = listener;
        _thread.start();
        _timer = new Timer();
        _timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    write(new StatusPollMessage());
                } catch (IOException e) {
                    close();
                    IO.println("Cannot send status poll: " + e.getMessage());
                }
            }
        }, POLL_TIMER_PERIODICITY_MSEC, POLL_TIMER_PERIODICITY_MSEC);
    }

    public void close() {
        if (!_isClosed) {
            _isClosed = true;
            _terminate = true;
            try {
                _channel.close();
                _timer.cancel();
                _timer = null;
                _listener.socketClosed(this);
            } catch (IOException ex) {
                System.out.println("Error forcing socket close:" + ex.getMessage());
            }
        }
    }

    public void setListener(final SocketChannelListener listener) {
        _listener = listener;
    }

    public void write(final Message message) throws IOException {
        write(message.getBuffer());
    }

    public synchronized void write(final ByteBuffer buffer) throws IOException {
        buffer.position(0);
        while (buffer.hasRemaining()) {
            try {
                if (_channel.write(buffer) == 0) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    public void run() {
        IO.println("Channel Handler started:" + _channel.socket().getRemoteSocketAddress());

        try {
            _channel.configureBlocking(true);
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
                var message = Message.create(inputBuffer);
                if (message instanceof StatusPollMessage) {
                    // Send a StatusMessage in response
                    _listener.socketTrafficReceived(this, message);
                    write(new StatusMessage().getBuffer());
                } else if (message instanceof StatusMessage) {
                    // TODO at some point we might check these against a timer to detect a dropped session
                } else if (message instanceof DisconnectMessage) {
                    // Tear down the session
                    close();
                } else {
                    // Everything else goes to the listener
                    _listener.socketTrafficReceived(this, message);
                }
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

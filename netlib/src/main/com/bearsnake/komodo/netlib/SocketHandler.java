/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.netlib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Wraps a SocketChannel for simplifying the processing of handling UTS-like network communication.
 * It provides the following benefits:
 *  Provides an asynchronous thread for reading network traffic, calling back to some listener as each message is received.
 *  Automatically sends a status poll to the remote connection at regular intervals.
 *  Invokes Message base class for packetizing messages.
 */
public class SocketHandler extends Thread {

    // Input from the remote end *can* exceed this size, but it will result in fragmented messages and traces.
    private static final int INPUT_BUFFER_SIZE = 8192;

    private final SocketChannel _channel;
    public boolean _isClosed = false;
    private SocketListener _listener;
    private boolean _terminate = false;

    private SocketTrace _currentTrace = null;
    private boolean _isTracePaused = false;

    /**
     * Constructor
     * @param channel underlying SocketChannel
     * @param listener listener for socket events
     */
    public SocketHandler(final SocketChannel channel,
                         final SocketListener listener) {
        _channel = channel;
        _listener = listener;
        var thread = new Thread(this);
        thread.start();
    }

    /**
     * Only for subclass, which MUST set _listener in its own constructor.
     * @param channel underlying SocketChannel
     */
    protected SocketHandler(final SocketChannel channel) {
        this(channel, null);
    }

    public void close() {
        if (!_isClosed) {
            _isClosed = true;
            _terminate = true;
            try {
                _listener.socketClosed(this);
                _channel.close();
            } catch (IOException ex) {
                System.out.println("Error forcing socket close:" + ex.getMessage());
            }
        }
    }

    protected void setListener(final SocketListener listener) {
        _listener = listener;
    }

    public boolean traceActive() {
        return _currentTrace != null;
    }

    public synchronized void tracePause() {
        _isTracePaused = true;
    }

    public synchronized boolean traceStart() {
        if (_currentTrace != null) {
            return false;
        }
        _currentTrace = new SocketTrace();
        _isTracePaused = false;
        return true;
    }

    public synchronized SocketTrace traceStop() {
        var trace = _currentTrace;
        _currentTrace = null;
        _isTracePaused = false;
        return trace;
    }

    /**
     * Writes data from a buffer to the underlying channel. Handles partial write situations.
     * We pro-actively set the buffer position back to zero before acting, so the calling routine does not have to.
     * @param buffer buffer containing data to be written
     * @throws IOException if writing to the channel fails
     */
    public void write(final ByteBuffer buffer) throws IOException {
        buffer.position(0);
        boolean traced = false;
        synchronized (this) {
            if (_currentTrace != null && !_isTracePaused) {
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                _currentTrace.addEntry(SocketTrace.Source.LOCAL, data);
                buffer.position(0);
                traced = true;
            }
        }

        if (traced) {
            _listener.socketTrafficTraced(this);
        }

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

    @Override
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

        var inputBuffer = new byte[INPUT_BUFFER_SIZE];
        var byteBuffer = ByteBuffer.wrap(inputBuffer);
        while (!_terminate) {
            try {
                byteBuffer.clear();
                var bytesRead = _channel.read(byteBuffer);
                if (bytesRead == -1) {
                    _terminate = true;
                } else if (bytesRead > 0) {
                    var traced = false;
                    synchronized (this) {
                        if (_currentTrace != null && !_isTracePaused) {
                            _currentTrace.addEntry(SocketTrace.Source.REMOTE, inputBuffer, 0, bytesRead);
                            traced = true;
                        }
                    }

                    if (traced) {
                        _listener.socketTrafficTraced(this);
                    }

                    _listener.socketTrafficReceived(this, inputBuffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                IO.println("Channel Handler failed to read from channel");
                _terminate = true;
            }
        }

        try {
            if (_isClosed) {
                _listener.socketClosed(this);
                _channel.close();
            }
        } catch (IOException ex) {
            IO.println("Channel Handler failed to close channel");
        }

        IO.println("Channel Handler ended:" + _channel.socket().getRemoteSocketAddress());
    }
}

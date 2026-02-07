/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.kutelib.messages.DisconnectMessage;
import com.bearsnake.komodo.kutelib.messages.StatusMessage;
import com.bearsnake.komodo.kutelib.messages.StatusPollMessage;
import com.bearsnake.komodo.kutelib.messages.UTSMessage;
import com.bearsnake.komodo.netlib.SocketHandler;
import com.bearsnake.komodo.netlib.SocketListener;
import com.bearsnake.komodo.netlib.SocketTrace;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.bearsnake.komodo.kutelib.Constants.ASCII_ETX;
import static com.bearsnake.komodo.kutelib.Constants.ASCII_SOH;

/**
 * Wraps a SocketChannel for simplifying the processing of handling UTS-like network communication.
 * Input and output is via UTSMessage objects.
 */
public class UTSSocketHandler extends SocketHandler implements SocketListener {

    // UTS message from the remote peer cannot exceed this size
    private static final int INPUT_BUFFER_SIZE = 8192;

    // Milliseconds between heartbeat polls
    private static final int POLL_TIMER_PERIODICITY_MSEC = 1000;

    private final byte[] _inputBuffer = new byte[INPUT_BUFFER_SIZE];
    private final ByteBuffer _byteBuffer = ByteBuffer.wrap(_inputBuffer);
    private Timer _timer;
    private final UTSSocketListener _utsListener;

    public UTSSocketHandler(final SocketChannel channel,
                            final UTSSocketListener utsListener) {
        super(channel);
        setListener(this);
        _utsListener = utsListener;

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

    @Override
    public void close() {
        super.close();
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }
    }

    /**
     * SocketListener API - do not invoke
     */
    @Override
    public void socketClosed(final SocketHandler source) {
        _utsListener.socketClosed(this);
    }

    /**
     * SocketListener API - do not invoke
     */
    @Override
    public void socketTrafficReceived(final SocketHandler source,
                                      final byte[] data,
                                      final int offset,
                                      final int length) {
        var dc = 0;
        var dx = offset;
        while (dc < length) {
            var ch = data[dx++];
            dc++;

            // Ignore NUL bytes
            if (ch == 0) {
                continue;
            }

            // If _inputBuffer position is zero, we have not yet received an ASCII_SOH.
            // Ignore input bytes until we do.
            if ((_byteBuffer.position() > 0) || (dx == ASCII_SOH)) {
                // If the byte buffer is full, discard the entire message
                if (_byteBuffer.remaining() == 0) {
                    _byteBuffer.clear();
                    continue;
                }

                // Add the byte to the buffer
                // If we have processed an ETX, we have completed a message.
                _byteBuffer.put(ch);
                if (ch == ASCII_ETX) {
                    var message = UTSMessage.create(Arrays.copyOfRange(_inputBuffer, 0, _byteBuffer.position()));
                    if (message instanceof StatusPollMessage) {
                        // Send a StatusMessage in response
                        _utsListener.socketTrafficReceived(this, message);
                        try {
                            write(new StatusMessage());
                        } catch (IOException ex) {
                            IO.println("Cannot send status message: " + ex.getMessage());
                            close();
                            return;
                        }
                    } else if (message instanceof StatusMessage) {
                        // TODO at some point we might check these against a timer to detect a dropped session
                        //   but for now, just ignore this one
                    } else if (message instanceof DisconnectMessage) {
                        // Tear down the session
                        close();
                        return;
                    } else {
                        // Everything else goes to the listener
                        _utsListener.socketTrafficReceived(this, message);
                    }
                    _byteBuffer.clear();
                }
            }
        }
    }

    @Override
    public void socketTrafficTraced(final SocketHandler source) {
        _utsListener.socketTrafficTraced(this);
    }

    /**
     * Writes a UTSMessage to the socket.
     * @param message the message to write
     * @throws IOException if writing to the socket fails
     */
    public void write(final UTSMessage message) throws IOException {
        super.write(message.getBuffer().position(0));
    }
}

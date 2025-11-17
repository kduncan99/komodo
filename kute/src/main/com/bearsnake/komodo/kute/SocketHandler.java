/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import com.bearsnake.komodo.kute.exceptions.StreamException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.bearsnake.komodo.kute.Constants.*;

/*
 * SocketHandler usage:
 * Upon connection request, Terminal creates a new SocketHandler.
 * During instantiation, we create a socket and get input and output streams.
 *  If this fails, we throw an exception, and Terminal should proceed accordingly.
 * Otherwise, we start up a thread reading input.
 * At any point, Terminal can post output via send(). If this fails, we will close the socket and
 *  terminate ourselves, returning false. Terminal may handle this condition, or wait upon a subsequent
 *  detection that we are no longer active.
 * We will read input as it arrives. If we detect an error in the input, we *may* close the socket and
 *  terminate, and Terminal should eventually notice we are no longer active.
 * Once a complete message arrives, we'll send it to the Terminal for further processing.
 */
class SocketHandler extends Thread {

    private final Terminal _terminal;
    private Socket _socket;
    private InputStream _inputStream;
    private OutputStream _outputStream;
    private boolean _active;
    private boolean _terminate;

    public SocketHandler(
        Terminal terminal,
        final String hostAddress,
        final int hostPort
    ) throws IOException {
        _terminal = terminal;
        _socket = new Socket(hostAddress, hostPort);
        _socket.setTcpNoDelay(true);
        _socket.setKeepAlive(true);
        _socket.setReuseAddress(true);

        _inputStream = _socket.getInputStream();
        _outputStream = _socket.getOutputStream();

        _terminate = false;

        start();
    }

    public boolean isActive() {
        return _active;
    }

    public boolean send(final StreamBuffer strm) {
        try {
            _outputStream.write(strm.getArray(), 0, strm.getPosition());
            return true;
        } catch (IOException e) {
            System.out.println("Error writing to socket:" + e);
            close();
            return false;
        }
    }

    public void run() {
        _active = true;
        ByteArrayOutputStream pending = null;
        while (!_terminate) {
            try {
                var ch = _inputStream.read();
                // If there is a pending object, we have read an SOH and are working on buffering a message.
                if (pending != null) {
                    if (ch == ASCII_ETX) {
                        // ETX reached - message is complete.
                        _terminal.ingestTraffic(pending.toByteArray(), pending.size());
                        pending = null;
                    } else {
                        pending.write(ch);
                    }
                } else {
                    // There is no pending object. If we read anything, it better be an SOH.
                    if (ch != ASCII_SOH) {
                        // TODO handle error condition (maybe kill the connection?)
                    } else {
                        pending = new ByteArrayOutputStream(2048);
                    }
                }
            } catch (IOException ex) {
                // Cannot read from the socket - post an error
                System.out.println("Cannot read from connection:" + ex);
            }
        }

        if (!_socket.isClosed()) {
            try {
                _socket.close();
            } catch (IOException ex) {
                System.out.println("Cannot close connection:" + ex);
            }
        }

        _socket = null;
        _inputStream = null;
        _outputStream = null;
        _active = false;
    }

    public void close() {
        if (!_socket.isClosed()) {
            try {
                _socket.close();
            } catch (IOException ex) {
                System.out.println("Error forcing socket close:" + ex.getMessage());
            }
        }
        _terminate = true;
    }
}

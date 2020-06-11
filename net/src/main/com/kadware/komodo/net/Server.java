/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javax.net.ServerSocketFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;

public class Server {

    final int _port;
    private ServerSocket _socket = null;
    private boolean _terminate = false;

    private static final int ACCEPT_TIMEOUT_MILLIS = 1000;
    private static final Logger LOGGER = LogManager.getLogger(Server.class);

    public Server(
        final int port
    ) {
        _port = port;
    }

    /**
     * Default handler for generic sockets - just close the socket.
     * Classes which actually do something with generic sockets should extend this class and override this method.
     */
    void handleNewSocket(
        final Socket socket
    ) {
        LOGGER.info("Default action - closing socket");
        try {
            socket.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Starts the server
     */
    public void start(
    ) throws IOException {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "start");
        _socket = ServerSocketFactory.getDefault().createServerSocket(_port);
        _socket.setSoTimeout(ACCEPT_TIMEOUT_MILLIS);
        _terminate = false;
        new ServerThread().start();
        LOGGER.traceExit(em);
    }

    /**
     * Starts the server
     * @param alternateSocket for the subclass
     */
    final void start(
        final ServerSocket alternateSocket
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "start");
        _socket = alternateSocket;
        _terminate = false;
        new ServerThread().start();
        LOGGER.traceExit(em);
    }

    /**
     * Stops the server
     */
    public final void stop(
    ) throws IOException {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "start");

        if (!_socket.isClosed()) {
            _socket.close();
        }
        LOGGER.traceExit(em);
    }

    /**
     * Async thread which listens for connections on the listen socket
     */
    private class ServerThread extends Thread {

        public void run() {
            EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                this.getClass().getSimpleName(),
                                                "run");
            int consecutiveErrors = 0;
            while (!_terminate) {
                try {
                    Socket newSocket = _socket.accept();
                    consecutiveErrors = 0;
                    handleNewSocket(newSocket);
                } catch (SocketTimeoutException ex) {
                    //  This is normal - ignore it
                    consecutiveErrors = 0;
                } catch (IOException ex) {
                    //  This is not normal - report it and don't let it happen too many times
                    LOGGER.catching(ex);
                    ++consecutiveErrors;
                    if (consecutiveErrors >= 5) {
                        LOGGER.error("Shutting down due to too many consecutive errors");
                        _terminate = true;
                    }
                }
            }

            LOGGER.traceExit(em);
        }
    }
}

/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;

@SuppressWarnings("DuplicatedCode")
public class WebServer {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data items
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(WebServer.class.getSimpleName());

    private static final int CONNECTION_BACKLOG             = 32;
    private static final int THREAD_POOL_SIZE               = 32;

    private final int _portNumber;
    private HttpServer _server;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param portNumber port number upon which we listen
     */
    public WebServer(
        final int portNumber
    ) {
        _portNumber = portNumber;
    }

    /**
     * Appends a handler with an associated path to the created server.
     * Call after setup() and before start()
     */
    public void appendHandler(
        final String path,
        final HttpHandler handler
    ) {
        _server.createContext(path, handler);
    }

    /**
     * Sets up the WebServer.
     */
    public void setup(
    ) throws IOException {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "setup");

        try {
            _server = HttpServer.create();
            InetAddress inetAddress = InetAddress.getByName("::");
            InetSocketAddress isAddr = new InetSocketAddress(inetAddress, _portNumber);
            _server.bind(isAddr, CONNECTION_BACKLOG);
            LOGGER.traceExit(em);
        } catch (Exception ex) {
            LOGGER.catching(ex);
            LOGGER.traceExit(em);
            throw ex;
        }
    }

    public int getPortNumber() {
        return _portNumber;
    }

    /**
     * Starts the server
     */
    public void start() {
        _server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
        _server.start();
    }

    /**
     * Stops the server
     */
    public void stop() {
        _server.stop(0);
    }
}

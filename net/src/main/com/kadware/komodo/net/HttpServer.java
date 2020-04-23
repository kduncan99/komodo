/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;

public class HttpServer extends Server {

    private final Map<String, HttpHandler> _handlers = new LinkedHashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(HttpServer.class);

    public HttpServer(
        final int port
    ) {
        super(port);
    }

    /**
     * Registers a handler along with the path it is servicing
     */
    void registerHandler(
        final String path,
        final HttpHandler handler
    ) {
        _handlers.put(path, handler);
    }

    /**
     * Handles a newly-accepted socket
     */
    @Override
    void handleNewSocket(
        final Socket socket
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(socket={})",
                                            this.getClass().getSimpleName(),
                                            "handleNewSocket",
                                            socket);

        //  Read from the socket and convert the data into an HttpRequest object.
        //  Then check the paths and pass the object to the appropriate handler.
        //  If there isn't a handler, send a 404. If the request is fubar, send a 400.
        byte[] rawData;
        try {
            rawData = socket.getInputStream().readAllBytes();
        } catch (IOException ex) {
            LOGGER.catching(ex);
            try {
                HttpResponse.createInternalServerError(ex.getMessage().getBytes(StandardCharsets.UTF_8)).send(socket);
                socket.close();
            } catch (IOException ey) {
                LOGGER.catching(ey);
            }
            LOGGER.traceExit(em);
            return;
        }

        HttpRequest request;
        try {
            request = HttpRequest.create(rawData);
        } catch (DataException ex) {
            LOGGER.catching(ex);
            try {
                HttpResponse.createBadRequest(ex.getMessage().getBytes(StandardCharsets.UTF_8)).send(socket);
                socket.close();
            } catch (IOException ey) {
                LOGGER.catching(ey);
            }
            LOGGER.traceExit(em);
            return;
        }

        for (Map.Entry<String, HttpHandler> entry : _handlers.entrySet()) {
            if (request._requestURI.startsWith(entry.getKey())) {
                entry.getValue().handle(socket, request);
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ex) {
                    LOGGER.catching(ex);
                }
                LOGGER.traceExit(em);
                return;
            }
        }

        try {
            HttpResponse.createNotFound(null).send(socket);
            socket.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
        LOGGER.traceExit(em);
    }
}

/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.networkManager;

import com.bearsnake.komodo.kexec.exceptions.SystemException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Server for DEMAND mode clients using TELNET protocol
 */
public class TelnetListener extends Listener implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(TelnetListener.class);

    private boolean _terminate = false;
    private Thread _thread;

    public TelnetListener(final InetAddress address,
                          final int port) {
        super(address, port);
    }

    @Override
    void shutdown() throws SystemException {
        _terminate = true;
        try {
            _serverSocket.close();
            _serverSocket = null;
        } catch (IOException ex) {
            throw new SystemException("Cannot close socket:" + ex, ex);
        }
    }

    @Override
    void startup() throws SystemException {
        _terminate = false;
        try {
            _serverSocket = new ServerSocket(_port, 4, _address);
        } catch (IOException ex) {
            throw new SystemException("Cannot get server socket:" + ex, ex);
        }
        _thread = new Thread(this);
        _thread.start();
    }

    @Override
    public String toString() {
        return String.format("TelnetListener:Addr=%s Port=%d", _address, _port);
    }

    @Override
    public void run() {
        while (!_terminate) {
            try {
                var socket = _serverSocket.accept();
                // TODO create a new telnet session
            } catch (IOException ex) {
                LOGGER.log(_terminate ? Level.INFO : Level.ERROR, "Cannot accept socket:{}", ex.getMessage());
                _terminate = true;
            }
        }
    }
}

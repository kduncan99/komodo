/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.networkManager;

import com.bearsnake.komodo.kexec.exceptions.SystemException;

import java.net.InetAddress;
import java.net.ServerSocket;

abstract class Listener {

    protected final InetAddress _address;
    protected final int _port;
    protected ServerSocket _serverSocket;

    protected Listener(final InetAddress address,
                       final int port) {
        _address = address;
        _port = port;
    }

    abstract void startup() throws SystemException;
    abstract void shutdown() throws SystemException;

    @Override
    public abstract String toString();
}

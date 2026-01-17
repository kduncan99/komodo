/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.networkManager;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exceptions.SystemException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Handles non-hardware networking
 */
public class NetworkManager implements Manager {

    private static final String LOG_SOURCE = "NetworkMgr";

    private final Collection<Listener> _listeners = new LinkedList<>();

    public NetworkManager() {
        Exec.getInstance().managerRegister(this);
    }

    @Override
    public void boot(boolean recoveryBoot) throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);

        try {
            for (Listener _listener : _listeners) {
                _listener.startup();
            }
        } catch (SystemException sex) {
            for (Listener _listener : _listeners) {
                _listener.shutdown();
            }
            throw sex;
        }
    }

    @Override
    public void close() {
        LogManager.logTrace(LOG_SOURCE, "close()");
        _listeners.clear();
    }

    @Override
    public void dump(PrintStream out, String indent, boolean verbose) {
        out.printf("%sNetworkManager ********************************\n", indent);
        _listeners.forEach(server -> out.printf("%s  %s", indent, server.toString()));
    }

    @Override
    public void initialize() throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "initialize()");

        for (var cfgNet : Exec.getInstance().getConfiguration().getNetworks()) {
            try {
                var addr = InetAddress.getByName(cfgNet._addressString);
                if (cfgNet._demandPort != null) {
                    _listeners.add(new TelnetListener(addr, cfgNet._demandPort));
                }
                if (cfgNet._consolePort != null) {
//TODO                    _servers.add(new ConsoleServer(addr, cfgNet._demandPort));
                }
            } catch (UnknownHostException e) {
                LogManager.logError(LOG_SOURCE, "Cannot convert %s to internet address", cfgNet._addressString);
            }
        }
    }

    @Override
    public void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
    }

    // -----------------------------------------------------------------------------------------------------------------
}

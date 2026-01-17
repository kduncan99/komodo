/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.ResourceException;

import java.net.Socket;

/**
 * handles DEMAND sessions using the TELNET protocol.
 * We expect to be handed a socket which has just been opened in stream mode.
 * We handle all TELNET negotiation.
 */
public final class RemoteTelnetSymbiont extends RemoteDemandSymbiont {

    private final Socket _socket;

    public RemoteTelnetSymbiont(
        final Socket socket
    ) throws ResourceException {
        super();
        _socket = socket;
    }

    @Override
    boolean poll() throws ExecStoppedException {
        return false;// TODO
    }
}

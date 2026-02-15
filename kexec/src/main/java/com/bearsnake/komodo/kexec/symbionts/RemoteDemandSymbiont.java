/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.ResourceException;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.util.LinkedList;

/**
 * handles DEMAND sessions without requiring a CMS-class application
 * It expects someone to pass an open socket to us, which we then use to communicate
 * with a UTS or TELNET-class client (via subclasses)
 */
public abstract class RemoteDemandSymbiont extends RemoteSymbiont {

    private static final LinkedList<String> INSTANCE_NAMES = new LinkedList<>();

    protected RemoteDemandSymbiont() throws ResourceException {
        super(generateUniqueName());
    }

    private static String generateUniqueName() throws ResourceException {
        synchronized (INSTANCE_NAMES) {
            var gccmin = Exec.getInstance().getConfiguration().getIntegerValue(Tag.GCCMIN);
            if (INSTANCE_NAMES.size() >= gccmin) {
                throw new ResourceException("Too many RSI sessions");
            }

            for (int i = 1; i < 999; i++) {
                String name = String.format("RSI%03d", i);
                if (!INSTANCE_NAMES.contains(name)) {
                    INSTANCE_NAMES.add(name);
                    return name;
                }
            }
        }
        throw new ResourceException("Too many RSI sessions");
    }

    @Override
    abstract boolean poll() throws ExecStoppedException;

    @Override
    public String getStateString() {
        return "";// TODO go to SOS to see what we should display
    }

    @Override
    public final void initialize() {
        // TODO
    }

    @Override
    public final void lockDevice() {
        // Does not apply to DEMAND sessions
    }

    @Override
    public final void reposition(int count) {
        // Does not apply to DEMAND sessions
    }

    @Override
    public final void setPageGeometry(final Integer linesPerPage,
                                      final Integer topMargin,
                                      final Integer bottomMargin,
                                      final Integer linesPerInch) {
        // Does not apply to DEMAND sessions
    }

    @Override
    public final void repositionAll() {
        // Does not apply to DEMAND sessions
    }

    @Override
    public final void requeue() {
        // Does not apply to DEMAND sessions
    }

    @Override
    public final void suspend() {
        // Does not apply to DEMAND sessions
    }

    @Override
    public final void terminateDevice() {
        // TODO
    }

    @Override
    public final void terminateFile() {
        // Does not apply to DEMAND sessions
    }
}

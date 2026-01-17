/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.ResourceException;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.rmi.Remote;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSI (including DEMAND mode) symbiont handler
 * TODO - need subclasses for DEMAND and remote print/readers (and punch?)
 */
abstract class RemoteSymbiont extends Symbiont {

    protected RemoteSymbiont(
        final String symbiontName
    ) {
        super(symbiontName);
    }

    @Override
    public abstract String getStateString();

    @Override
    public final boolean isInputSymbiont() {
        return true;
    }

    @Override
    public final boolean isOnSiteSymbiont() {
        return false;
    }

    @Override
    public final boolean isOutputSymbiont() {
        return true;
    }

    @Override
    public final boolean isPrintSymbiont() {
        return true;
    }

    @Override
    public final boolean isRemoteSymbiont() {
        return true;
    }

    @Override
    public void initialize() {
        // TODO
    }

    @Override
    public void lockDevice() {
        // TODO
    }

    @Override
    public void reposition(int count) {
        // TODO
    }

    @Override
    public abstract void setPageGeometry(final Integer linesPerPage,
                                         final Integer topMargin,
                                         final Integer bottomMargin,
                                         final Integer linesPerInch);

    @Override
    public abstract void repositionAll();

    @Override
    public abstract void requeue();

    @Override
    public abstract void suspend();

    @Override
    public abstract void terminateFile();

    @Override
    abstract boolean poll() throws ExecStoppedException;
}

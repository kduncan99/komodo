/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * A few things common to Print, Punch, and Reader symbionts
 */
abstract class OnSiteSymbiont extends Symbiont {

    public OnSiteSymbiont(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
    }

    @Override
    abstract boolean poll() throws ExecStoppedException;

    @Override
    abstract public String getStateString();

    @Override
    public abstract boolean isInputSymbiont();

    @Override
    public final boolean isOnSiteSymbiont() {
        return true;
    }

    @Override
    public abstract boolean isOutputSymbiont();

    @Override
    public abstract boolean isPrintSymbiont();

    @Override
    public final boolean isRemoteSymbiont() {
        return false;
    }

    @Override
    public abstract void initialize() throws ExecStoppedException;

    @Override
    public abstract void lockDevice();

    @Override
    public abstract void reposition(int count) throws ExecStoppedException;

    @Override
    public abstract void repositionAll() throws ExecStoppedException;

    @Override
    public abstract void requeue();

    @Override
    public abstract void setPageGeometry(final Integer linesPerPage,
                                         final Integer topMargin,
                                         final Integer bottomMargin,
                                         final Integer linesPerInch);

    @Override
    public abstract void suspend();

    @Override
    public abstract void terminateFile() throws ExecStoppedException;
}

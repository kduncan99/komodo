/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * RSI (including DEMAND mode) symbiont handler
 * TODO - need subclasses for DEMAND and remote print/readers (and punch?)
 */
abstract class RemoteSymbiont extends Symbiont {

    public RemoteSymbiont(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
    }

    @Override
    boolean poll() throws ExecStoppedException {
        return false;// TODO
    }

    @Override
    public String getStateString() {
        return "";// TODO
    }

    @Override
    public abstract boolean isInputSymbiont();

    @Override
    public abstract boolean isOnSiteSymbiont();

    @Override
    public abstract boolean isOutputSymbiont();

    @Override
    public abstract boolean isPrintSymbiont();

    @Override
    public abstract boolean isRemoteSymbiont();

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
    public final void setPageGeometry(final Integer linesPerPage,
                                      final Integer topMargin,
                                      final Integer bottomMargin,
                                      final Integer linesPerInch) {
        // TODO applies to RSI devices...
    }

    @Override
    public void repositionAll() {
        // TODO
    }

    @Override
    public void requeue() {
        // TODO
    }

    @Override
    public final void suspend() {
        // TODO
    }

    @Override
    public final void terminateFile() {
        // TODO
    }
}

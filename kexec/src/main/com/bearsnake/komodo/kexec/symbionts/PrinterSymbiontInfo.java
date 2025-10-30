/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * Handles printer-type output symbionts.
 * Reads files in printer output queues, performs necessary formatting, and sends appropriate line or page
 * images to the destination (which might be a virtual printer, a real printer queue on the host, or a real printer).
 */
class PrinterSymbiontInfo extends OnSiteSymbiontInfo {

    private int _linesPerPage;
    private int _topMargin;
    private int _bottomMargin;
    private int _linesPerInch;

    private int _pageCount = 0;

    public PrinterSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);

        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();

        // Set page geometry according to config defaults
        _linesPerPage = (int)(long)cfg.getIntegerValue(Tag.STDPAG);
        _topMargin = (int)(long)cfg.getIntegerValue(Tag.SHDGSP);
        _bottomMargin = (int)(long)cfg.getIntegerValue(Tag.STDBOT);
        _linesPerPage = 8;
    }

    @Override
    public String getStateString() {
        return String.format("%s %s,%s,PAGES PRINTED = %d", _node.getNodeName(), _status, _state, _pageCount);
    }

    @Override
    public boolean isInputSymbiont() {
        return false;
    }

    @Override
    public boolean isOutputSymbiont() {
        return true;
    }

    @Override
    public boolean isPrintSymbiont() {
        return true;
    }

    // -------------------------------------------------------------------------

    /**
     * SM symbiont I keyin
     */
    @Override
    public final void initialize() {
        // TODO
    }

    @Override
    public void lockDevice() {
        // TODO
    }

    @Override
    public void reposition(int count) throws ExecStoppedException {
        // TODO
    }

    @Override
    public void repositionAll() throws ExecStoppedException {
        // TODO
    }

    @Override
    public void requeue() {
        // TODO
    }

    @Override
    public void setPageGeometry(final Integer linesPerPage,
                                final Integer topMargin,
                                final Integer bottomMargin,
                                final Integer linesPerInch) {
        // TODO
    }

    @Override
    public void suspend() {
        // TODO
    }

    @Override
    public void terminateFile() throws ExecStoppedException {
        // TODO
    }

    // -------------------------------------------------------------------------

    /**
     * Implements state machine for PrinterSymbiontInfo
     * @return true if we did something useful, false if we are waiting for something.
     */
    @Override
    boolean poll() throws ExecStoppedException {
        var result = false;

//        if (!_isSuspended) {
//            // TODO
//        }

        return result;
    }
}

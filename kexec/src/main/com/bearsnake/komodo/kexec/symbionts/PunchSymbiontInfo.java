/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * Handles punch-type output symbionts.
 * Reads files in punch output queues, performs necessary formatting, and sends appropriate card images to the destination
 * (which will generally be a local or remote holding area for image files - we don't expect ever to have real card punch devices).
 */
class PunchSymbiontInfo extends OnSiteSymbiontInfo {

    private int _imageCount = 0;

    public PunchSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
    }

    @Override
    public String getStateString() {
        return String.format("%s %s,%s,CARDS PUNCHED = %d", _node.getNodeName(), _status, _state, _imageCount);
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
        return false;
    }

    // -------------------------------------------------------------------------

    /**
     * SM symbiont I keyin
     */
    @Override
    public synchronized final void initialize() {
        // TODO
    }

    @Override
    public void lockDevice() {
        // TODO
    }

    @Override
    public synchronized final void reposition(int count) {
        // TODO
    }

    @Override
    public synchronized final void repositionAll() {
        // TODO
    }

    @Override
    public synchronized final void requeue() {
        // TODO
    }

    /**
     * For SM * C... but it has no applicability for card punches.
     */
    @Override
    public final void setPageGeometry(final Integer linesPerPage,
                                      final Integer topMargin,
                                      final Integer bottomMargin,
                                      final Integer linesPerInch) {
        // Ignore this... SM keyin should never invoke it
    }

    @Override
    public synchronized final void suspend() {
        // TODO
    }

    @Override
    public synchronized final void terminateFile() throws ExecStoppedException {
        // TODO
    }

    // -------------------------------------------------------------------------

    /**
     * Implements state machine for PunchSymbiontInfo
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

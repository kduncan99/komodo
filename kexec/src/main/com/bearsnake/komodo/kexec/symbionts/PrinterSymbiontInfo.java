/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * Handles printer-type output symbionts.
 * Reads files in printer output queues, performs necessary formatting, and sends appropriate line or page
 * images to the destination (which might be a virtual printer, a real printer queue on the host, or a real printer).
 */
class PrinterSymbiontInfo extends SymbiontInfo {

    private int _pageCount = 0;

    public PrinterSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
    }

//    @Override
//    void end() throws ExecStoppedException {
//        // TODO
//    }

    @Override
    public String getStateString() {
        return String.format("%s %s,%s,PAGES PRINTED = %d", _node.getNodeName(), _status, _state, _pageCount);
    }

    /**
     * SM symbiont I keyin
     */
    @Override
    public void initialize() throws ExecStoppedException {
        // TODO
    }

    /**
     * SM symbiont L keyin
     */
    @Override
    public void lock() throws ExecStoppedException {
        // TODO
    }

//    @Override
//    void reposition(int count) throws ExecStoppedException {
//        // count must be zero, but we do not enforce that - we just ignore it.
//        // TODO
//    }
//
//    @Override
//    void requeue() throws ExecStoppedException {
//        // TODO
//    }
//
//    @Override
//    void suspend() throws ExecStoppedException {
//        // TODO
//    }
//
//    @Override
//    void terminate() throws ExecStoppedException {
//        // TODO
//    }

    /**
     * Implements state machine for OutputSymbiontInfo
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

/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * Handles input symbionts - essentially card readers, virtual or otherwise.
 */
class PunchSymbiontInfo extends SymbiontInfo {

    private int _imageCount = 0;

    public PunchSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
        _channelProgram.setFunction(ChannelProgram.Function.Write);
    }

//    @Override
//    void end() throws ExecStoppedException {
//        // TODO
//    }

    @Override
    public String getStateString() {
        return String.format("%s %s,%s,CARDS PUNCHED = %d", _node.getNodeName(), _status, _state, _imageCount);
    }

//    @Override
//    void initiate() throws ExecStoppedException {
//        // TODO
//    }
//
//    @Override
//    void lock() throws ExecStoppedException {
//        // TODO
//    }
//
//    @Override
//    void reposition(int count) throws ExecStoppedException {
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

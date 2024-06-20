/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 *
 */
class InputSymbiontInfo extends SymbiontInfo {

    public InputSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
        _channelProgram.setFunction(ChannelProgram.Function.Read);
    }

    /**
     * Implements state machine for InputSymbiontInfo
     * @return true if we did something useful, false if we are waiting for something.
     */
    @Override
    boolean poll() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();

        if (!_node.isReady() && (_state != SymbiontDeviceState.Quiesced)) {
            // This should not happen - we are in the middle of reading a deck, and the device rudely went offline.
            // TODO
            return false;
        }

        return switch (_state) {
            case Quiesced -> pollQuiesced();
            case PreRun -> pollPreRun();
            default -> pollDefault();
        };
    }

    /**
     * We have not yet read a @RUN card, but the IO to do so has been issued.
     * If the IO is complete, do @RUN card processing.
     */
    private boolean pollPreRun() throws ExecStoppedException {
        if (_channelProgram.getIoStatus() == IoStatus.InProgress) {
            return false;
        } else if (_channelProgram.getIoStatus() != IoStatus.Complete) {
            // IO error - abort the input, do NOT delete the deck, DN the device
            return false;
        } else {
            // We have an image - is it a @RUN card?
            // If so, pull the necessary information from it, create a READ$ file,
            // and advance the state. If not, do missing-@RUN-card algorithm
            var image = _buffer.toASCII();
            var split = image.split("[, ]");
            if (split[0].equalsIgnoreCase("@RUN")) {
                // TODO
                return true;
            } else {
                // TODO
                return false;
            }
        }
    }

    /**
     * The symbiont was not active, but we've found it to be ready.
     * Start an IO to read a run-card and advance the state.
     */
    private boolean pollQuiesced() throws ExecStoppedException {
        try {
            var exec = Exec.getInstance();
            var fm = exec.getFacilitiesManager();
            var msg = _node.getNodeName() + " ACTIVE";
            exec.sendExecReadOnlyMessage(msg);
            fm.routeIo(_channelProgram);
            _state = SymbiontDeviceState.PreRun;
            return true;
        } catch (NoRouteForIOException ex) {
            // abort the input, do NOT delete the deck, and DN the device
            // TODO
            return false;
        }
    }
}

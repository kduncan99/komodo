/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.SymbiontDevice;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.logger.LogManager;

/**
 * Information (and more importantly, common functionality) relating to any symbiont device.
 */
abstract class SymbiontInfo {

    protected static final String LOG_SOURCE = "SymbMgr";

    protected final NodeInfo _nodeInfo;
    protected final SymbiontDevice _node;
    protected final int _nodeIdentifier;
    protected final ArraySlice _buffer;
    protected final ChannelProgram _channelProgram;
    protected final ChannelProgram.ControlWord _controlWord;

    protected boolean _abort;
    protected boolean _isSuspended;
    protected boolean _isLocked;
    protected boolean _isWaiting;
    protected SymbiontDeviceState _state;

    protected SymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        _nodeInfo = nodeInfo;
        _node = (SymbiontDevice) _nodeInfo.getNode();
        _nodeIdentifier = _node.getNodeIdentifier();
        _isSuspended = false;
        _isLocked = true;
        _state = SymbiontDeviceState.Quiesced;

        _buffer = new ArraySlice(new long[132]);
        _controlWord = new ChannelProgram.ControlWord().setBuffer(_buffer)
                                                       .setDirection(ChannelProgram.Direction.Increment);
        _channelProgram = new ChannelProgram().addControlWord(_controlWord)
                                              .setNodeIdentifier(_nodeIdentifier);
    }

    /**
     * State machine for subclass
     * @return true if we did something useful, false if we are waiting for something.
     */
    abstract boolean poll() throws ExecStoppedException;

    /**
     * Default handler for invalid state values
     */
    protected boolean pollDefault() throws ExecStoppedException {
        LogManager.logFatal(LOG_SOURCE, "Symbiont state %s not valid for %s", _state, _node.getNodeName());
        Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
        throw new ExecStoppedException();
    }

    /**
     * Issues a reset for the underlying node.
     * Can be issued while another IO using the object's generic buffer is in progress.
     * @return true generally, false if the IO failed
     */
    private boolean resetNode(
        final NodeInfo nodeInfo
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();

        try {
            var cp = new ChannelProgram().setNodeIdentifier(nodeInfo.getNode().getNodeIdentifier())
                                         .setFunction(ChannelProgram.Function.Control)
                                         .setSubFunction(ChannelProgram.SubFunction.Reset);
            exec.getFacilitiesManager().routeIo(cp);
            while (cp.getIoStatus() == IoStatus.InProgress) {
                Exec.sleep(10);
            }
            if (cp.getIoStatus() != IoStatus.Complete) {
                var msg = String.format("%s IO error - %s", nodeInfo.getNode().getNodeName(), cp.getIoStatus());
                exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
                fm.setNodeStatus(_nodeIdentifier, NodeStatus.Down, ConsoleType.InputOutput);
                _abort = true;
                return false;
            }
        } catch (NoRouteForIOException e) {
            var msg = String.format("%s IO error - no route to device", nodeInfo.getNode().getNodeName());
            exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
            fm.setNodeStatus(_nodeIdentifier, NodeStatus.Down, ConsoleType.InputOutput);
            _abort = true;
            return false;
        }

        return true;
    }
}

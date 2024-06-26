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
    protected static final String NO_ROUTE_FOR_IO_MSG = "No Route to Device";

    protected final NodeInfo _nodeInfo;
    protected final SymbiontDevice _node;
    protected final int _nodeIdentifier;
    protected final ArraySlice _buffer;
    protected final ChannelProgram _channelProgram;
    protected final ChannelProgram.ControlWord _controlWord;

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
     * For handling SM * E keyins - Terminates the active file
     * For input, an EOF is written and the remainder of the input is discarded (but the run is not deleted)
     * For output, no more output is produced for the file - it is effectively a skip-to-end.
     */
    abstract void end() throws ExecStoppedException;

    /**
     * For handling SM * I keyins - initiates the device
     * (Re)starts the device.
     */
    abstract void initiate() throws ExecStoppedException;

    /**
     * For handling SM * L keyins - Locks out the device
     * Any current file runs to completion, but no subsequent file is processed
     */
    abstract void lock() throws ExecStoppedException;

    /**
     * For handling SM * R keyins - Rewinds or advances by a number of pages or cards
     * For input files the current file is bypassed until the next @RUN image, and the run is discarded.
     * For output files a negative count rewinds by the given number of cards or pages and then output is resumed.
     * A positive count advances printing by the given number of cards or pages.
     * @param count number of cards or pages (must be zero for input symbiont)
     */
    abstract void reposition(final int count) throws ExecStoppedException;

    /**
     * For handling SM * Q keyins - Re-queues current file and locks the device
     * Does not apply to input symbionts.
     */
    abstract void requeue() throws ExecStoppedException;

    /**
     * For handling SM * S keyins - Suspends the device
     * Immediately pauses input or output.
     */
    abstract void suspend() throws ExecStoppedException;

    /**
     * For handling SM * T keyins - Terminates and locks out the device
     * The remainder of the file is discarded and the devices is locked out.
     * For input, the run is discarded.
     */
    abstract void terminate() throws ExecStoppedException;

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
     * Notifies the console that the symbiont encountered an IO error,
     * and sets the device status to DN.
     * @param detail detail message
     * @return true if caller should retry, false if we abort
     */
    protected boolean notifyConsoleIOError(
        final String detail,
        final boolean allowRetry
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        String msg;
        String[] responses;
        if (allowRetry) {
            msg = String.format("%s IO error - %s: AE", _node.getNodeName(), detail);
            responses = new String[]{"A", "E"};
        } else {
            msg = String.format("%s IO error - %s: AE", _node.getNodeName(), detail);
            responses = new String[]{"E"};
        }
        var response = exec.sendExecRestrictedReadReplyMessage(msg, responses, ConsoleType.InputOutput);
        if (response.equalsIgnoreCase("E")) {
            fm.setNodeStatus(_nodeIdentifier, NodeStatus.Down, ConsoleType.InputOutput);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Issues a reset for the underlying node.
     * Can be issued while another IO using the object's generic buffer is in progress.
     * @return true generally, false if the IO (and all retries, if any) failed
     */
    protected boolean resetNode() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        var failed = false;
        var retry = true;
        while (retry) {
            try {
                var cp = new ChannelProgram().setNodeIdentifier(_nodeIdentifier)
                                             .setFunction(ChannelProgram.Function.Control)
                                             .setSubFunction(ChannelProgram.SubFunction.Reset);
                fm.routeIo(cp);
                while (cp.getIoStatus() == IoStatus.InProgress) {
                    Exec.sleep(10);
                }

                if (cp.getIoStatus() == IoStatus.Complete) {
                    retry = false;
                } else {
                    retry = notifyConsoleIOError(cp.getIoStatus().toString(), true);
                    failed = true;
                }
            } catch (NoRouteForIOException e) {
                retry = notifyConsoleIOError(NO_ROUTE_FOR_IO_MSG, true);
                failed = true;
            }
        }

        return !failed;
    }
}

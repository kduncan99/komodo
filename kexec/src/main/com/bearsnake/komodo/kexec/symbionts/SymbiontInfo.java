/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.channels.ChannelIoPacket;
import com.bearsnake.komodo.hardwarelib.channels.TransferFormat;
import com.bearsnake.komodo.hardwarelib.devices.SymbiontDevice;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.logger.LogManager;

/**
 * Information (and more importantly, common functionality) relating to any symbiont device.
 */
public abstract class SymbiontInfo implements Runnable {

    private static final int POLL_DELAY_MILLISECONDS = 1000;
    private static final int POLL_LONG_DELAY_MILLISECONDS = 5000;
    protected static final String NO_ROUTE_FOR_IO_MSG = "No Route to Device";

    protected final NodeInfo _nodeInfo;
    protected final SymbiontDevice _node;
    protected final int _nodeIdentifier;
    protected final ArraySlice _buffer;
    protected final ChannelIoPacket _channelPacket;

    protected SymbiontStatus _status;
    protected SymbiontState _state;

    private boolean _terminate = false;

    protected SymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        _nodeInfo = nodeInfo;
        _node = (SymbiontDevice) _nodeInfo.getNode();
        _nodeIdentifier = _node.getNodeIdentifier();
        _status = SymbiontStatus.Locked;
        _state = SymbiontState.Stopped;

        _buffer = new ArraySlice(new long[132]);
        _channelPacket = new ChannelIoPacket().setBuffer(_buffer)
                                              .setFormat(TransferFormat.QuarterWord)
                                              .setNodeIdentifier(_nodeIdentifier);
    }

    abstract boolean poll() throws ExecStoppedException;

    public final void run() {
        LogManager.logTrace(_node.getNodeName(), "SymbiontInfo thread starting");

        while (!_terminate) {
            try {
                int delay = 0;
                if (!Exec.getInstance().getGenFileInterface().isReady()) {
                    delay = POLL_LONG_DELAY_MILLISECONDS;
                } else {
                    delay = poll() ? 0 : POLL_DELAY_MILLISECONDS;
                }
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                LogManager.logCatching(_node.getNodeName(), ex);
            } catch (ExecStoppedException ex) {
                LogManager.logCatching(_node.getNodeName(), ex);
                _terminate = true;
            }
        }

        LogManager.logTrace(_node.getNodeName(), "SymbiontInfo thread stopped");
    }

    final void start() {
        _terminate = false;
        Thread thread = new Thread(this);
        thread.start();
    }

    final void terminate() {
        _terminate = true;
    }

//    /**
//     * For handling SM * E keyins - Terminates the active file
//     * For input, an EOF is written and the remainder of the input is discarded (but the run is not deleted)
//     * For output, no more output is produced for the file - it is effectively a skip-to-end.
//     */
//    abstract void end() throws ExecStoppedException;

    /**
     * Creates a string indicating the state of the symbiont device, to be sent to the console
     */
    public abstract String getStateString();

    /**
     * For handling SM * I keyins - initiates the device
     * (Re)starts the device.
     */
    public abstract void initialize() throws ExecStoppedException;

    /**
     * For handling SM * L keyins - Locks out the device
     * Any current file runs to completion, but no subsequent file is processed
     */
    public abstract void lock() throws ExecStoppedException;

//    /**
//     * For handling SM * R keyins - Rewinds or advances by a number of pages or cards
//     * For input files the current file is bypassed until the next @RUN image, and the run is discarded.
//     * For output files a negative count rewinds by the given number of cards or pages and then output is resumed.
//     * A positive count advances printing by the given number of cards or pages.
//     * @param count number of cards or pages (must be zero for input symbiont)
//     */
//    abstract void reposition(final int count) throws ExecStoppedException;
//
//    /**
//     * For handling SM * Q keyins - Re-queues current file and locks the device
//     * Does not apply to input symbionts.
//     */
//    abstract void requeue() throws ExecStoppedException;
//
//    /**
//     * For handling SM * S keyins - Suspends the device
//     * Immediately pauses input or output.
//     */
//    abstract void suspend() throws ExecStoppedException;
//
//    /**
//     * For handling SM * T keyins - Terminates and locks out the device
//     * The remainder of the file is discarded and the devices is locked out.
//     * For input, the run is discarded.
//     */
//    abstract void terminate() throws ExecStoppedException;

//    /**
//     * State machine for subclass
//     * @return true if we did something useful, false if we are waiting for something.
//     */
//    abstract boolean poll() throws ExecStoppedException;

//    /**
//     * Default handler for invalid state values
//     */
//    protected boolean pollDefault() throws ExecStoppedException {
//        LogManager.logFatal(LOG_SOURCE, "Symbiont state %s not valid for %s", _state, _node.getNodeName());
//        Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
//        throw new ExecStoppedException();
//    }

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
            msg = String.format("%s IO error - %s: E", _node.getNodeName(), detail);
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
                var channelPacket = new ChannelIoPacket().setNodeIdentifier(_nodeIdentifier)
                                                         .setIoFunction(IoFunction.Reset);
                fm.routeIo(channelPacket);
                if (channelPacket.getIoStatus() == IoStatus.Successful) {
                    retry = false;
                } else {
                    retry = notifyConsoleIOError(channelPacket.getIoStatus().toString(), true);
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

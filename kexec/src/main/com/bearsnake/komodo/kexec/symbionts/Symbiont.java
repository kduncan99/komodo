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
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.logger.LogManager;

/**
 * Information (and more importantly, common functionality) relating to any symbiont device.
 */
public abstract class Symbiont implements Runnable {

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

    protected Symbiont(
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

    public final String getNodeName() {
        return _node.getNodeName();
    }

    /**
     * State machine for subclass
     * @return true if we did something useful, false if we are waiting for something.
     */
    abstract boolean poll() throws ExecStoppedException;

    public final void run() {
        LogManager.logTrace(_node.getNodeName(), "SymbiontInfo thread starting");
        var exec = Exec.getInstance();

        while (!_terminate) {
            try {
                int delay = 0;
                if (!exec.getGenFileInterface().isReady()) {
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
            } catch (Throwable t) {
                LogManager.logCatching(_node.getNodeName(), t);
                exec.stop(StopCode.ExecActivityTakenToEMode);
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

    /**
     * Creates a string indicating the state of the symbiont device, to be sent to the console
     */
    public abstract String getStateString();

    /**
     * For handling SM * I keyins - initiates the device
     * (Re)starts the device.
     */
    public abstract void initialize() throws ExecStoppedException;

    public abstract boolean isInputSymbiont();
    public abstract boolean isOnSiteSymbiont();
    public abstract boolean isOutputSymbiont();
    public abstract boolean isPrintSymbiont();
    public abstract boolean isRemoteSymbiont();

    /**
     * For handling SM * L keyins - Locks out the device
     * Any current file runs to completion, but no subsequent file is processed
     */
    public abstract void lockDevice();

    /**
     * For handling SM * R keyins - Rewinds or advances by a number of pages or cards
     * For input files the current file is bypassed until the next @RUN image, and the run is discarded --
     * if count is specified, it is ignored (not specifying a count is only allowed for input).
     * For output files a negative count rewinds by the given number of cards or pages and then output is resumed.
     * A positive count advances printing by the given number of cards or pages.
     * @param count number of cards or pages (must be zero for input symbiont)
     */
    public abstract void reposition(final int count) throws ExecStoppedException;

    /**
     * For handling SM * RALL keyins - Rewinds an entire output file.
     * For input files the current file is bypassed until the next @RUN image, and the run is discarded.
     */
    public abstract void repositionAll() throws ExecStoppedException;

    /**
     * For handling SM * Q keyins - Re-queues current file and locks the device
     * Does not apply to input symbionts.
     */
    public abstract void requeue();

    /**
     * For handling SM * C[HANGE] keyins - applies only to print symbions, local or remote
     * If any particular parameter is null, its value is not changed.
     */
    public abstract void setPageGeometry(final Integer linesPerPage,
                                         final Integer topMargin,
                                         final Integer bottomMargin,
                                         final Integer linesPerInch);

    /**
     * For handling SM * S keyins - Suspends the device
     * Immediately pauses input or output.
     */
    public abstract void suspend();

    /**
     * For handling SM * E and SM * T keyins
     * Either will discard the remainder of the file - for input, the run we are building is discarded.
     * For SM * T, the device should be locked out before invoking this.
     */
    public abstract void terminateFile() throws ExecStoppedException;

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

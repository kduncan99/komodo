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

/**
 * A few things common to Print, Punch, and Reader symbionts
 */
abstract class OnSiteSymbiont extends Symbiont {

    protected static final String NO_ROUTE_FOR_IO_MSG = "No Route to Device";

    protected final NodeInfo _nodeInfo;
    protected final SymbiontDevice _node;
    protected final int _nodeIdentifier;
    protected final ArraySlice _buffer;
    protected final ChannelIoPacket _channelPacket;

    protected SymbiontStatus _status;
    protected SymbiontState _state;

    public OnSiteSymbiont(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo.getNode().getNodeName());

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

    @Override
    abstract boolean poll() throws ExecStoppedException;

    @Override
    abstract public String getStateString();

    @Override
    public abstract boolean isInputSymbiont();

    @Override
    public final boolean isOnSiteSymbiont() {
        return true;
    }

    @Override
    public abstract boolean isOutputSymbiont();

    @Override
    public abstract boolean isPrintSymbiont();

    @Override
    public final boolean isRemoteSymbiont() {
        return false;
    }

    @Override
    public abstract void initialize() throws ExecStoppedException;

    @Override
    public abstract void lockDevice();

    @Override
    public abstract void reposition(int count) throws ExecStoppedException;

    @Override
    public abstract void repositionAll() throws ExecStoppedException;

    @Override
    public abstract void requeue();

    @Override
    public abstract void setPageGeometry(final Integer linesPerPage,
                                         final Integer topMargin,
                                         final Integer bottomMargin,
                                         final Integer linesPerInch);

    @Override
    public abstract void suspend();

    @Override
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
            msg = String.format("%s IO error - %s: AE", getSymbiontName(), detail);
            responses = new String[]{"A", "E"};
        } else {
            msg = String.format("%s IO error - %s: E", getSymbiontName(), detail);
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

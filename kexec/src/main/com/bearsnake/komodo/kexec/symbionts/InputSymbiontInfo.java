/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * Handles input symbionts - essentially card readers, virtual or otherwise.
 */
class InputSymbiontInfo extends SymbiontInfo {

    private RunCardInfo _runCardInfo = null;

    public InputSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
        _channelProgram.setFunction(ChannelProgram.Function.Read);
    }

    @Override
    void end() throws ExecStoppedException {
        // TODO
    }

    @Override
    void initiate() throws ExecStoppedException {
        // TODO
    }

    @Override
    void lock() throws ExecStoppedException {
        // TODO
    }

    @Override
    void reposition(int count) throws ExecStoppedException {
        // count must be zero, but we do not enforce that - we just ignore it.
        // TODO
    }

    @Override
    void requeue() throws ExecStoppedException {
        // TODO
    }

    @Override
    void suspend() throws ExecStoppedException {
        // TODO
    }

    @Override
    void terminate() throws ExecStoppedException {
        // TODO
    }

    /**
     * Implements state machine for InputSymbiontInfo
     * @return true if we did something useful, false if we are waiting for something.
     */
    @Override
    boolean poll() throws ExecStoppedException {
        var result = false;

        if (!_isSuspended) {
            var exec = Exec.getInstance();
            var fm = exec.getFacilitiesManager();

            if (!_node.isReady() && (_state != SymbiontDeviceState.Quiesced)) {
                // This should not happen - we are in the middle of reading a deck, and the device rudely went offline.
                // TODO
            } else {
                result = switch (_state) {
                    case Quiesced -> pollQuiesced();
                    case PreRun -> pollPreRun();
                    case Loading -> pollLoading();
                    case LoadingFile -> pollLoadingFile();
                    default -> pollDefault();
                };
            }
        }

        return result;
    }

    private boolean pollLoading() throws ExecStoppedException {
        return false; // TODO
    }

    private boolean pollLoadingFile() throws ExecStoppedException {
        return false; // TODO
    }

    /**
     * We have not yet read a @RUN card, but the IO to do so has been issued.
     * If the IO is complete, do @RUN card processing.
     */
    private boolean pollPreRun() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var ioStatus = _channelProgram.getIoStatus();
        if (ioStatus == IoStatus.InProgress) {
            return false;
        } else if (ioStatus == IoStatus.EndOfFile) {
            // TODO
            return false;
        } else if (ioStatus != IoStatus.Complete) {
            // IO error - abort the input, do NOT delete the deck, DN the device
            return false;
        } else {
            // We have an image - is it a @RUN card?
            // If so, pull the necessary information from it, create a READ$ file, and advance the state.
            var image = _buffer.toASCII();
            var split = image.split("[, ]");
            if (split[0].equalsIgnoreCase("@RUN")) {
                try {
                    _runCardInfo = RunCardInfo.parse(exec, image);
                    // TODO all the following should be in a separate function setupREAD$():
                    //  generate effective Run-Id
                    //  create and assign READ$ file
                    //  set up SymbiontFileWriter
                    //  write @RUN image to SymbiontFileWriter
                    _state = SymbiontDeviceState.Loading;
                    return true;
                } catch (Parser.SyntaxException ex) {
                    var msg = _node.getNodeName() + " Invalid @RUN card";
                    exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
                    resetNode();
                    return false;
                }
            }

            // First card is not a @RUN card - if it is a legal comment card, ignore it.
            if (image.equals("@") || image.startsWith("@ ")) {
                return true;
            }

            // Deck is apparently missing the @RUN card.
            var msg = _node.getNodeName() + " Missing @RUN card";
            exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
            resetNode();

            msg = _node.getNodeName() + " INACTIVE";
            exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
            _state = SymbiontDeviceState.Quiesced;
            return false;
        }
    }

    /**
     * The symbiont was not active, but we've found it to be ready.
     * Start an IO to read a run-card and advance the state.
     */
    private boolean pollQuiesced() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        var msg = _node.getNodeName() + " ACTIVE";
        exec.sendExecReadOnlyMessage(msg);

        var failed = false;
        var retry = true;
        while (retry) {
            try {
                fm.routeIo(_channelProgram);
                _state = SymbiontDeviceState.PreRun;
                retry = false;
            } catch (NoRouteForIOException ex) {
                retry = notifyConsoleIOError(NO_ROUTE_FOR_IO_MSG, true);
                failed = true;
            }
        }

        if (failed) {
            resetNode();
            msg = _node.getNodeName() + " INACTIVE";
            exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
            _state = SymbiontDeviceState.Quiesced;
        }

        return !failed;
    }
}

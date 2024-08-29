/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

/**
 * Handles input symbionts - essentially card readers, virtual or otherwise.
 */
class ReaderSymbiontInfo extends SymbiontInfo {

    private static enum MicroState {
        None,
    }

//    private RunCardInfo _runCardInfo = null;
//    private FileSpecification _fileSpecification = null;
//    private SymbiontFileWriter _fileWriter = null;
    private int _imageCount = 0;
    private MicroState _microState = MicroState.None;
//    private Run _run = null;

    public ReaderSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
        _channelProgram.setFunction(ChannelProgram.Function.Read);
    }

//    /**
//     * SM symbiont E keyin
//     */
//    @Override
//    void end() throws ExecStoppedException {
//        // TODO
//    }

    @Override
    public String getStateString() {
        return String.format("%s %s,%s,CARDS READ = %d", _node.getNodeName(), _status, _state, _imageCount);
    }

//    /**
//     * SM symbiont I keyin
//     */
//    @Override
//    void initiate() throws ExecStoppedException {
//        // TODO
//    }

//    /**
//     * SM symbiont L keyin
//     */
//    @Override
//    void lock() throws ExecStoppedException {
//        // TODO
//    }

//    /**
//     * SM symbiont Rnnn or R+nnn or RALL keyin
//     */
//    @Override
//    void reposition(int count) throws ExecStoppedException {
//        // count must be zero, but we do not enforce that - we just ignore it.
//        // TODO
//    }

//    /**
//     * SM symbiont R keyin
//     */
//    @Override
//    void requeue() throws ExecStoppedException {
//        // TODO
//    }

//    /**
//     * SM symbiont S keyin
//     */
//    @Override
//    void suspend() throws ExecStoppedException {
//        // TODO
//    }

//    /**
//     * SM symbiont T keyin
//     */
//    @Override
//    void terminate() throws ExecStoppedException {
//        // TODO
//    }

    /**
     * Implements state machine for InputSymbiontInfo.
     * Invoked by some higher-level entity (we do not specify what that is, here) on a periodic basis.
     * Our job is to advance the reader state machine in some fashion which makes sense.
     * @return true if we did something useful, false if we are waiting for something.
     */
    @Override
    boolean poll() throws ExecStoppedException {
        var exec = Exec.getInstance();

        switch (_status) {
            case Active -> {
                // We are currently reading input - continue to do so
                doRead();
                return true;
            }

            case Inactive -> {
                if (_node.isReady()) {
                    // If the symbiont device is ready, it means there are images ready to be read.
                    _imageCount = 0;
//                    _runCardInfo = null;
//                    _run = null;
                    _status = SymbiontStatus.Active;
                    _state = SymbiontState.Reading;
                    exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
                    return true;
                }
            }

            case Locked -> {
                // If we are currently reading input, continue to do so
                if (_state == SymbiontState.Reading) {
                    doRead();
                    return true;
                }
            }

            case Suspended -> {
                // Do nothing here
            }

            case Waiting -> {
                // And how is this different from inactive? I don't know...
            }
        }

        return false;
    }

//    /**
//     * Something went wrong, possibly anywhere in this process.
//     * We've already alerted the console, so just abandon the process.
//     */
//    private void cancelInput() throws ExecStoppedException {
//        var exec = Exec.getInstance();
//        var fm = exec.getFacilitiesManager();
//
//        resetNode();
//        exec.unregisterRun(_run.getActualRunId());
//        _run = null;
//        _fileWriter = null;
//        _runCardInfo = null;
//
//        var fsResult = new FacStatusResult();
//        fm.releaseFile(exec,
//                       _fileSpecification,
//                       FacilitiesManager.ReleaseBehavior.Normal,
//                       true,
//                       true,
//                       false,
//                       false,
//                       fsResult);
//
//        _state = SymbiontState.Quiesced;
//    }

    /**
     * Reads an image from the associated reader device and dispatches it appropriately.
     * Handles all possible situations for the caller, including any necessary changes to state/status.
     */
    private void doRead() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();

        var retry = true;
        while (retry) {
            var failed = false;
            try {
                fm.routeIo(_channelProgram);
                while (_channelProgram.getIoStatus() == IoStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }

                switch (_channelProgram.getIoStatus()) {
                    case EndOfFile -> {
                        System.out.println("<< EOF >>");// TODO remove
                        // TODO handle EOF
                        _status = SymbiontStatus.Inactive;
                        _state = SymbiontState.Stopped;
                        exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
                    }

                    case Complete -> {
                        _imageCount++;
                        // TODO handle image
                        _controlWord.getBuffer().dump();// TODO remove
                    }

                    default -> failed = true;
                }
            } catch (NoRouteForIOException ex) {
                failed = true;
            }

            if (failed) {
                retry = notifyConsoleIOError(NO_ROUTE_FOR_IO_MSG, true);
            } else {
                break;
            }
        }
    }

//    /**
//     * We have a READ$ file set up for writing - all we need to do is read images from
//     * the input symbiont, and write them to the READ$ file.
//     */
//    private boolean pollLoading() throws ExecStoppedException {
//        var ioStatus = _channelProgram.getIoStatus();
//        if (ioStatus == IoStatus.InProgress) {
//            return false;
//        } else if (ioStatus == IoStatus.EndOfFile) {
//            // TODO need to check for @FILE and do the appropriate stuffs
//            wrapREADFile();
//            return true;
//        } else {
//            var image = _buffer.toASCII();
//            try {
//                _fileWriter.writeDataImage(image);
//            } catch (ExecIOException ex) {
//                cancelInput();
//                return false;
//            }
//            return initiateRead();
//        }
//    }

//    private boolean pollLoadingFile() throws ExecStoppedException {
//        return false; // TODO
//    }

//    /**
//     * We have not yet read a @RUN card, but the IO to do so has been issued.
//     * If the IO is complete, do @RUN card processing.
//     */
//    private boolean pollPreRun() throws ExecStoppedException {
//        var exec = Exec.getInstance();
//        var ioStatus = _channelProgram.getIoStatus();
//        if (ioStatus == IoStatus.InProgress) {
//            return false;
//        } else if (ioStatus == IoStatus.EndOfFile) {
//            // There was no run card - send missing run card message to console
//            //  TODO
//            return false;
//        } else if (ioStatus != IoStatus.Complete) {
//            // IO error - abort the input, do NOT delete the deck, DN the device
//            return false;
//        } else {
//            // We have an image - is it a @RUN card?
//            // If so, pull the necessary information from it, create a READ$ file, and advance the state.
//            var image = _buffer.toASCII();
//            var split = image.split("[, ]");
//            if (split[0].equalsIgnoreCase("@RUN")) {
//                try {
//                    _runCardInfo = RunCardInfo.parse(exec, image);
//                    return setupREADFile(image);
//                } catch (Parser.SyntaxException ex) {
//                    var msg = _node.getNodeName() + " Invalid @RUN card";
//                    exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
//                    resetNode();
//                    return false;
//                }
//            }
//
//            // First card is not a @RUN card - if it is a legal comment card, ignore it.
//            if (image.equals("@") || image.startsWith("@ ")) {
//                return true;
//            }
//
//            // Deck is apparently missing the @RUN card.
//            var msg = _node.getNodeName() + " Missing @RUN card";
//            exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
//            resetNode();
//
//            msg = _node.getNodeName() + " INACTIVE";
//            exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
//            _state = SymbiontState.Quiesced;
//            return false;
//        }
//    }

//    /**
//     * The symbiont was not active, but we've found it to be ready.
//     * Start an IO to read a run-card and advance the state.
//     */
//    private boolean pollQuiesced() throws ExecStoppedException {
//        var exec = Exec.getInstance();
//        var msg = _node.getNodeName() + " ACTIVE";
//        exec.sendExecReadOnlyMessage(msg);
//
//        return initiateRead();
//    }

//    private boolean setupREADFile(
//        final String runImage
//    ) throws ExecStoppedException {
//        // Create BatchRun entity
//        var exec = Exec.getInstance();
//        var cfg = exec.getConfiguration();
//        var fm = exec.getFacilitiesManager();
//        _run = exec.createBatchRun(_runCardInfo);
//
//        // Create the READ$ file and assign it to the exec (for now).
//        var filename = "READ$X" + _run.getActualRunId();
//        _fileSpecification = new FileSpecification("SYS$", filename, null, null, null);
//        var fsResult = new FacStatusResult();
//        fm.catalogDiskFile(_fileSpecification,
//                           cfg.getStringValue(Tag.MDFALT),
//                           _run.getProjectId(),
//                           _run.getAccountId(),
//                           false,
//                           false,
//                           true,
//                           false,
//                           false,
//                           false,
//                           Granularity.Track,
//                           1,
//                           999,
//                           null,
//                           fsResult);
//        if (fsResult.hasErrorMessages()) {
//            var msg = "Cannot create READ$ file for run " + _run.getActualRunId();
//            exec.sendExecReadOnlyMessage(msg, ConsoleType.System);
//            cancelInput();
//            return false;
//        }
//
//        fsResult = new FacStatusResult();
//        fm.assignCatalogedDiskFileToExec(_fileSpecification, true, fsResult);
//        if (fsResult.hasErrorMessages()) {
//            var msg = "Cannot assign READ$ file for run " + _run.getActualRunId();
//            exec.sendExecReadOnlyMessage(msg, ConsoleType.System);
//            cancelInput();
//            return false;
//        }
//
//        // Set up SymbiontFileWriter for READ$ file.
//        // Yes, I know the READ$ file needs a SymbiontFileReader, but that only happens when the run
//        // comes out of backlog.  First we have to write to the stupid thing.
//        try {
//            _fileWriter = new SymbiontFileWriter(filename,
//                                                 SDFFileType.READ$,
//                                                 01,
//                                                 (int)(long)cfg.getIntegerValue(Tag.SYMFBUF));
//            _fileWriter.writeREAD$LabelControlImage(01,
//                                                    filename,
//                                                    _node.getNodeName(),
//                                                    _runCardInfo.getRunId(),
//                                                    Instant.now());
//            _fileWriter.writeDataImage(runImage);
//            _state = SymbiontState.Loading;
//        } catch (ExecIOException ex) {
//            cancelInput();
//            return false;
//        }
//        return initiateRead();
//    }

//    /**
//     * Write EOF to the READ$ file, release it, and queue up the job.
//     */
//    private void wrapREADFile() throws ExecStoppedException {
//        var exec = Exec.getInstance();
//        var fm = exec.getFacilitiesManager();
//
//        try {
//            _fileWriter.writeEndOfFileControlImage();
//        } catch (ExecIOException ex) {
//            cancelInput();
//        }
//
//        var fsResult = new FacStatusResult();
//        fm.releaseFile(exec,
//                       _fileSpecification,
//                       FacilitiesManager.ReleaseBehavior.Normal,
//                       false,
//                       false,
//                       false,
//                       false,
//                       fsResult);
//
//        _fileWriter = null;
//        _fileSpecification = null;
//        resetNode();
//
//        // TODO Invoke GenFileInterface to create an input queue item and put it in backlog.
//
//        _state = SymbiontState.Quiesced;
//    }
}

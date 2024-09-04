/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.channels.ChannelIoPacket;
import com.bearsnake.komodo.hardwarelib.channels.TransferFormat;
import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.SDFFileType;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exceptions.ExecIOException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.Run;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

import java.time.Instant;
import java.util.LinkedList;

/**
 * Handles input symbionts' state machine - one instance per device.
 * This is for anything which acts as a card reader, virtual or otherwise.
 */
class ReaderSymbiontInfo extends SymbiontInfo {

    private final ChannelIoPacket _channelPacket;
    private SymbiontFileWriter _fileWriter = null;
    private int _imageCount = 0;
    private Run _run = null;

    public ReaderSymbiontInfo(
        final NodeInfo nodeInfo
    ) {
        super(nodeInfo);
        _channelPacket = new ChannelIoPacket();
        _channelPacket.setBuffer(new ArraySlice(new long[33]))
                      .setFormat(TransferFormat.QuarterWord)
                      .setIoFunction(IoFunction.Read)
                      .setNodeIdentifier(nodeInfo.getNode().getNodeIdentifier());
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

    /**
     * SM symbiont I keyin
     */
    @Override
    public void initialize() throws ExecStoppedException {
        if (_state == SymbiontState.Reading) {
            _status = SymbiontStatus.Active;
        } else {
            _status = SymbiontStatus.Inactive;
            _state = SymbiontState.Stopped;
        }
    }

    /**
     * SM symbiont L keyin
     */
    @Override
    public void lock() throws ExecStoppedException {
        _status = SymbiontStatus.Locked;
    }

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
                    _run = null;
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
            var failMessage = "";
            try {
                fm.routeIo(_channelPacket);
                switch (_channelPacket.getIoStatus()) {
                    case EndOfFile -> {
                        if (_run != null) {
                            try {
                                _fileWriter.writeEndOfFileControlImage();
                                _run.setIsReady();
                                _fileWriter = null;
                                _run = null;
                            } catch (ExecIOException e) {
                                _run.setInvalidRunReason("CANNOT WRITE TO READ$ FILE");
                                _run.setIsReady();
                                _run = null;
                                _fileWriter = null;
                                resetNode();
                            }
                        }
                        _status = SymbiontStatus.Inactive;
                        _state = SymbiontState.Stopped;
                        exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
                    }

                    case Successful -> {
                        _imageCount++;
                        var image = Word36.toStringFromASCII(_channelPacket.getBuffer());
                        handleImage(image);
                    }

                    default -> {
                        failed = true;
                        failMessage = _channelPacket.getIoStatus().toString();
                    }
                }
            } catch (NoRouteForIOException ex) {
                failed = true;
                failMessage = NO_ROUTE_FOR_IO_MSG;
            }

            if (failed) {
                retry = notifyConsoleIOError(failMessage, true);
            } else {
                break;
            }
        }
    }

    /**
     * Handles an image read from the image reader device.
     */
    void handleImage(final String image) throws ExecStoppedException {
        var exec = Exec.getInstance();
        if (_run == null) {
            if (!image.toUpperCase().startsWith("@RUN")) {
                var msg = _node.getNodeName() + " Missing or Invalid @RUN card";
                exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
                resetNode();
                _status = SymbiontStatus.Inactive;
                _state = SymbiontState.Stopped;
                exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
                return;
            }

            try {
                var runCardInfo = RunCardInfo.parse(exec, image);
                if (!setupREADFile(runCardInfo)) {
                    resetNode();
                }
            } catch (Parser.SyntaxException ex) {
                var msg = _node.getNodeName() + " Invalid @RUN card";
                exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
                resetNode();
                _status = SymbiontStatus.Inactive;
                _state = SymbiontState.Stopped;
                exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
            }
        } else {
            // We are building a run.
            if (image.toUpperCase().startsWith("@FILE")) {
                // TODO handle this
            }

            try {
                _fileWriter.writeDataImage(image);
            } catch (ExecIOException ex) {
                _run.setInvalidRunReason("CANNOT WRITE TO READ$ FILE");
                _run.setIsReady();
                _run = null;
                _fileWriter = null;
                resetNode();
            }
        }
    }

    /**
     * Creates a batch run for the given run card.
     * @return true if things go well, and we can continue building the run - in which case run will be a reference to the
     * BatchRun we are building. Otherwise, we return false indicating that the run cannot proceed. The run will be marked invalid
     * so that it can be brought out of backlog and shot down. The scanner can go ahead and drop the rest of the input.
     * @param runCardInfo represents the @RUN image which precipitated this
     */
    private boolean setupREADFile(
        final RunCardInfo runCardInfo
    ) throws ExecStoppedException {
        // Create BatchRun entity
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        var fm = exec.getFacilitiesManager();
        var run = exec.createBatchRun(runCardInfo);

        // Create the READ$ file and assign it to the exec (for now).
        var filename = "READ$X" + run.getActualRunId();
        var fileSpecification = new FileSpecification("SYS$", filename, null, null, null);
        var fsResult = new FacStatusResult();
        fm.catalogDiskFile(fileSpecification,
                           cfg.getStringValue(Tag.MDFALT),
                           run.getProjectId(),
                           run.getAccountId(),
                           false,
                           false,
                           true,
                           false,
                           false,
                           false,
                           Granularity.Track,
                           1,
                           999,
                           new LinkedList<>(),
                           fsResult);
        if ((fsResult.getStatusWord() & 0_400000_000000L) != 0) {
            run.setInvalidRunReason(String.format("FAC STATUS %012o CREATING READ$ FILE", fsResult.getStatusWord()));
            run.setIsReady();
            return false;
        }

        fsResult = new FacStatusResult();
        fm.assignCatalogedDiskFileToExec(fileSpecification, true, fsResult);
        if (fsResult.hasErrorMessages()) {
            run.setInvalidRunReason(String.format("FAC STATUS %012o ASSIGNING READ$ FILE", fsResult.getStatusWord()));
            run.setIsReady();
            return false;
        }

        // Set up SymbiontFileWriter for READ$ file.
        // Yes, I know the READ$ file needs a SymbiontFileReader, but that only happens when the run
        // comes out of backlog.  First we have to write the stupid thing.
        SymbiontFileWriter fileWriter = null;
        try {
            fileWriter = new SymbiontFileWriter(filename,
                                                SDFFileType.READ$,
                                                01,
                                                (int)(long)cfg.getIntegerValue(Tag.SYMFBUF));
            fileWriter.writeREAD$LabelControlImage(01,
                                                   filename,
                                                   _node.getNodeName(),
                                                   runCardInfo.getRunId(),
                                                   Instant.now());
            fileWriter.writeDataImage(runCardInfo.getImage());
        } catch (ExecIOException ex) {
            run.setInvalidRunReason("CANNOT WRITE TO READ$ FILE");
            run.setIsReady();
            return false;
        }

        _run = run;
        _fileWriter = fileWriter;
        return true;
    }
}

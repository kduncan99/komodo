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
import com.bearsnake.komodo.kexec.exceptions.ScheduleManagerException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.CatalogDiskFileRequest;
import com.bearsnake.komodo.kexec.scheduleManager.BatchRun;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;

import java.time.Instant;

/**
 * Handles input symbionts' state machine - one instance per device.
 * This is for anything which acts as a card reader, virtual or otherwise.
 * Functionality is constrained to reading images and writing them to a temporary READ$ file.
 */
class ReaderSymbiontInfo extends OnSiteSymbiontInfo {

    private final ChannelIoPacket _channelPacket;
    private SymbiontFileWriter _fileWriter = null;
    private int _imageCount = 0;
    private BatchRun _run = null;

    private boolean _pendingLocked = false;     // status is not yet locked, but it will be
    private boolean _skipToRunCard = false;     // implies _run is null

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

    @Override
    public String getStateString() {
        return String.format("%s %s,%s,CARDS READ = %d", _node.getNodeName(), _status, _state, _imageCount);
    }

    @Override
    public boolean isInputSymbiont() {
        return true;
    }

    @Override
    public boolean isOutputSymbiont() {
        return false;
    }

    @Override
    public boolean isPrintSymbiont() {
        return false;
    }

    // -------------------------------------------------------------------------

    /**
     * Handles SM * I (which we don't, actually)
     */
    @Override
    public synchronized final void initialize() {
        if (_status == SymbiontStatus.Locked) {
            _status = SymbiontStatus.Inactive;
            _state = SymbiontState.Stopped;
        } else if (_status == SymbiontStatus.Suspended) {
            _status = SymbiontStatus.Active;
            _state = SymbiontState.Reading;
        }

        _pendingLocked = false;
    }

    /**
     * Handles SM * L
     * Locks the symbiont right away, if possible.
     * Otherwise, it sets the pending lock flag, which will be observed and
     * acted upon in due course by the poll() routine.
     */
    @Override
    public synchronized final void lockDevice() {
        if (_status == SymbiontStatus.Inactive) {
            _status = SymbiontStatus.Locked;
            _state = SymbiontState.Stopped;
        } else {
            _pendingLocked = true;
        }
    }

    /**
     * Handles SM * R[nnn]
     * For Reader, this cancels the job we're working on (if any) and skips anything remaining in the input
     * until we hit the next @RUN card (if any). The count is ignored.
     * @param count number of cards or pages (must be zero for input symbiont)
     */
    @Override
    public synchronized final void reposition(int count) throws ExecStoppedException {
        abortRun();
    }

    /**
     * Handles SM * RALL
     * The effects are identical to reposition().
     */
    @Override
    public synchronized final void repositionAll() throws ExecStoppedException {
        abortRun();
    }

    /**
     * For SM * Q... but it has no applicability for card readers.
     */
    @Override
    public synchronized final void requeue() {
        // Ignore this... SM keyin should never invoke it
    }

    /**
     * For SM * C... but it has no applicability for card readers.
     */
    @Override
    public final void setPageGeometry(final Integer linesPerPage,
                                      final Integer topMargin,
                                      final Integer bottomMargin,
                                      final Integer linesPerInch) {
        // Ignore this... SM keyin should never invoke it
    }

    /**
     * For SM * S
     */
    @Override
    public synchronized final void suspend() {
        _status = SymbiontStatus.Suspended;
    }

    /**
     * For SM * E or SM * T
     * discards the current run (if any) and also discards the remainder of the card deck
     * (aka the input file on the reader).
     * For SM * T, caller should invoke lockDevice() first, then this.
     */
    @Override
    public synchronized void terminateFile() throws ExecStoppedException {
        var exec = Exec.getInstance();

        abortRun();
        resetNode();

        if (_status != SymbiontStatus.Inactive) {
            _status = SymbiontStatus.Inactive;
            _state = SymbiontState.Stopped;
            exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
        }
    }

    private void abortRun() throws ExecStoppedException {
        // abort the run we are currently building, and set skipToRun.
        var exec = Exec.getInstance();
        var sch = exec.getScheduleManager();

        // Close the symbiont file we're writing for the job
        if (_fileWriter != null) {
            try {
                _fileWriter.close();
            } catch (ExecIOException e) {
                // Don't do anything - there's really nothing we *can* do
            }
            _fileWriter = null;
        }

        // Close out the run and remove it
        if (_run != null) {
            sch.unregisterRun(_run.getActualRunId());
            _run = null;
        }

        _skipToRunCard = true;
    }

    // -------------------------------------------------------------------------

    /**
     * Implements state machine for ReaderSymbiontInfo.
     * Invoked by some higher-level entity (we do not specify what that is, here) on a periodic basis.
     * Our job is to advance the reader state machine in some fashion which makes sense.
     * @return true if we did something useful, false if we are waiting for something.
     */
    @Override
    synchronized boolean poll() throws ExecStoppedException {
        var exec = Exec.getInstance();

        switch (_status) {
            case Active -> {
                // We are currently reading input - continue to do so unless we are commanded to abort
                doRead();
                return true;
            }

            case Inactive -> {
                if (_node.isReady()) {
                    // If the symbiont device is ready, it means there are images ready to be read.
                    _imageCount = 0;
                    _run = null;
                    _skipToRunCard = true;
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
        var sch = exec.getScheduleManager();

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
                                _fileWriter.close();
                                _run.clearHoldCondition(BatchRun.HoldCondition.FinStatementHold);
                                _fileWriter = null;
                                _run = null;
                            } catch (ExecIOException e) {
                                sch.unregisterRun(_run.getActualRunId());
                                _run = null;
                                _fileWriter = null;
                                resetNode();
                            }
                        }

                        _status = _pendingLocked ? SymbiontStatus.Locked : SymbiontStatus.Inactive;
                        _state = SymbiontState.Stopped;
                        exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
                    }

                    case Successful -> {
                        _imageCount++;
                        var image = Word36.toStringFromASCII(new ArraySlice(_channelPacket.getBuffer(),
                                                                            0,
                                                                            _channelPacket.getActualWordCount()));
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
                if (_skipToRunCard) {
                    return;
                }

                var msg = _node.getNodeName() + " Missing or Invalid @RUN card";
                exec.sendExecReadOnlyMessage(msg, ConsoleType.InputOutput);
                resetNode();
                _status = SymbiontStatus.Inactive;
                _state = SymbiontState.Stopped;
                exec.sendExecReadOnlyMessage(getStateString(), ConsoleType.InputOutput);
                return;
            }

            _skipToRunCard = false;

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
            var imUpper = image.toUpperCase();
            var imPartial = imUpper.split(" ")[0];
            if (imPartial.equals("@FILE")) {
                // TODO handle this for @FILE
                //  this is messy - we need to open an output SDF file and copy images from here to there
                //  until we hit @ENDF, so that is extra things to track, extra things to clean up,
                //  and one or more extra states to handle... but maybe we should do it anyway?
            }

            try {
                _fileWriter.writeDataImage(image);
                if (imPartial.equals("@FIN")) {
                    // Close out symbiont file we're creating, release the run,
                    // and skip to next @RUN statement in the device input (if there is one).
                    _fileWriter.close();
                    _fileWriter = null;
                    _run.clearHoldCondition(BatchRun.HoldCondition.FinStatementHold);
                    _skipToRunCard = true;
                }
            } catch (ExecIOException ex) {
                exec.getScheduleManager().unregisterRun(_run.getActualRunId());
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
        var sch = exec.getScheduleManager();
        BatchRun run;
        try {
            run = sch.createBatchRun(runCardInfo);
        } catch (ScheduleManagerException ex) {
            exec.sendExecReadOnlyMessage(ex.getMessage(), ConsoleType.InputOutput);
            return false;
        }

        // Create the READ$ file and assign it to the exec (for now).
        var filename = "READ$X" + run.getActualRunId();
        var fileSpecification = new FileSpecification("SYS$", filename, null, null, null);
        var fsResult = new FacStatusResult();
        var req = new CatalogDiskFileRequest(fileSpecification).setMnemonic(cfg.getStringValue(Tag.MDFALT))
                                                               .setProjectId(run.getProjectId())
                                                               .setAccountId(run.getAccountId())
                                                               .setIsUnloadInhibited()
                                                               .setGranularity(Granularity.Track)
                                                               .setInitialGranules(1).setMaximumGranules(999);
        fm.catalogDiskFile(req, fsResult);
        if ((fsResult.getStatusWord() & 0_400000_000000L) != 0) {
            sch.unregisterRun(run.getActualRunId());
            return false;
        }

        fsResult = new FacStatusResult();
        fm.assignCatalogedDiskFileToExec(fileSpecification, true, fsResult);
        if (fsResult.hasErrorMessages()) {
            sch.unregisterRun(run.getActualRunId());
            return false;
        }

        // Set up SymbiontFileWriter for READ$ file, and write the @RUN image thereto.
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
            sch.unregisterRun(run.getActualRunId());
            return false;
        }

        _run = run;
        _fileWriter = fileWriter;
        return true;
    }
}

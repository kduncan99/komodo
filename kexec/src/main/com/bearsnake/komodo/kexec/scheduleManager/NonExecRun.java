/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.baselib.FileCycleSpecification;
import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.AssignCatalogedDiskFileRequest;
import com.bearsnake.komodo.kexec.facilities.CatalogDiskFileRequest;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.FacilitiesManager;
import com.bearsnake.komodo.kexec.facilities.facItems.DiskFileFacilitiesItem;
import com.bearsnake.komodo.kexec.mfd.DiskFileCycleInfo;
import com.bearsnake.komodo.kexec.symbionts.SymbiontFileReader;
import com.bearsnake.komodo.kexec.symbionts.SymbiontFileWriter;

public abstract class NonExecRun extends Run {

    protected SymbiontFileWriter _currentPrintSymbiont = null;
    protected SymbiontFileWriter _currentPunchSymbiont = null;
    protected SymbiontFileReader _currentReadSymbiont = null;
    protected boolean _deletePrintPart = false;
    protected String _directPrintPartTo = null;
    protected int _nextPrintPartNumber = 0;
    protected int _nextPunchPartNumber = 0;

    // Sector address in GENF$ of the input queue item for this job if it isn't open yet
    // Will be set when the input queue item is created; will be set to zero once the input queue item is removed.
    protected int _inputQueueAddress;

    // True only if we have come out of backlog for batch, or are DEMAND, and have subsequently finished.
    // We *might* have queue items, but if we don't, we will very quickly be removed from ScheduleManager.
    protected boolean _isFinished = false;

    // False if we never left backlog; true if we are running, or we have finished.
    // Always true for DEMAND.
    // Usually there are queue items, but this might not be true for jobs which have *just* finished with no
    // queue items and have not yet been removed from ScheduleManager, or for jobs which *did* have queue items
    // but those queue items have just been removed, and we have not yet been removed from ScheduleManager.
    protected boolean _isStarted = false;

    // Indicates whether this run is currently suspended
    protected boolean _isSuspended = false;

    // Indicates the symbiont of site-id which produced the run.
    // null if started from the console or if this is a TIP run.
    protected String _siteId;

    // Thread which is running this runnable, if we are started
    protected Thread _thread;

    protected NonExecRun(
        final RunType runType,
        final String actualRunId,
        final RunCardInfo runCardInfo
    ) {
        super(runType, actualRunId, runCardInfo);

        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        if (_runCardInfo.getMaxCards() == null) {
            _runCardInfo.setMaxCards(cfg.getIntegerValue(Tag.MAXCRD));
        }
        if (_runCardInfo.getMaxPages() == null) {
            _runCardInfo.setMaxPages(cfg.getIntegerValue(Tag.MAXPAG));
        }
        if (_runCardInfo.getMaxTime() == null) {
            _runCardInfo.setMaxTime(cfg.getIntegerValue(Tag.MAXTIM));
        }

        _currentCardLimit = _runCardInfo.getMaxCards();
        _currentPageLimit = _runCardInfo.getMaxPages();
        _currentTimeLimit = _runCardInfo.getMaxTime();
        _defaultQualifier = _runCardInfo.getProjectId();
        _impliedQualifier = _runCardInfo.getProjectId();
    }

    /**
     * Creates a new output symbiont print file, and applies the PRINT$ internal file name.
     * If there is already a PRINT$ file in use, it is closed and possibly queued.
     * Caller must ensure the next part number value is <= 0511 before invoking.
     * Invoked in the following circumstances...
     *      When a batch run starts
     *      Upon the first attempt to write output to PRINT$ for a tip run
     *      During @BRKPT processing for batch or tip (demand uses RSI redirection)
     * @return true if successful; false means the run fails with FAC ERROR.
     */
    protected synchronized FacStatusResult assignPRINT$File() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();

        var fileName = String.format("%s@PR%03o", getActualRunId(), _nextPrintPartNumber);
        var fileSpec = new FileSpecification("SYS$", fileName, FileCycleSpecification.newRelativeSpecification(1), null, null);
        var fsResult = new FacStatusResult();
        var catReq = new CatalogDiskFileRequest(fileSpec).setMnemonic("F")
                                                         .setProjectId(exec.getProjectId())
                                                         .setAccountId(exec.getAccountId())
                                                         .setIsGuarded()
                                                         .setIsPrivate()
                                                         .setIsUnloadInhibited()
                                                         .setGranularity(Granularity.Track)
                                                         .setInitialGranules(1)
                                                         .setMaximumGranules(9999);
        var ok = fm.catalogDiskFile(catReq, fsResult);
        if (ok) {
            var asgReq = new AssignCatalogedDiskFileRequest(fileSpec).setOptionsWord(Word36.A_OPTION | Word36.X_OPTION).setExclusiveUse().setDoNotHoldRun();
            ok = fm.assignCatalogedDiskFileToRun(this, asgReq, fsResult);
        }

        if (ok) {
            // TODO check for existing PRINT$, maybe release it, and maybe queue it if it exists
            //  release it if we assigned it during @BRKPT processing or if it is a PR@ file.
            //  queue it if it is a PR@ file and we have not done a @SYM,U
            if (_deletePrintPart) {

            }

            // Now apply PRINT$ internal file name
            fm.establishUseItem(this, "PRINT$", fileSpec, false);
            ++_nextPrintPartNumber;
        }

        return fsResult;
    }

    /**
     * Creates a new output symbiont punch file, and applies the PUNCH$ internal file name.
     * If there is already a PUNCH$ file in use, it is closed and possibly queued.
     * Invoked in the following circumstances...
     *      Upon the first attempt to write output to PUNCH$
     *      During @BRKPT processing
     * @return true if successful; false means the run fails with FAC ERROR.
     */
    protected synchronized boolean assignPUNCH$File() {
        return false;//TODO
    }

    /**
     * Looks up the GENF entry for this run, and attempts to assign the input file,
     * and applies the READ$ internal use name to that file.
     * If successful, a SymbiontReader is created for the input file.
     * Invoked upon batch run startup (demand uses RSI redirection)
     * @return FacStatusResult describing the result of the request - only the status word is guaranteed to be populated
     * @throws ExecStoppedException if something goes badly wrong
     */
    protected FacStatusResult assignREAD$File() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        var genf = exec.getGenFileInterface();

        var fsResult = genf.assignInputFile(this, _inputQueueAddress);
        if ((fsResult.getStatusWord() & 0_400000_000000L) != 0) {
            var msg = String.format("READ$FILE REJECT STATUS %06o", fsResult.getStatusWord());
            postReadOnlyConsoleMessage(msg);
            _facErrorFlag = true;
            return fsResult;
        }

        var facItem = (DiskFileFacilitiesItem) _facilitiesItemTable.getFacilitiesItemByInternalName("READ$");
        var fcInfo = (DiskFileCycleInfo) facItem.getAcceleratedCycleInfo().getFileCycleInfo();
        var sectorCount = fcInfo.getHighestTrackWritten() + 1;
        var bufferSize = (int)(long)cfg.getIntegerValue(Tag.SYMFBUF);
        _currentReadSymbiont = new SymbiontFileReader("READ$", sectorCount, bufferSize);

        return fsResult;
    }

    public final void clearInputQueueAddress() { _inputQueueAddress = 0; }
    @Override public String getAccountId() { return _runCardInfo.getAccountId(); }
    public final long getInputQueueAddress() { return _inputQueueAddress; }
    public final String getSiteId() { return _siteId; }
    @Override public final boolean isFinished() { return _isFinished; }
    @Override public final boolean isStarted() { return _isStarted; }
    @Override public final boolean isSuspended() { return _isSuspended; }

    @Override
    public boolean isPrivileged() {
        // check SYS$*DLOC$
        var facItem = _facilitiesItemTable.getFacilitiesItem("SYS$", "DLOC$");
        if (facItem != null) {
            var dfi = (DiskFileFacilitiesItem)facItem;
            if (dfi.isReadable() && dfi.isWriteable()) {
                return true;
            }
        }

        // Is the run privileged by way of Security?
        // That's a tricky one, there are various dimensions of privilege. This applies only to general all-encompassing privilege.
        // TODO
        return false;
    }

    protected void postReadOnlyConsoleMessage(final String message) {
        var msg = String.format("%-6s %s", getActualRunId(), message.substring(0, 72));
        Exec.getInstance().sendExecReadOnlyMessage(msg, ConsoleType.System);
    }

    @Override
    public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode
    ) {
        // TODO
    }

    @Override
    public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode,
        final long auxiliary
    ) {
        // TODO
    }

    @Override
    public void postToPrint(
        final String text,
        final int lineSkip
    ) {
        // TODO
    }

    @Override
    public void postToPunch(
        final String text
    ) {
        // TODO
    }

    @Override
    public void postToTailSheet(
        final String message
    ) {
        // TODO
    }

}

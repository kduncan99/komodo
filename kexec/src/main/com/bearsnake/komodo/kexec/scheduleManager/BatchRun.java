/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;

import java.util.HashSet;
import java.util.Set;

public class BatchRun extends Run implements Runnable {

    public static enum HoldCondition {
        ArbitraryDeviceWait(7, "AD"),
        CatalogedFileWait(5, "CT"),
        FinStatementHold(0, "F"),
        MassStorageAvailability(4, "MS"),
        IndividualHold(1, "O"),
        OtherFacilityHold(8, "OH"),
        SOptionHold(2, "S"),
        StartTimePending(3, "ST"),
        TapeHold(6, "TP");

        private final String _code;
        private final int _priority; // lower value is higher priority

        HoldCondition(final int priority, final String code) {
            _priority = priority;
            _code = code;
        }

        public String getCode() { return _code; }
        public int getPriority() { return _priority; }
    }

    protected final Set<HoldCondition> _holdConditions = new HashSet<>();

    // Sector address in GENF$ of the input queue item for this job if it isn't open yet
    // Will be set when the input queue item is created; will be set to zero once the input queue item is removed.
    protected long _inputQueueAddress;

    // True only if we have come out of backlog, and have subsequently finished.
    // We *might* have queue items, but if we don't, we will very quickly be removed from ScheduleManager.
    protected boolean _isFinished = false;

    // False if we never left backlog; true if we are running, or we have finished.
    // Usually there are queue items, but this might not be true for jobs which have *just* finished with no
    // queue items and have not yet been removed from ScheduleManager, or for jobs which *did* have queue items
    // but those queue items have just been removed and we have not yet been removed from ScheduleManager.
    protected boolean _isStarted = false;

    // Indicates whether this run is currently suspended
    protected boolean _isSuspended = false;

    // Indicates the symbiont of site-id which produced the run. null if started from the console.
    protected String _siteId;

    // Runstream is still being read, have not yet reached @FIN
    protected boolean _waitingOnFin = true;

    public BatchRun(final String actualRunId,
                    final RunCardInfo runCardInfo) {
        super(RunType.Batch, actualRunId, runCardInfo);
        _inputQueueAddress = 0;
    }

    public final void clearHoldCondition(final HoldCondition condition) { _holdConditions.remove(condition); }
    public final void clearInputQueueAddress() { _inputQueueAddress = 0; }
    public final long getInputQueueAddress() { return _inputQueueAddress; }
    public final String getSiteId() { return _siteId; }
    @Override public final boolean isFinished() { return _isFinished; }
    public final boolean isHeld() { return !_holdConditions.isEmpty(); }
    public final boolean isHeldFor(final HoldCondition condition) { return _holdConditions.contains(condition); }
    @Override public final boolean isStarted() { return _isStarted; }
    @Override public final boolean isSuspended() { return _isSuspended; }

    @Override public BatchRun setDefaultQualifier(final String qualifier) { super.setDefaultQualifier(qualifier); return this; }
    public BatchRun setHoldCondition(final HoldCondition condition) { _holdConditions.add(condition); return this; }
    @Override public BatchRun setImpliedQualifier(final String qualifier) { super.setImpliedQualifier(qualifier); return this; }
    public BatchRun setInputQueueAddress(final long address) { _inputQueueAddress = address; return this; }
    public BatchRun setIsFinished(final boolean isFinished) { _isFinished = isFinished; return this; }
    public BatchRun setIsStarted(final boolean isStarted) { _isStarted = isStarted; return this; }
    @Override public BatchRun setProcessorPriority(final Character priority) { super.setProcessorPriority(priority); return this; }
    @Override public BatchRun setSchedulingPriority(final Character priority) { super.setSchedulingPriority(priority); return this; }
    public BatchRun setSiteId(final String siteId) { _siteId = siteId; return this; }

    public HoldCondition getHighestPriorityHoldCondition() {
        HoldCondition result = null;
        for (var condition : _holdConditions) {
            if (result == null || (condition.getPriority() < result.getPriority())) {
                result = condition;
            }
        }
        return result;
    }

    /**
     * To be invoked when the run comes out of backlog.
     * The first image is a @RUN card, but it has already been processed by the exec (else we would not exist).
     * So, we just start reading images (ignoring the initial @RUN card other than printing it to PRINT$) and processing them.
     */
    @Override
    public void run() {
        // TODO
        //IMPROPER RUNSTREAM IN FILE
        //The first image in the file or element specified on the @START statement is an invalid @RUN statement.
        //MAX CARDS
        //The estimated card output has been exceeded. A system generation parameter or the C option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX PAGES
        //The estimated page output has been exceeded. A system generation parameter or the P option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX TIME
        //The estimated running time has been exceeded. A system generation parameter or the T option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX VOLUNTARY DELAY TIME
        //This message appears on the tail sheet of any batch run that exceeds its quota of voluntary delay time. This message appears only if the run has a quota abort.
        //MISSING ACCOUNT
        //The account is required, but it is not specified on the @RUN statement. If you submitted the run from a demand terminal, you can enter another @RUN statement. Batch runs are terminated.
        //MISSING USER-ID
        //The user-id is required, but it is not specified on the @RUN statement. If the run originated from a demand device, another @RUN statement can be entered. Batch runs are terminated.
        //OPERATOR REMOVED RUN
        //The operator removed the run from the backlog by using the RM keyin. A possible solution to eliminate receiving this error is to have valid user-id, account number, and characters in your run-id.
        //OPERATOR TERMINATED RUN BY AN E-KEYIN
        //The operator replied with an E to terminate the run, or a demand user entered an @@X T.
        //OPERATOR TERMINATED RUN BY AN X-KEYIN
        //The operator entered an X keyin to terminate the run.
        //OPERATOR TERMINATED RUN KILLED VIA AN E-KEYIN
        //The operator entered an E keyin for this run. When used for a demand run, an E keyin terminates the task that is currently executing.
        //OPERATOR TERMINATED RUN KILLED VIA AN X-KEYIN
        //The operator entered an X keyin for this run. When used for a demand run, an X keyin terminates the task that is currently executing.
        //READ$FILE REJECT STATUS xxxxxx
        //The READ$ file cannot be assigned.
        //*RUN ALREADY ACTIVE*
        //An @RUN statement was received while in demand run mode.
        //  *RUN-ID NOT ACTIVE*
        //The run-id on an @@TM is not currently active.
        //  RUN REMOVED DUE TO INVALID USER-ID/PASSWORD
        //Either the user-id or the password on the @PASSWD statement is illegal.
        //  RUN REMOVED DUE TO SECURITY ERROR
        //The user’s run has been removed due to an illegal password, or a password was not found in the runstream.
        //  RUNSTREAM ANALYSIS TERMINATED
        //The run has been terminated because of an error condition, and the remaining control statements are not processed.
    }
}

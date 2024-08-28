/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.DateConverter;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Run;

import java.io.PrintStream;
import java.time.Instant;

/*
 * Describes one output queue item - corresponding to a print or punch entity.
 *
 * Output queue item sector format
 *   +000,S1    Type (02)
 *   +000,S2    Priority Index
 *   +000,T2    GENF$ recovery cycle (at time of sym)
 *   +000,T3    Absolute F-Cycle
 *   +001       Actual Run-id FD LJSF
 *   +002:003   Account-id FD LJSF
 *   +004:005   Project-id FD LJSF
 *   +006:007   User-id FD LJSF
 *   +010       Queue-id FD LJSF
 *   +011       Output-id FD LJSF
 *   +012:013   Use-name FD LJSF
 *   +014:015   Qualifier FD LJSF
 *   +016:017   Filename FD LJSF
 *   +020:021   Banner FD LJSF
 *   +022       Time-sym'd ModSW
 *   +023       Fac status bits (of last fac operation on this entry)
 *   +024,H1    Fac message code 1
 *   +024,H2    Fac message code 2
 *   +025,H1    Sector address of first part-name entry (for tape files)
 *   +025,H2    Sector address of initial SMOQUE entry (could be this one)
 *   +026       Estimated pages or cards
 *   +027       Flags
 *   +030,H1    Breakpoint Part Number
 *   +030,H2    Number of times file is queued (if this is the initial entry)
 *   +031:033   reserved
 *
 * Flags:
 *   000001  Tape File
 *   000002  Multi-tape file
 *   000004  All files on tape are to be printed/punched
 *   000010  Error occurred while processing this entry
 *   000200  File could not be assigned while processing this entry
 *   000400  Print file
 *   001000  Punch file
 *   002000  In-progress
 *   004000  SV is set for this entry
 *   010000  Queued to a user-id
 *   020000  Label-print-output set when sym'd
 *   040000  Every-page-labeling set when sym'd
 *   100000  Removable disk file
 * Priority Index Values:
 *     Index        File Priority   Run Priority
 *       1                0         ROLOUT and ROLBAK runs
 *       2                1         Critical deadline runs
 *       3                A         A, B, C
 *       4                D         D, E, F
 *       5                G         G, H, I
 *       6                J         J, K, L
 *       7                M         M, N, O
 *       8                P         P, Q, R
 *       9                S         S, T, U
 *      10                V         V, W, X
 *      11                Y         Y, Z
 */
public class OutputQueueItem extends Item {

    private int _priorityIndex;                   // See above
    private final int _genfRecoveryCycle;         // GENF recovery cycle at the time this entry was created
    private String _qualifier;
    private String _filename;
    private int _absoluteCycle;
    private String _runId;                        // run-id of the run which enqueued the item
    private String _accountId;                    // account-id of the run which enqueued the item
    private String _projectId;                    // project-id of run which enqueued the item
    private String _userId;                       // user-id of run which enqueued the item
    private String _queueId;                      // Name of the output queue which contains this queue item
    private String _outputId;                     // If output is in progress, this is the device-id, or the run-id of the run doing the output.
    private String _useName;                      // If a run has assigned the file, this is the @use-name attached to the file.
    private String _banner;                       // Banner to be used on the banner page (1 to 12 characters)
    private Instant _symTimestamp;                // date/time this entry was enqueued
    private long _facilityStatusBits;             // Fac status bits associated with most recent action on this entry
    private final int[] _facilityMessageCodes;    // Two fac status codes associated with most recent action on this entry
    private int _partNameSectorAddress;           // GENF$ address of entry containing tape part-name data for this entry
    private int _initialEntrySectorAddress;       // address of initial smoque entry in chain containing this entry
    private long _estimatedPagesOrCards;
    private long _flags;                          // See above
    private int _breakpointPartNumber;            // If this was a PRINT$ or PUNCH$ file, this is the part number
    private int _numberOfTimesQueued;             // Only for first entry in chain, contains number of entries in chain

    public OutputQueueItem(
        final long sectorAddress,
        final int genfRecoveryCycle
    ) {
        super(ItemType.OutputQueueItem, sectorAddress);
        _genfRecoveryCycle = genfRecoveryCycle;
        _symTimestamp = Instant.now();
        _facilityMessageCodes = new int[2];
    }

    public static OutputQueueItem deserialize(
        final long sectorAddress,
        final ArraySlice source
    ) {
        var genfRecCycle = (int) source.getT2(0);
        var item = new OutputQueueItem(sectorAddress, genfRecCycle);

        item._priorityIndex = (int) source.getS2(0);
        item._absoluteCycle = (int) source.getT3(0);
        item._runId = Word36.toStringFromFieldata(source, 1, 1);
        item._accountId = Word36.toStringFromFieldata(source, 2, 2);
        item._projectId = Word36.toStringFromFieldata(source, 4, 2);
        item._userId = Word36.toStringFromFieldata(source, 6, 2);
        item._queueId = Word36.toStringFromFieldata(source, 010, 1);
        item._outputId = Word36.toStringFromFieldata(source, 011, 1);
        item._useName = Word36.toStringFromFieldata(source, 012, 2);
        item._qualifier = Word36.toStringFromFieldata(source, 014, 2);
        item._filename = Word36.toStringFromFieldata(source, 016, 2);
        item._banner = Word36.toStringFromFieldata(source, 020, 2);
        item._symTimestamp = DateConverter.fromModifiedSingleWordTime(source.get(022));
        item._facilityStatusBits = source.get(023);
        item._facilityMessageCodes[0] = (int) source.getH1(024);
        item._facilityMessageCodes[1] = (int) source.getH2(024);
        item._partNameSectorAddress = (int) source.getH1(025);
        item._initialEntrySectorAddress = (int) source.getH2(025);
        item._estimatedPagesOrCards = source.get(026);
        item._flags = source.get(027);
        item._breakpointPartNumber = (int) source.getH1(030);
        item._numberOfTimesQueued = (int) source.getH2(030);

        return item;
    }

    public final int getAbsoluteCycle() { return _absoluteCycle; }
    public final String getAccountId() { return _accountId; }
    public final String getBanner() { return _banner; }
    public final int getBreakpointPartNumber() { return _breakpointPartNumber; }
    public final long getEstimatedPagesOrCards() { return _estimatedPagesOrCards; }
    public final int[] getFacilityMessageCodes() { return _facilityMessageCodes; }
    public final long getFacilityStatusBits() { return _facilityStatusBits; }
    public final String getFilename() { return _filename; }
    public final long getFlags() { return _flags; }
    public final int getInitialEntrySectorAddress() { return _initialEntrySectorAddress; }
    public final int getGenfRecoveryCycle() { return _genfRecoveryCycle; }
    public final int getNumberOfTimesQueued() { return _numberOfTimesQueued; }
    public final String getOutputId() { return _outputId; }
    public final int getPartNameSectorAddress() { return _partNameSectorAddress; }
    public final int getPriorityIndex() { return _priorityIndex; }
    public final String getProjectId() { return _projectId; }
    public final String getQualifier() { return _qualifier; }
    public final String getQueueId() { return _queueId; }
    public final String getRunId() { return _runId; }
    public final Instant getSymTimestamp() { return _symTimestamp; }
    public final String getUseName() { return _useName; }
    public final String getUserId() { return _userId; }

    public OutputQueueItem setBanner(String banner) { _banner = banner; return this; }

    public OutputQueueItem setEstimatedCards(final long cards) { _estimatedPagesOrCards = cards; return this; }
    public OutputQueueItem setEstimatedPages(final long pages) { _estimatedPagesOrCards = pages; return this; }

    public OutputQueueItem setFacilityStatus(
        final long facilityStatusBits,
        final int facilityCode1,
        final int facilityCode2
    ) {
        _facilityStatusBits = facilityStatusBits;
        _facilityMessageCodes[0] = facilityCode1;
        _facilityMessageCodes[1] = facilityCode2;

        return this;
    }

    public OutputQueueItem setFileSpecificationInfo(
        final FileSpecification fileSpecification,
        final int breakpointPartNumber
    ) {
        _qualifier = fileSpecification.getQualifier();
        _filename = fileSpecification.getFilename();
        _absoluteCycle = fileSpecification.getFileCycleSpecification().getCycle();
        _breakpointPartNumber = breakpointPartNumber;

        return this;
    }

    public OutputQueueItem setFlags(final long flags) { _flags = flags; return this; }
    public OutputQueueItem setInitialEntrySectorAddress(final int address) { _initialEntrySectorAddress = address; return this; }
    public OutputQueueItem setNumberOfTimesQueued(final int timesQueued) { _numberOfTimesQueued = timesQueued; return this; }
    public OutputQueueItem setOutputId(final String outputId) { _outputId = outputId; return this; }
    public OutputQueueItem setPartNameSectorAddress(final int address) { _partNameSectorAddress = address; return this; }
    public OutputQueueItem setPriorityIndex(final int priorityIndex) { _priorityIndex = priorityIndex; return this; }
    public OutputQueueItem setQueueId(final String queueId) { _queueId = queueId; return this; }

    public OutputQueueItem setRunInfo(final Run run) {
        _runId = run.getActualRunId();
        _accountId = run.getAccountId();
        _projectId = run.getProjectId();
        _userId = run.getUserId();
        return this;
    }

    public OutputQueueItem setUseName(final String useName) { _useName = useName; return this; }

    @Override
    public void dump(PrintStream out, String indent) {
        super.dump(out, indent);
        // TODO
    }

    @Override
    public void serialize(final ArraySlice destination) throws ExecStoppedException {
        super.serialize(destination);

        destination.setS2(0, _priorityIndex);
        destination.setT2(0, _genfRecoveryCycle);
        destination.setT3(0, _absoluteCycle);
        destination.set(01, Word36.stringToWordFieldata(_runId));
        Word36.stringToWordsFieldata(_accountId, destination, 02, 2);
        Word36.stringToWordsFieldata(_projectId, destination, 04, 2);
        Word36.stringToWordsFieldata(_userId, destination, 06, 2);
        destination.set(010, Word36.stringToWordFieldata(_queueId));
        destination.set(011, Word36.stringToWordFieldata(_outputId));
        Word36.stringToWordsFieldata(_useName, destination, 012, 2);
        Word36.stringToWordsFieldata(_qualifier, destination, 014, 2);
        Word36.stringToWordsFieldata(_filename, destination, 016, 2);
        Word36.stringToWordsFieldata(_banner, destination, 020, 2);
        destination.set(022, DateConverter.getModifiedSingleWordTime(_symTimestamp));
        destination.set(023, _facilityStatusBits);
        destination.setH1(024, _facilityMessageCodes[0]);
        destination.setH2(024, _facilityMessageCodes[1]);
        destination.setH1(025, _partNameSectorAddress);
        destination.setH2(025, _initialEntrySectorAddress);
        destination.set(026, _estimatedPagesOrCards);
        destination.set(027, _flags);
        destination.setH1(030, _breakpointPartNumber);
        destination.setH2(030, _numberOfTimesQueued);
    }
}

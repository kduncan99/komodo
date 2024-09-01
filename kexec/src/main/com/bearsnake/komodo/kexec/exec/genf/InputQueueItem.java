/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.DateConverter;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.io.PrintStream;
import java.time.Instant;

/*
 * Input queue item sector format
 *   +000,S1    Type (01)
 *   +000,S2    Scheduling Priority fieldata 'A' through 'Z'
 *   +000,S3    Processing Priority fieldata 'A' through 'Z'
 *   +001       @RUN statement options
 *   +002       Actual Run-id FD LJSF
 *   +003       Original Run-id FD LJSF
 *   +004:005   Account-id FD LJSF
 *   +006:007   Project-id FD LJSF
 *   +010:011   User-id FD LJSF
 *   +012       Start-time ModSW
 *   +013       Deadline-time ModSW
 *   +014       Submission-time ModSW
 *   +015,H1    Max Pages
 *   +015,H2    Max Cards
 *   +016,H1    Max Time
 *   +017       Source Symbiont FD LJSF
 *   +020:033   reserved
 */
public class InputQueueItem extends Item {

    private final String _actualRunId;
    private Character _processingPriority;
    private final RunCardInfo _runCardInfo;
    private Character _schedulingPriority;
    private final String _sourceSymbiontName;
    private final Instant _submissionTime;

    public InputQueueItem(
        final int sectorAddress,
        final String sourceSymbiontName,
        final String actualRunId,
        final RunCardInfo runCardInfo,
        final Instant submissionTime
    ) {
        super(ItemType.InputQueueItem, sectorAddress);
        _sourceSymbiontName = sourceSymbiontName;
        _actualRunId = actualRunId;
        _runCardInfo = runCardInfo;
        _submissionTime = submissionTime;
    }

    public String getActualRunId() { return _actualRunId; }
    public Character getProcessingPriority() { return _processingPriority; }
    public RunCardInfo getRunCardInfo() { return _runCardInfo; }
    public char getSchedulingPriority() { return _schedulingPriority; }
    public Instant getSubmissionTime() { return _submissionTime; }
    public String getSourceSymbiontName() { return _sourceSymbiontName; }

    public InputQueueItem setProcessingPriority(final Character processingPriority) { _processingPriority = processingPriority; return this; }
    public InputQueueItem setSchedulingPriority(final Character schedulingPriority) { _schedulingPriority = schedulingPriority; return this; }

    public static InputQueueItem deserialize(
        final int sectorAddress,
        final ArraySlice source
    ) {
        var runCardInfo = new RunCardInfo("");
        runCardInfo.setSchedulingPriority((char) source.getS2(0));
        runCardInfo.setProcessorPriority((char) source.getS3(0));
        runCardInfo.setOptionWord(source.get(01));
        runCardInfo.setRunId(Word36.toStringFromFieldata(source.get(03)).trim());
        runCardInfo.setAccountId((Word36.toStringFromFieldata(source.get(04)) + Word36.toStringFromFieldata(source.get(05))).trim());
        runCardInfo.setProjectId((Word36.toStringFromFieldata(source.get(06)) + Word36.toStringFromFieldata(source.get(07))).trim());
        runCardInfo.setUserId((Word36.toStringFromFieldata(source.get(010)) + Word36.toStringFromFieldata(source.get(011))).trim());
        runCardInfo.setStartTime(DateConverter.fromSingleWordTime(source.get(012)));
        runCardInfo.setDeadlineTime(DateConverter.fromSingleWordTime(source.get(013)));
        runCardInfo.setMaxPages(source.getH1(015));
        runCardInfo.setMaxCards(source.getH2(015));
        runCardInfo.setMaxTime(source.getH1(016));
        return new InputQueueItem(
            sectorAddress,
            Word36.toStringFromFieldata(source.get(017)),
            Word36.toStringFromASCII(source.get(02)),
            runCardInfo,
            DateConverter.fromSingleWordTime(source.get(014)));
    }

    @Override
    public void dump(PrintStream out, String indent) {
        super.dump(out, indent);
        // TODO
    }

    @Override
    public void serialize(final ArraySlice destination) throws ExecStoppedException {
        super.serialize(destination);
        destination.setS2(0, _runCardInfo.getSchedulingPriority());
        destination.setS3(0, _runCardInfo.getProcessorPriority());
        destination.set(01, _runCardInfo.getOptionWord());
        destination.set(02, Word36.stringToWordFieldata(_actualRunId));
        destination.set(03, Word36.stringToWordFieldata(_runCardInfo.getRunId()));
        destination.set(04, Word36.stringToWordFieldata(_runCardInfo.getAccountId().substring(0, 6)));
        destination.set(05, Word36.stringToWordFieldata(_runCardInfo.getAccountId().substring(6)));
        destination.set(06, Word36.stringToWordFieldata(_runCardInfo.getProjectId().substring(0, 6)));
        destination.set(07, Word36.stringToWordFieldata(_runCardInfo.getProjectId().substring(6)));
        destination.set(010, Word36.stringToWordFieldata(_runCardInfo.getUserId().substring(0, 6)));
        destination.set(011, Word36.stringToWordFieldata(_runCardInfo.getUserId().substring(6)));
        destination.set(012, DateConverter.getModifiedSingleWordTime(_runCardInfo.getStartTime()));
        destination.set(013, DateConverter.getModifiedSingleWordTime(_runCardInfo.getDeadlineTime()));
        destination.set(014, DateConverter.getModifiedSingleWordTime(_submissionTime));
        destination.setH1(015, _runCardInfo.getMaxPages());
        destination.setH2(015, _runCardInfo.getMaxCards());
        destination.setH1(016, _runCardInfo.getMaxTime());
        destination.set(017, Word36.stringToWordFieldata(_sourceSymbiontName));
    }
}

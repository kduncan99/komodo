/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.DateConverter;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;

import java.time.Instant;
import java.util.stream.IntStream;

/**
 * Manages the GENF$ file and manages the backlog and the output queues.
 * GENF$ is read at boot time, and all information is kept in memory, with GENF$ being rewritten as necessary.
 * ---
 * Information is stored on sector boundaries. Sector types identify the type of sector, and are stored
 * in Word+0, S1 of each sector. The values include:
 * Type Description
 *  00   unused items
 *  01   input queue items (backlog)
 *  02   output queue items (for SMOQUE)
 *  03   system information items (persist information across system boots)
 * ---
 * Input queue item format
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
 *   +017:033   reserved
 * ---
 * Output queue item format
 */

public class GenFileInterface {

    private enum SectorType {
        Unused,
        InputQueue,
        OutputQueue,
        System,
    }

    private static class InputQueueItem {

        public String _sourceSymbiontName;
        public String _actualRunId;
        public RunCardInfo _runCardInfo;
        public Instant _submissionTime;

        public InputQueueItem(
            final String sourceSymbiontName,
            final String actualRunId,
            final RunCardInfo runCardInfo,
            final Instant submissionTime
        ) {
            _sourceSymbiontName = sourceSymbiontName;
            _actualRunId = actualRunId;
            _runCardInfo = runCardInfo;
            _submissionTime = submissionTime;
        }

        public static InputQueueItem deserialize(final ArraySlice source) {
            var runCardInfo = new RunCardInfo();
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
                Word36.toStringFromFieldata(source.get(017)),
                Word36.toStringFromASCII(source.get(02)),
                runCardInfo,
                DateConverter.fromSingleWordTime(source.get(014)));
        }

        public void serialize(final ArraySlice destination) {
            IntStream.range(0, 033).forEach(wx -> destination.set(wx, 0));
            destination.setS1(0, SectorType.InputQueue.ordinal());
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

    private static class OutputQueueItem {

        public OutputQueueItem(
        ) {
            // TODO
        }

        public static OutputQueueItem deserialize(final ArraySlice source) {
            // TODO
            return new OutputQueueItem();
        }

        public void serialize(final ArraySlice destination) {
            IntStream.range(0, 033).forEach(wx -> destination.set(wx, 0));
            destination.setS1(0, SectorType.OutputQueue.ordinal());
            // TODO
        }
    }

    public GenFileInterface() {
        // TODO
    }

    /**
     * Initializes the GENF$ file - used during JK13 and JK9 boots
     */
    public void initialize() {
        // TODO
    }

    /**
     * Recovers the GENF$ file - used during regular recovery boots
     */
    public void recover() {
        // TODO
    }
}

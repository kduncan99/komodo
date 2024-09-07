/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import java.io.PrintStream;
import java.util.HashMap;

import static com.bearsnake.komodo.baselib.Parser.isValidAccountId;
import static com.bearsnake.komodo.baselib.Parser.isValidProjectId;
import static com.bearsnake.komodo.baselib.Parser.isValidRunid;
import static com.bearsnake.komodo.baselib.Parser.isValidUserId;

/**
 * A rough analog to the Coarse Scheduler.
 */
public class ScheduleManager implements Manager {

    private static final String DEFAULT_RUN_ID = "RUN000";
    private static final String DEFAULT_ACCOUNT_ID = "000000";
    private static final String DEFAULT_PROJECT_ID = "Q$Q$Q$";

    private int _maxBatchJobs = 0;
    private final HashMap<String, Run> _runEntries = new HashMap<>(); // keyed by RunId

    public ScheduleManager() {
        Exec.getInstance().managerRegister(this);
    }

    @Override
    public void boot(boolean recoveryBoot) throws KExecException {
        var exec = Exec.getInstance();
        _runEntries.clear();
        _runEntries.put(exec.getActualRunId(), exec);
        _maxBatchJobs = (int)(long)(exec.getConfiguration().getIntegerValue(Tag.MAXOPN));
    }

    @Override
    public void close() {

    }

    @Override
    public void dump(PrintStream out, String indent, boolean verbose) {
        out.printf("%sScheduleManager ********************************\n", indent);
        out.printf("%s  Run control entries:\n", indent);
        _runEntries.values().forEach(rce -> rce.dump(out, indent + "  ", verbose));
    }

    @Override
    public void initialize() throws KExecException {

    }

    @Override
    public void stop() {

    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Atomic process to determine a unique run-id, then create the BatchRun entity,
     * then store it in the runEntries table. Mostly exists to avoid run-id race conditions.
     */
    public synchronized Run createBatchRun(
        final RunCardInfo runCardInfo
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();

        var invalidRunId = false;
        if (runCardInfo.getRunId() == null) {
            runCardInfo.setRunId(DEFAULT_RUN_ID);
        } else if (!isValidRunid(runCardInfo.getRunId())) {
            runCardInfo.setRunId(DEFAULT_RUN_ID);
            invalidRunId = true;
        }
        var actualRunId = createUniqueRunid(runCardInfo.getRunId());
        if (!actualRunId.equals(runCardInfo.getRunId())) {
            var msg = String.format("%s Duplicated; New ID is %s", runCardInfo.getRunId(), actualRunId);
            exec.sendExecReadOnlyMessage(msg, ConsoleType.System);
        }

        var invalidAccountId = false;
        if (runCardInfo.getAccountId() == null) {
            runCardInfo.setAccountId(DEFAULT_ACCOUNT_ID);
        } else if (!isValidAccountId(runCardInfo.getAccountId())) {
            runCardInfo.setAccountId(DEFAULT_ACCOUNT_ID);
            invalidAccountId = true;
        }

        var invalidUserId = (runCardInfo.getUserId() == null) || !isValidUserId(runCardInfo.getUserId());

        var invalidProjectId = false;
        if (runCardInfo.getProjectId() == null) {
            runCardInfo.setProjectId(DEFAULT_PROJECT_ID);
        } else if (!isValidProjectId(runCardInfo.getProjectId())) {
            runCardInfo.setProjectId(DEFAULT_PROJECT_ID);
            invalidProjectId = true;
        }

        var run = new BatchRun(actualRunId, runCardInfo);
        if (invalidRunId) {
            run.setInvalidRunReason("Invalid or Missing Run ID");
        } else if (invalidAccountId) {
            run.setInvalidRunReason("Invalid or Missing Account ID");
        } else if (invalidProjectId) {
            run.setInvalidRunReason("Invalid or Missing Project ID");
        } else if (invalidUserId) {
            run.setInvalidRunReason("Invalid or Missing User ID");
        }

        _runEntries.put(actualRunId, run);
        return run;
    }

    public int getMaxBatchJobs() { return _maxBatchJobs; }

    public void setMaxBatchJobs(final int maxBatchJobs) { _maxBatchJobs = maxBatchJobs; }

    /**
     * Unregisters a run - should be done only when there are no artifacts in the input or output queues
     * for the run (and the run itself is no longer active).
     * @param runid generated run-id of the run
     */
    public void unregisterRun(final String runid) {
        _runEntries.remove(runid.toUpperCase());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * For createUniqueRunid, below
     */
    static String rotateString(final String input) {
        var bytes = input.getBytes();
        int ix = bytes.length - 1;
        while (ix >= 0) {
            if (bytes[ix] == '9') {
                bytes[ix] = 'A';
                ix--;
            } else if (bytes[ix] == 'Z') {
                bytes[ix] = '0';
                return new String(bytes);
            } else {
                bytes[ix]++;
                return new String(bytes);
            }
        }
        return null;
    }

    /**
     * Given an original runid (which should be uppercase already, but we don't trust that)
     * we generate a new unique runid so that we never have duplicated runids.
     * Because we force uppercase, the caller should *always* use the result of this algorithm for the actual runid of a run.
     * This needs to be invoked under synchronization during the process of adding a RunEntry, thus it is internal to the class.
     * The result should be as follows:
     *   The original run-id, if less than six characters, will have an 'A' appended to it.
     *   Subsequent iterations will increment that appended character through 'Z', then through '0' to '9'.
     *   The next iteration will increment the last character of the *original* run-id, and the appended characters will
     *   then re-cycle. This will continue, incrementing each subsequent character to the left, until we have reached all
     *   possible iterations of n+1 characters, where n is the length of the original run-id.
     * Thus...
     *   SYS -> SYSA -> SYSB ... SYS9 -> SYTA -> SYTB ... SYUA -> SYUB ... SZAA ... ZZZZ
     * Once we reach Z...Z, iteration stops and we reject the run-id. Longer run-ids have more room for iteration.
     * A run-id which begins with 6 characters will begin iterating with the last character of the run-id.
     * @param runid proposed (original) runid
     * @return unique runid
     */
    String createUniqueRunid(
        final String runid
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();
        var original = runid.toUpperCase();
        synchronized (_runEntries) {
            if (!_runEntries.containsKey(original)) {
                return original;
            }

            var proposed = original.length() == 6 ? original : original + "A";
            while (_runEntries.containsKey(proposed)) {
                proposed = rotateString(proposed);
                if (proposed == null) {
                    exec.stop(StopCode.FullCycleReachedForRunIds);
                    throw new ExecStoppedException();
                }
            }
            return proposed;
        }
    }
}

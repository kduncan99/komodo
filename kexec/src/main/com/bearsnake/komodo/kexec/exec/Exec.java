/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.configuration.Configuration;
import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.consoles.ConsoleManager;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.consoles.ReadOnlyMessage;
import com.bearsnake.komodo.kexec.consoles.ReadReplyMessage;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.genf.GenFileInterface;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.FacilitiesManager;
import com.bearsnake.komodo.kexec.keyins.KeyinManager;
import com.bearsnake.komodo.kexec.mfd.MFDManager;
import com.bearsnake.komodo.kexec.scheduleManager.Run;
import com.bearsnake.komodo.kexec.scheduleManager.RunType;
import com.bearsnake.komodo.kexec.scheduleManager.ScheduleManager;
import com.bearsnake.komodo.kexec.symbionts.SymbiontManager;
import com.bearsnake.komodo.kexec.tasks.ExecTask;
import com.bearsnake.komodo.logger.LogManager;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

public class Exec extends Run {

    public static final int INVALID_LDAT = 0_400000;
    public static final int EXEC_VERSION = 1;
    public static final int EXEC_RELEASE = 1;
    public static final String EXEC_PATCH = "";
    public static final String VERSION_STRING = String.format("%dR%d%s", EXEC_VERSION, EXEC_RELEASE, EXEC_PATCH);

    private static final DateTimeFormatter DATE_TIME_MSG_FORMAT = DateTimeFormatter.ofPattern("EEE dd MMM yyyy HH:mm:ss");
    private static final String LOG_SOURCE = "Exec";

    private static Exec _instance = null;

    private boolean _allowRecoveryBoot;
    private Configuration _configuration;
    private final boolean[] _jumpKeys;
    private final LinkedList<Manager> _managers = new LinkedList<>();
    private Phase _phase;
    private int _session = 0;
    private StopCode _stopCode;
    private boolean _stopFlag = false;

    private final GenFileInterface _genFileInterface = new GenFileInterface();

    private ConsoleManager _consoleManager;
    private FacilitiesManager _facilitiesManager;
    private KeyinManager _keyinManager;
    private MFDManager _mfdManager;
    private ScheduleManager _scheduleManager;
    private SymbiontManager _symbiontManager;

    private static final RunCardInfo RUN_CARD_INFO =
        new RunCardInfo("").setRunId("EXEC-8").setAccountId("SYSTEM").setProjectId("SYS$").setUserId("EXEC8");

    public Exec(final boolean[] jumpKeyTable) {
        super(RunType.Exec, "EXEC-8", RUN_CARD_INFO);

        _jumpKeys = jumpKeyTable;
        _allowRecoveryBoot = false;
        _phase = Phase.NotStarted;
        _instance = this;

        _activeTask = new ExecTask();

        _consoleManager = new ConsoleManager();
        _facilitiesManager = new FacilitiesManager();
        _keyinManager = new KeyinManager();
        _mfdManager = new MFDManager();
        _scheduleManager = new ScheduleManager();
        _symbiontManager = new SymbiontManager();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Run interface
    // -----------------------------------------------------------------------------------------------------------------

    @Override public String getAccountId() { return _configuration.getStringValue(Tag.MSTRACC); }
    @Override public final boolean isFinished() { return false; }
    @Override public final boolean isPrivileged() { return true; }
    @Override public final boolean isStarted() { return true; }
    @Override public final boolean isSuspended() { return false; }

    // -----------------------------------------------------------------------------------------------------------------
    // public
    // -----------------------------------------------------------------------------------------------------------------

    public static Exec getInstance() { return _instance; }

    public Configuration getConfiguration() { return _configuration; }
    public ConsoleManager getConsoleManager() { return _consoleManager; }
    public FacilitiesManager getFacilitiesManager() { return _facilitiesManager; }
    public KeyinManager getKeyinManager() { return _keyinManager; }
    public MFDManager getMFDManager() { return _mfdManager; }
    public ScheduleManager getScheduleManager() { return _scheduleManager; }
    public SymbiontManager getSymbiontManager() { return _symbiontManager; }

    public GenFileInterface getGenFileInterface() { return _genFileInterface; }
    public Phase getPhase() { return _phase; }
    public StopCode getStopCode() { return _stopCode; }
    public boolean isJumpKeySet(final int jumpKey) { return _jumpKeys[jumpKey - 1]; }
    public boolean isRecoveryBootAllowed() { return _allowRecoveryBoot; }
    public boolean isStopped() { return _phase == Phase.Stopped; }
    public synchronized void managerRegister(final Manager manager) { _managers.add(manager); }
    public synchronized void managerUnregister(final Manager manager) { _managers.remove(manager); }

    public void setConfiguration(final Configuration config) { _configuration = config; }

    public void setJumpKeyValue(final int jumpKey, final boolean value) { _jumpKeys[jumpKey - 1] = value; }

    public void boot(final boolean recoveryBoot,
                     final int session) throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot");
        _phase = Phase.Booted;
        _stopFlag = false;
        _session = session;

        for (var m : _managers) {
            m.boot(recoveryBoot);
        }

        var msg = String.format("KEXEC Version %s Session %03o started", VERSION_STRING, _session);
        sendExecReadOnlyMessage(msg, ConsoleType.System);
        displayDateAndTime();
        displayJumpKeys(null);

        // allow the operator to modify the configuration
        if (!recoveryBoot || Exec.getInstance().isJumpKeySet(1)) {
            msg = "Modify config then enter DONE";
            var candidates = new String[]{"DONE"};
            sendExecRestrictedReadReplyMessage(msg, candidates);
        }

        if (Exec.getInstance().isJumpKeySet(13)) {
            msg = "JK13 set during boot - Continue? Y/N";
            var candidates = new String[]{"Y", "N"};
            var response = sendExecRestrictedReadReplyMessage(msg, candidates);
            if (response.equals("N")) {
                Exec.getInstance().stop(StopCode.ConsoleResponseRequiresReboot);
                throw new ExecStoppedException();
            }
        }

        // TODO there's a sequencing problem here - if we are not JK13, we need to compare number of MS packs
        //  against previous number, which is stored in GENF$, but we don't have GENF$ until much later... ?
        _facilitiesManager.startup();

        if (isJumpKeySet(9) || isJumpKeySet(13)) {
            _genFileInterface.initialize(); // TODO need exec session number and number of fixed packs
        } else {
            _genFileInterface.recover(); // TODO
            // TODO repopulate backlog and SMOQUE (is that handled by .recover()?)
        }

        _phase = Phase.Running;
        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    /**
     * Convenience wrapper
     */
    public boolean catalogDiskFileForExec(
        final String qualifier,
        final String filename,
        final String type,
        final long initialGranules,
        final long maxGranules
    ) throws ExecStoppedException {
        var fs = new FileSpecification(qualifier, filename, null, null, null);
        var fsResult = new FacStatusResult();
        var packIds = new LinkedList<String>();
        return _facilitiesManager.catalogDiskFile(fs,
                                                  type,
                                                  getProjectId(),
                                                  getAccountId(),
                                                  true,
                                                  true,
                                                  true,
                                                  false,
                                                  false,
                                                  false,
                                                  Granularity.Track,
                                                  initialGranules,
                                                  maxGranules,
                                                  packIds,
                                                  fsResult);
    }

    /**
     * Invoke before discarding the Exec, and do NOT use the Exec after invoking here.
     */
    public void close() {
        _managers.forEach(Manager::close);
        _consoleManager = null;
        _facilitiesManager = null;
        _keyinManager = null;
        _mfdManager = null;
        _scheduleManager = null;
        _symbiontManager = null;
    }

    public void displayDateAndTime() {
        var dateTime = LocalDateTime.now();
        String dtStr = dateTime.format(DATE_TIME_MSG_FORMAT);
        var msg = String.format("The current date and time is %s", dtStr);
        sendExecReadOnlyMessage(msg, ConsoleType.System);
    }

    public void displayJumpKeys(final ConsoleId source) {
        var sb = new StringBuilder();
        var anySet = false;
        for (int jk = 1; jk <= 36; ++jk) {
            if (isJumpKeySet(jk)) {
                anySet = true;
                if (sb.isEmpty()) {
                    sb.append("Jump Keys Set: ").append(jk);
                } else {
                    sb.append(",").append(jk);
                    if (sb.length() > 60) {
                        sendExecReadOnlyMessage(sb.toString(), source);
                        sb.setLength(0);
                    }
                }
            }
        }

        if (!anySet) {
            sendExecReadOnlyMessage("Jump Keys Set: <none>", source);
        } else if (!sb.isEmpty()) {
            sendExecReadOnlyMessage(sb.toString(), source);
        }
    }

    public String dump(boolean verbose) {
        var dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String dtStr = dateTime.format(formatter);
        var filename = String.format("kexec-%s.dump", dtStr);
        PrintStream out;
        try {
            out = new PrintStream(filename);
        } catch (FileNotFoundException ex) {
            LogManager.logFatal(LOG_SOURCE, "Failed to create panic dump file");
            if (isRunning()) {
                stop(StopCode.PanicDumpFailed);
            }
            return null;
        }

        out.println("Exec Dump ----------------------------------------------------");
        out.printf("  Phase:          %s\n", _phase);
        out.printf("  Stopped:        %s\n", _stopFlag);
        out.printf("  StopCode:       %s\n", _stopCode);

        var jkStr = new StringBuilder();
        for (var jkx = 0; jkx < 36; jkx++) {
            if (_jumpKeys[jkx]) {
                jkStr.append(jkx + 1).append(" ");
            }
        }
        out.printf("  JumpKeys:       %s\n", jkStr);
        out.printf("  Allow Recovery: %s\n", _allowRecoveryBoot);

        _managers.forEach(m -> m.dump(out, "  ", verbose));
        _genFileInterface.dump(out, "  ", verbose);

        out.close();
        return filename;
    }

    public void initialize() throws KExecException {
        for (var m : _managers) {
            m.initialize();
        }
    }

    public boolean isRunning() {
        return (_phase != Phase.Stopped) && (_phase != Phase.NotStarted);
    }

    public void sendExecReadOnlyMessage(
        final String message
    ) {
        var romsg = new ReadOnlyMessage(this, null, message, true);
        _consoleManager.sendReadOnlyMessage(romsg);
    }

    public void sendExecReadOnlyMessage(
        final String message,
        final ConsoleId routing
    ) {
        var romsg = new ReadOnlyMessage(this, routing, null, message, true);
        _consoleManager.sendReadOnlyMessage(romsg);
    }

    public void sendExecReadOnlyMessage(
        final String message,
        final ConsoleType consoleType
    ) {
        var romsg = new ReadOnlyMessage(this, consoleType, null, message, true);
        _consoleManager.sendReadOnlyMessage(romsg);
    }

    public String sendExecReadReplyMessage(
        final String message,
        final int maxReplyChars
    ) throws ExecStoppedException {
        return sendExecReadReplyMessage(message, maxReplyChars, null, null);
    }

    public String sendExecReadReplyMessage(
        final String message,
        final int maxReplyChars,
        final ConsoleId routing
    ) throws ExecStoppedException {
        return sendExecReadReplyMessage(message, maxReplyChars, routing, null);
    }

    public String sendExecReadReplyMessage(
        final String message,
        final int maxReplyChars,
        final ConsoleType consoleType
    ) throws ExecStoppedException {
        return sendExecReadReplyMessage(message, maxReplyChars, null, consoleType);
    }

    public String sendExecReadReplyMessage(
        final String message,
        final int maxReplyChars,
        final ConsoleId routing,
        final ConsoleType consoleType
    ) throws ExecStoppedException {
        var rrmsg = new ReadReplyMessage(this,
                                         routing,
                                         consoleType,
                                         null,
                                         message,
                                         true,
                                         false,
                                         maxReplyChars);

        _consoleManager.sendReadReplyMessage(rrmsg);
        if (rrmsg.isCanceled()) {
            LogManager.logError(LOG_SOURCE, "Console message is canceled");
            throw new ExecStoppedException();
        }

        return rrmsg.getResponse();
    }

    public String sendExecRestrictedReadReplyMessage(
        final String message,
        final String[] candidates
    ) throws ExecStoppedException {
        return sendExecRestrictedReadReplyMessage(message, candidates, null, null);
    }

    public String sendExecRestrictedReadReplyMessage(
        final String message,
        final String[] candidates,
        final ConsoleId routing
    ) throws ExecStoppedException {
        return sendExecRestrictedReadReplyMessage(message, candidates, routing, null);
    }

    public String sendExecRestrictedReadReplyMessage(
        final String message,
        final String[] candidates,
        final ConsoleType consoleType
    ) throws ExecStoppedException {
        return sendExecRestrictedReadReplyMessage(message, candidates, null, consoleType);
    }

    /**
     * this is stupid, but handy
     */
    public static void sleep(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // do nothing
        }
    }

    /**
     * All exec code should invoke this in order to stop the exec.
     * It does NOT immediately shut things down, but it will cause that to happen fairly soon,
     * as the exec thread notices and responds.
     * @param stopCode reason for stopping
     */
    public void stop(final StopCode stopCode) {
        LogManager.logTrace(LOG_SOURCE, "stop(%s)", stopCode);
        if (_phase == Phase.Stopped) {
            LogManager.logError(LOG_SOURCE, "Already stopped");
        } else {
            _stopCode = stopCode;
            _stopFlag = true;
            _phase = Phase.Stopped;
            _managers.forEach(Manager::stop);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // private
    // -----------------------------------------------------------------------------------------------------------------

    private String sendExecRestrictedReadReplyMessage(
        final String message,
        final String[] candidates,
        final ConsoleId routing,
        final ConsoleType consoleType
    ) throws ExecStoppedException {
        if (candidates.length == 0) {
            LogManager.logFatal(LOG_SOURCE, "sendExecRestrictedReadReplyMessage:Empty candidates list");
            Exec.getInstance().stop(StopCode.FacilitiesComplex);
            throw new ExecStoppedException();
        }

        int maxReplyLen = 0;
        for (var cand : candidates) {
            if (maxReplyLen < cand.length()) {
                maxReplyLen = cand.length();
            }
        }

        var rrmsg = new ReadReplyMessage(this,
                                         routing,
                                         consoleType,
                                         null,
                                         message,
                                         true,
                                         false,
                                         maxReplyLen);
        var done = false;
        while (!done) {
            if (isStopped()) {
                throw new ExecStoppedException();
            }

            _consoleManager.sendReadReplyMessage(rrmsg);
            if (rrmsg.isCanceled()) {
                LogManager.logError(LOG_SOURCE, "Console message is canceled");
                throw new ExecStoppedException();
            }

            var response = rrmsg.getResponse().toUpperCase();
            for (var cand : candidates) {
                if (cand.toUpperCase().equals(response)) {
                    done = true;
                    break;
                }
            }
        }

        return rrmsg.getResponse().toUpperCase();
    }

}

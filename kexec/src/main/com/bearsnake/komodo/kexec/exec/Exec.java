/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.Configuration;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.consoles.ConsoleManager;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.consoles.ReadOnlyMessage;
import com.bearsnake.komodo.kexec.consoles.ReadReplyMessage;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.facilities.FacilitiesManager;
import com.bearsnake.komodo.kexec.keyins.KeyinManager;
import com.bearsnake.komodo.kexec.mfd.MFDManager;
import com.bearsnake.komodo.logger.LogManager;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.IntStream;

public class Exec {

    public static final int INVALID_LDAT = 0_400000;
    public static final int EXEC_VERSION = 1;
    public static final int EXEC_RELEASE = 1;
    public static final String EXEC_PATCH = "";
    public static final String VERSION_STRING = String.format("%dR%d%s", EXEC_VERSION, EXEC_RELEASE, EXEC_PATCH);

    private static final DateTimeFormatter _dateTimeMsgFormat = DateTimeFormatter.ofPattern("EEE dd MMM yyyy HH:mm:ss");
    private static final SimpleDateFormat _dumpFileNameFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
    private static Exec _instance = null;
    private static final String LOG_SOURCE = "Exec";

    private boolean _allowRecoveryBoot;
    private Configuration _configuration;
    private final boolean[] _jumpKeys;
    private final LinkedList<Manager> _managers = new LinkedList<>();
    private Phase _phase;
    private ExecRunControlEntry _runControlEntry;
    private final HashMap<String, RunControlEntry> _runControlEntries = new HashMap<>(); // keyed by RunId
    private int _session = 0;
    private StopCode _stopCode;
    private boolean _stopFlag = false;

    private ConsoleManager _consoleManager;
    private FacilitiesManager _facilitiesManager;
    private KeyinManager _keyinManager;
    private MFDManager _mfdManager;

    public Exec(final boolean[] jumpKeyTable) {
        _jumpKeys = jumpKeyTable;
        _allowRecoveryBoot = false;
        _phase = Phase.NotStarted;
        _instance = this;

        _consoleManager = new ConsoleManager();
        _facilitiesManager = new FacilitiesManager();
        _keyinManager = new KeyinManager();
        _mfdManager = new MFDManager();
    }

    public Configuration getConfiguration() { return _configuration; }
    public ConsoleManager getConsoleManager() { return _consoleManager; }
    public FacilitiesManager getFacilitiesManager() { return _facilitiesManager; }
    public static Exec getInstance() { return _instance; }
    public MFDManager getMFDManager() { return _mfdManager; }
    public KeyinManager getKeyinManager() { return _keyinManager; }
    public Phase getPhase() { return _phase; }
    public RunControlEntry getRunControlEntry() { return _runControlEntry; }
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

        _runControlEntries.clear();
        _runControlEntry = new ExecRunControlEntry(_configuration.getMasterAccountId());
        _runControlEntries.put(_runControlEntry._runId, _runControlEntry);

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

        _facilitiesManager.startup();

        if (!isJumpKeySet(9) && !isJumpKeySet(13)) {
            // TODO populate rce's with entries from backlog and SMOQUE
            //   well, at some point. probably not here.
        }

        LogManager.logTrace(LOG_SOURCE, "boot complete");
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
    }

    public void displayDateAndTime() {
        var dateTime = LocalDateTime.now();
        String dtStr = dateTime.format(_dateTimeMsgFormat);
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

        out.println("  Run Control Entries:");
        for (var rce : _runControlEntries.values()) {
            rce.dump(out, "    ", verbose);
        }

        for (var m : _managers) {
            m.dump(out, "  ", verbose);
        }

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

    public static boolean isValidFilename(
        final String filename
    ) {
        if ((filename.isEmpty()) || (filename.length() > 12)) {
            return false;
        }
        return IntStream.range(0, filename.length()).allMatch(chx -> isValidFilenameChar(filename.charAt(chx)));
    }

    public static boolean isValidFilenameChar(
        final char ch
    ) {
        return (ch >= 'A' && ch <= 'Z') || ch == '-' || ch == '$';
    }

    public static boolean isValidNodeName(
        final String deviceName
    ) {
        if (deviceName.isEmpty() || deviceName.length() > 6) {
            return false;
        }

        for (var ch : deviceName.getBytes()) {
            if ((ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9')) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidPackName(
        final String packName
    ) {
        if (packName.isEmpty() || packName.length() > 6) {
            return false;
        }

        for (var ch : packName.getBytes()) {
            if ((ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9')) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidPrepFactor(
        final int value
    ) {
        return (value == 28)
            || (value == 56)
            || (value == 112)
            || (value == 224)
            || (value == 448)
            || (value == 896)
            || (value == 1792);
    }

    public static boolean isValidReadWriteKey(
        final String key
    ) {
        if (key.isEmpty() || key.length() > 6) {
            return false;
        }

        for (int ch : key.getBytes()) {
            // any fieldata character is allowed excepting period, comma, semicolon, slash, and blank
            if ((ch > 127) || Word36.FIELDATA_FROM_ASCII[ch] == 005) {
                return false;
            }
            if (ch == '.' || ch == ',' || ch == ';' || ch == '/') {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidQualifier(
        final String qualifier
    ) {
        if ((qualifier.isEmpty()) || (qualifier.length() > 12)) {
            return false;
        }
        return IntStream.range(0, qualifier.length()).allMatch(chx -> isValidQualifierChar(qualifier.charAt(chx)));
    }

    public static boolean isValidQualifierChar(
        final char ch
    ) {
        return (ch >= 'A' && ch <= 'Z') || ch == '-' || ch == '$';
    }

    public void sendExecReadOnlyMessage(
        final String message
    ) {
        var romsg = new ReadOnlyMessage(_runControlEntry, null, message, true);
        _consoleManager.sendReadOnlyMessage(romsg);
    }

    public void sendExecReadOnlyMessage(
        final String message,
        final ConsoleId routing
    ) {
        var romsg = new ReadOnlyMessage(_runControlEntry, routing, null, message, true);
        _consoleManager.sendReadOnlyMessage(romsg);
    }

    public void sendExecReadOnlyMessage(
        final String message,
        final ConsoleType consoleType
    ) {
        var romsg = new ReadOnlyMessage(_runControlEntry, consoleType, null, message, true);
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
        var rrmsg = new ReadReplyMessage(_runControlEntry,
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

        var rrmsg = new ReadReplyMessage(_runControlEntry,
                                         routing,
                                         consoleType,
                                         null,
                                         message,
                                         true,
                                         false,
                                         maxReplyLen);
        var done = false;
        while (!done) {
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
}

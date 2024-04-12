/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.kexec.Configuration;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.consoles.ConsoleManager;
import com.bearsnake.komodo.kexec.consoles.ReadOnlyMessage;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.facilities.FacilitiesManager;
import com.bearsnake.komodo.kexec.keyins.KeyinServices;
import com.bearsnake.komodo.logger.LogManager;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Exec {

    public static final long INVALID_LDAT = 0_400000;
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
    private ScheduledThreadPoolExecutor _executor;
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
    private KeyinServices _keyinManager;

    public Exec(final boolean[] jumpKeyTable) {
        _jumpKeys = jumpKeyTable;
        _allowRecoveryBoot = false;
        _phase = Phase.NotStarted;
        _instance = this;

        _consoleManager = new ConsoleManager();
        _facilitiesManager = new FacilitiesManager();
        _keyinManager = new KeyinServices();
    }

    public Configuration getConfiguration() { return _configuration; }
    public ConsoleManager getConsoleManager() { return _consoleManager; }
    public ScheduledThreadPoolExecutor getExecutor() { return _executor; }
    public static Exec getInstance() { return _instance; }
    public KeyinServices getKeyinManager() { return _keyinManager; }
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

        _runControlEntries.clear();
        _runControlEntry = new ExecRunControlEntry(_configuration.getMasterAccountId());
        _runControlEntries.put(_runControlEntry._runId, _runControlEntry);
        if (!isJumpKeySet(9) && !isJumpKeySet(13)) {
            // TODO populate rce's with entries from backlog and SMOQUE
            //   well, at some point. probably not here.
        }

        _executor = new ScheduledThreadPoolExecutor((int)_configuration.getExecThreadPoolSize());
        _executor.setRemoveOnCancelPolicy(true);
        for (var m : _managers) {
            m.boot(recoveryBoot);
        }

        var msg = String.format("KEXEC Version %s Session %03o started", VERSION_STRING, _session);
        sendExecReadOnlyMessage(msg, null);
        displayDateAndTime();
        displayJumpKeys(null);

        LogManager.logTrace(LOG_SOURCE, "boot complete");
    }

    public void close() {
        _executor.close();
        _executor = null;
    }

    public void displayDateAndTime() {
        var dateTime = LocalDateTime.now();
        String dtStr = dateTime.format(_dateTimeMsgFormat);
        var msg = String.format("The current date and time is %s", dtStr);
        sendExecReadOnlyMessage(msg, null);
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

    public void dump(final boolean verbose) {
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
            return;
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

        if (_executor != null) {
            out.printf("  Activities (cpSize=%d active=%d tasks=%d comp=%d):\n",
                       _executor.getCorePoolSize(),
                       _executor.getActiveCount(),
                       _executor.getTaskCount(),
                       _executor.getCompletedTaskCount());
            if (verbose) {
                _executor.getQueue().forEach(q -> out.printf("    %s\n", q.toString()));
            }
        }

        out.println("  Run Control Entries:");
        for (var rce : _runControlEntries.values()) {
            rce.dump(out, "    ", verbose);
        }

        for (var m : _managers) {
            m.dump(out, "  ", verbose);
        }
    }

    public void initialize() throws KExecException {
        for (var m : _managers) {
            m.initialize();
        }
    }

    public boolean isRunning() {
        return (_phase != Phase.Stopped) && (_phase != Phase.NotStarted);
    }

    public void sendExecReadOnlyMessage(final String message,
                                        final ConsoleId routing) {
        var romsg = new ReadOnlyMessage(_runControlEntry, routing, null, message, true);
        _consoleManager.sendReadOnlyMessage(romsg);
    }

    /*

func (e *Exec) SendExecReadReplyMessage(
	message string,
	maxReplyChars int,
	routing *kexec.ConsoleIdentifier,
) (string, error) {
	consMsg := kexec.ConsoleReadReplyMessage{
		Source:         e.runControlEntry,
		Routing:        routing,
		Text:           message,
		DoNotEmitRunId: true,
		MaxReplyLength: maxReplyChars,
	}

	err := e.consoleMgr.SendReadReplyMessage(&consMsg)
	if err != nil {
		return "", err
	}

	return consMsg.Reply, nil
}

func (e *Exec) SendExecRestrictedReadReplyMessage(
	message string,
	accepted []string,
	routing *kexec.ConsoleIdentifier,
) (string, error) {
	if len(accepted) == 0 {
		return "", fmt.Errorf("bad accepted list")
	}

	maxReplyLen := 0
	for _, acceptString := range accepted {
		if maxReplyLen < len(acceptString) {
			maxReplyLen = len(acceptString)
		}
	}

	consMsg := kexec.ConsoleReadReplyMessage{
		Source:         e.runControlEntry,
		Routing:        routing,
		Text:           message,
		DoNotEmitRunId: true,
		MaxReplyLength: maxReplyLen,
	}

	done := false
	for !done {
		err := e.consoleMgr.SendReadReplyMessage(&consMsg)
		if err != nil {
			return "", err
		}

		resp := strings.ToUpper(consMsg.Reply)
		for _, acceptString := range accepted {
			if acceptString == resp {
				done = true
				break
			}
		}
	}

	return strings.ToUpper(consMsg.Reply), nil
}
     */
    public void stop(final StopCode stopCode) {
        LogManager.logTrace(LOG_SOURCE, "stop(%s)", stopCode);
        _stopCode = stopCode;
        _stopFlag = true;
        _phase = Phase.Stopped;
        _executor.shutdownNow();
        try {
            if (!_executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LogManager.logWarning(LOG_SOURCE, "Executor failed to terminate");
            }
        } catch (InterruptedException ex) {
            // nothing to do here
        }
        _executor = null;
        for (var m : _managers) {
            m.stop();
        }
    }
}

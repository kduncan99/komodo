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
import com.bearsnake.komodo.kexec.keyins.KeyinManager;
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

@SuppressWarnings("resource")
public class Exec {

    public static final int JumpKeyIndex_1 = 0;
    public static final int JumpKeyIndex_2 = 1;
    public static final int JumpKeyIndex_3 = 2;
    public static final int JumpKeyIndex_4 = 3;
    public static final int JumpKeyIndex_5 = 4;
    public static final int JumpKeyIndex_6 = 5;
    public static final int JumpKeyIndex_7 = 6;
    public static final int JumpKeyIndex_8 = 7;
    public static final int JumpKeyIndex_9 = 8;
    public static final int JumpKeyIndex_10 = 9;
    public static final int JumpKeyIndex_11 = 10;
    public static final int JumpKeyIndex_12 = 11;
    public static final int JumpKeyIndex_13 = 12;
    public static final int JumpKeyIndex_14 = 13;
    public static final int JumpKeyIndex_15 = 14;
    public static final int JumpKeyIndex_16 = 15;
    public static final int JumpKeyIndex_17 = 16;
    public static final int JumpKeyIndex_18 = 17;

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
    private StopCode _stopCode;
    private boolean _stopFlag = false;

    private ConsoleManager _consoleManager;
    private KeyinManager _keyinManager;

    public Exec(final boolean[] jumpKeyTable) {
        _jumpKeys = jumpKeyTable;
        _allowRecoveryBoot = false;
        _phase = Phase.NotStarted;
        _instance = this;
        _consoleManager = new ConsoleManager();
        _keyinManager = new KeyinManager();
    }

    public Configuration getConfiguration() { return _configuration; }
    public ConsoleManager getConsoleManager() { return _consoleManager; }
    public ScheduledThreadPoolExecutor getExecutor() { return _executor; }
    public static Exec getInstance() { return _instance; }
    public KeyinManager getKeyinManager() { return _keyinManager; }
    public Phase getPhase() { return _phase; }
    public StopCode getStopCode() { return _stopCode; }
    public boolean isRecoveryBootAllowed() { return _allowRecoveryBoot; }
    public boolean isStopped() { return _phase == Phase.Stopped; }
    public synchronized void managerRegister(final Manager m) { _managers.add(m); }
    public synchronized void managerUnregister(final Manager m) { _managers.remove(m); }
    public void setConfiguration(final Configuration config) { _configuration = config; }

    public void boot() throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot");
        _phase = Phase.Booted;
        _stopFlag = false;

        _runControlEntries.clear();
        _runControlEntry = new ExecRunControlEntry(_configuration.MasterAccountId);
        _runControlEntries.put(_runControlEntry._runId, _runControlEntry);
        if (!_jumpKeys[JumpKeyIndex_9] && !_jumpKeys[JumpKeyIndex_13]) {
            // TODO populate rce's with entries from backlog and SMOQUE
            //   well, at some point. probably not here.
        }

        _executor = new ScheduledThreadPoolExecutor((int)_configuration.ExecThreads);
        _executor.setRemoveOnCancelPolicy(true);
        for (var m : _managers) {
            m.boot();
        }

        // TODO
        sendExecReadOnlyMessage("KEXEC Version (?)", null);
        displayDateAndTime();

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

    public void dump(final boolean verbose) {
        var now = LocalDateTime.now();
        var filename = String.format("kexec-%s.dump", _dumpFileNameFormat.format(now));
        PrintStream out = null;
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
        out.printf("  Phase:      %s\n", _phase);
        out.printf("  Stopped:    %s\n", _stopFlag);
        out.printf("  StopCode:   %s\n", _stopCode);

        var jkStr = new StringBuilder();
        for (var jkx = 0; jkx < 36; jkx++) {
            if (_jumpKeys[jkx]) {
                jkStr.append(jkx + 1).append(" ");
            }
        }
        out.printf("  JumpKeys:   %s\n", jkStr);
        out.println("  Run Control Entries:");
        for (var rce : _runControlEntries.values()) {
            rce.dump(out, "    ");
        }
        /*
	_, _ = fmt.Fprintf(dumpFile, "  Run Control Entries:\n")
	for _, rce := range e.runControlTable {
		rce.Dump(dumpFile, "    ")
	}

	// TODO something different when fullFlag is set

	e.consoleMgr.Dump(dumpFile, "")
	e.keyinMgr.Dump(dumpFile, "")
	e.nodeMgr.Dump(dumpFile, "")
	e.facMgr.Dump(dumpFile, "")
	e.mfdMgr.Dump(dumpFile, "")

	// TODO run control table, etc

	err = dumpFile.Close()
	if err != nil {
		err := fmt.Errorf("cannot close dump file:%v\n", err)
		return "", err
	}

	return fileName, nil
 */
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

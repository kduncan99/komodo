/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;

/**
 * Unit tests for SystemProcessor class
 */
public class Test_SystemProcessor {

    private static final Logger LOGGER = LogManager.getLogger("TESTER");

    private static class ConsoleExerciser {

        private enum JobState {
            Init,
            Running,
            Done,
        };

        private class Job extends Thread {

            private final String _name;
            private JobState _state = JobState.Init;
            private boolean _restart = false;
            private boolean _terminate = false;
            private long _timeStartMillis = 0;
            private long _timeTermMillis = 0;

            public Job(
                final String name
            ) {
                _name = name;
            }

            public void run() {
                _state = JobState.Running;
                _timeStartMillis = System.currentTimeMillis();
                _systemProcessor.consoleSendReadOnlyMessage(String.format("  %s START", _name), false);

                //TODO read-reply msg "{name}*JOB STARTING - CONTINUE? YN"
                //TODO read-reply msg "{name}*ENTER SLEEP INTERVAL IN SECONDS"
                //TODO read-reply msg "{name}*ENTER REPETITION COUNTER"
                //TODO loop on something or other

                _systemProcessor.consoleSendReadOnlyMessage(String.format("  %s DONE", _name));
                _state = JobState.Done;
                _timeTermMillis = System.currentTimeMillis();
            }
        }

        private int _inputCount = 0;
        private final Map<String, Job> _jobs = new HashMap<>();
        int _session = 0;               //  job monitor session
        boolean _stop = false;          //  stop the job monitor
        boolean _terminate = false;     //  terminate the exerciser
        SystemProcessor _systemProcessor = null;

        private void checkForInput() {
            String msg = _systemProcessor.consolePollInputMessage();
            if ((msg != null) && !msg.isEmpty()) {
                char idChar = msg.charAt(0);
                if (Character.isDigit(msg.charAt(0))) {
                    //  The input is a read-reply response.  Ensure we're waiting on it, check length, etc.
                    //  Then route it appropriately.
                    //  (does the SP clear it out - or the RESTSystemConsole? who notifies the client that it's been answered?)
                    //TODO
                } else {
                    //  Handle unsolicited input from the user.  First, echo it as a Read-Only message,
                    //  then validate it, then pass it along to the appropriate command handler.
                    String[] split = msg.toUpperCase().split(" ");
                    switch (split[0]) {
                        case "h":
                        case "H":   //  help
                            commandHelp(split);
                            break;

                        case "l":
                        case "L":   //  list all jobs
                            commandList(split);
                            break;

                        case "q":
                        case "Q":   //  quit
                            commandQuit(split);
                            break;

                        case "r":
                        case "R":   //  restart the job monitor
                            commandRestart(split);
                            break;

                        case "s":
                        case "S":   //  start a job
                            commandStart(split);
                            break;

                        case "t":
                        case "T":   //  terminate a particular job
                            commandTerminate(split);
                            break;

                        default:
                            _systemProcessor.consoleSendReadOnlyMessage("  Command Not Recognized - Enter H for Help");
                    }
                }
            }
        }

        private void commandHelp(
            final String[] commandSplit
        ) {
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("Invalid Input");
                return;
            }

            _systemProcessor.consoleSendReadOnlyMessage("  Commands:");
            _systemProcessor.consoleSendReadOnlyMessage("    H           - help");
            _systemProcessor.consoleSendReadOnlyMessage("    L           - list active jobs");
            _systemProcessor.consoleSendReadOnlyMessage("    Q           - quit exerciser");
            _systemProcessor.consoleSendReadOnlyMessage("    R           - restart job monitor");
            _systemProcessor.consoleSendReadOnlyMessage("    S {jobName} - start a job");
            _systemProcessor.consoleSendReadOnlyMessage("    T {jobName} - terminate a job");
            _inputCount++;
        }

        private void commandList(
            final String[] commandSplit
        ) {
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("Invalid Input");
                return;
            }

            synchronized (_jobs) {
                if (_jobs.size() == 0) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Job List Is Empty");
                } else {
                    long now = System.currentTimeMillis();
                    for (Job job : _jobs.values()) {
                        long msec = (job._state == JobState.Done) ? (job._timeTermMillis - job._timeStartMillis)
                                                                  : now - job._timeStartMillis;
                        String msg = String.format("  %s Time:%dms State%s", job._name, msec, job._state);
                        _systemProcessor.consoleSendReadOnlyMessage(msg);
                    }
                }
            }
        }

        private void commandQuit(
            final String[] commandSplit
        ) {
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("Invalid Input");
                return;
            }

            _terminate = true;
            _inputCount++;
        }

        public void commandRestart(
            final String[] commandSplit
        ) {
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("Invalid Input");
                return;
            }

            _stop = true;
            _inputCount++;
        }

        public void commandStart(
            final String[] commandSplit
        ) {
            if (commandSplit.length != 2) {
                _systemProcessor.consoleSendReadOnlyMessage("Invalid Input");
                return;
            }

            String jobName = commandSplit[1].toUpperCase();
            if (!Character.isAlphabetic(jobName.charAt(0))) {
                _systemProcessor.consoleSendReadOnlyMessage("Invalid Job Name");
                return;
            }
            for (int nx = 1; nx < commandSplit[1].length(); ++nx) {
                char ch = jobName.charAt(nx);
                if (!Character.isDigit(ch) && !(Character.isAlphabetic(ch))) {
                    _systemProcessor.consoleSendReadOnlyMessage("Invalid Job Name");
                    return;
                }
            }

            synchronized (_jobs) {
                if (_jobs.containsKey(jobName)) {
                    _systemProcessor.consoleSendReadOnlyMessage("Duplicate Job Name");
                    return;
                }

                Job job = new Job(jobName);
                _jobs.put(jobName, job);
                job.start();
            }

            _inputCount++;
        }

        public void commandTerminate(
            final String[] commandSplit
        ) {
            _systemProcessor.consoleSendReadOnlyMessage("Not yet implemented");
            _inputCount++;
        }

        public void jobMonitor() {
            _session++;
            _inputCount = 0;
            _terminate = false;
            _stop = false;
            LOGGER.info(String.format("Job Monitor Session %d Starting", _session));
            _systemProcessor.consoleReset();
            _systemProcessor.consoleSendReadOnlyMessage(String.format("Job Monitor Active - Session %d", _session));

            final long duration5Seconds = 5 * 1000;
            final long duration5Minutes = 5 * 60 * 1000;
            long next5SecondAction = System.currentTimeMillis() + duration5Seconds;
            long next5MinuteAction = System.currentTimeMillis() + duration5Minutes;

            while (!_stop && !_terminate) {
                checkForInput();

                final long now = System.currentTimeMillis();
                if (now >= next5SecondAction) {
                    fiveSecondAction();
                    next5SecondAction += duration5Seconds;
                }

                if (now >= next5MinuteAction) {
                    fiveMinuteAction();
                    next5MinuteAction += duration5Minutes;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }

            _systemProcessor.consoleSendReadOnlyMessage("  Shutting down jobs...");
            synchronized (_jobs) {
                for (Job job : _jobs.values()) {
                    job._terminate = true;
                    while (job._state != JobState.Done) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            LOGGER.catching(ex);
                        }
                    }
                }
                _jobs.clear();
            }

            _systemProcessor.consoleSendReadOnlyMessage(String.format("Job Monitor Session %d Stopped", _session));
            LOGGER.info(String.format("Job Monitor Session %d Stopped", _session));
        }

        public void exercise(
        ) throws MaxNodesException {
            _systemProcessor = InventoryManager.getInstance().createSystemProcessor();
            while (!_systemProcessor.isReady()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }

            LOGGER.info("Console Exerciser Starting");
            while (!_terminate) {
                jobMonitor();
            }
            LOGGER.info("Console Exerciser Done");
            _systemProcessor.terminate();
        }

        public void fiveSecondAction() {
            String[] messages = new String[2];
            String timeStr = Instant.now().toString().split("\\.")[0];
            messages[0] = String.format("Time %s  Session %d", timeStr, _session);
            messages[1] = String.format("Inputs:%d  Jobs:%d", _inputCount, _jobs.size());
            _systemProcessor.consoleSendStatusMessage(messages);
        }

        public void fiveMinuteAction() {
            String msg = String.format("T/D %s", Instant.now().toString().split("\\.")[0]);
            _systemProcessor.consoleSendReadOnlyMessage(msg, true);
            synchronized (_jobs) {
                _jobs.entrySet().removeIf(entry -> entry.getValue()._state == JobState.Done);
            }
        }
    }

    //  Note that this ONLY works if you run THIS TEST CASE EXPLICITLY.
    //  If you try to run the module, the environment variables are not set up.
    @Test
    public void exercise(
    ) throws MaxNodesException {
        new ConsoleExerciser().exercise();
    }

    //  TODO Need a variety of unit tests here
}

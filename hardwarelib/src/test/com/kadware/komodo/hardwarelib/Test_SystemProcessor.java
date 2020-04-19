/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import java.time.Instant;
import java.util.*;
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

        private static class Job extends Thread {

            private static int _nextNotificationId = 1;
            private String _pendingMessage;     //  from operator
            private final String _name;
            private JobState _state = JobState.Init;
            private final SystemProcessor _systemProcessor;
            private boolean _terminate = false;
            private long _timeStartMillis = 0;  //  system time when job started
            private long _timeTermMillis = 0;   //  system time when job completed

            public Job(
                final String name,
                final SystemProcessor sp
            ) {
                _name = name;
                _systemProcessor = sp;
            }

            public void run() {
                _state = JobState.Running;
                _timeStartMillis = System.currentTimeMillis();
                post("STARTED");

                String reply = null;
                while (!_terminate && (reply == null)) {
                    reply = postAndWait("JOB STARTING - CONTINUE? YN", 1);
                    if (reply != null) {
                        if (reply.equalsIgnoreCase("n")) {
                            _terminate = true;
                        } else if (!reply.equalsIgnoreCase("y")) {
                            reply = null;
                        }
                    }
                }

                //TODO read-reply msg "{name}*ENTER SLEEP INTERVAL IN SECONDS"
                //TODO read-reply msg "{name}*ENTER REPETITION COUNTER"
                int interval = 5000;
                int count = 10;

                if (!_terminate) {
                    while (count > 0) {
                        synchronized (this) {
                            try {
                                this.wait(interval);
                            } catch (InterruptedException ex) {
                                LOGGER.catching(ex);
                            }
                        }
                        count--;
                    }
                }

                post("DONE");
                _state = JobState.Done;
                _timeTermMillis = System.currentTimeMillis();
            }

            private void post(
                final String message
            ) {
                _systemProcessor.consoleSendReadOnlyMessage("  " + _name + ":" + message, false);
            }

            private String postAndWait(
                final String notification,
                final int maxReplySize
            ) {
                int nid;
                synchronized (Job.class) {
                    nid = _nextNotificationId++;
                }

                _systemProcessor.consoleSendReadReplyMessage(nid, "! " + _name + ":" + notification, maxReplySize);
                String reply = null;
                while (!_terminate && (reply == null)) {
                    if (_pendingMessage != null) {
                        if (_pendingMessage.length() > maxReplySize) {
                            _systemProcessor.consoleSendReadOnlyMessage("  " + _name + ":Response Too Long",
                                                                        false);
                        } else {
                            reply = _pendingMessage;
                        }
                        _pendingMessage = null;
                    } else {
                        synchronized(this) {
                            try {
                                this.wait(1000);
                            } catch (InterruptedException ex) {
                                //  nothing to do
                            }
                        }
                    }
                }

                _systemProcessor.consoleCancelReadReplyMessage(nid, "  " + _name + ":" + notification);
                return reply;
            }
        }

        private int _inputCount = 0;
        private final Map<String, Job> _jobs = new HashMap<>();
        int _session = 0;               //  job monitor session
        boolean _stop = false;          //  stop the job monitor
        boolean _terminate = false;     //  terminate the exerciser
        SystemProcessor _systemProcessor = null;

        private void checkForInput() {
            String msg = _systemProcessor.consolePollInputMessage(10000);
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

                        case "m":
                        case "M":   //  message
                            commandMessage(split);
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
            _inputCount++;
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Input");
                return;
            }

            _systemProcessor.consoleSendReadOnlyMessage("  Commands:");
            _systemProcessor.consoleSendReadOnlyMessage("    H                     - help");
            _systemProcessor.consoleSendReadOnlyMessage("    L                     - list active jobs");
            _systemProcessor.consoleSendReadOnlyMessage("    Q                     - quit exerciser");
            _systemProcessor.consoleSendReadOnlyMessage("    M {jobName} {message} - send a message to a job");
            _systemProcessor.consoleSendReadOnlyMessage("    R                     - restart job monitor");
            _systemProcessor.consoleSendReadOnlyMessage("    S {jobName}           - start a job");
            _systemProcessor.consoleSendReadOnlyMessage("    T {jobName}           - terminate a job");
        }

        private void commandList(
            final String[] commandSplit
        ) {
            _inputCount++;
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Input");
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
                        String msg = String.format("  %s Time:%dms State:%s Msg:%s",
                                                   job._name,
                                                   msec,
                                                   job._state,
                                                   job._pendingMessage == null ? "N" : "Y");
                        _systemProcessor.consoleSendReadOnlyMessage(msg);
                    }
                }
            }
        }

        private void commandQuit(
            final String[] commandSplit
        ) {
            _inputCount++;
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Input");
                return;
            }

            _terminate = true;
        }

        public void commandMessage(
            final String[] commandSplit
        ) {
            _inputCount++;
            if (commandSplit.length != 3) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Input");
                return;
            }

            String jobName = commandSplit[1];
            String message = commandSplit[2];
            Job job = null;
            synchronized (_jobs) {
                job = _jobs.get(jobName);
                if (job == null) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Job Not Found");
                } else if (job._state == JobState.Done) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Job Is Already Done");
                    job = null;
                } else if (job._pendingMessage != null) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Job Has Not Retrieved Previous Message");
                    job = null;
                } else {
                    job._pendingMessage = message;
                }
            }

            if (job != null) {
                synchronized (job) {
                    job.notify();
                }
            }
        }

        public void commandRestart(
            final String[] commandSplit
        ) {
            _inputCount++;
            if (commandSplit.length > 1) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Input");
                return;
            }

            _stop = true;
        }

        public void commandStart(
            final String[] commandSplit
        ) {
            _inputCount++;
            if (commandSplit.length != 2) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Input");
                return;
            }

            String jobName = commandSplit[1].toUpperCase();
            if (!Character.isAlphabetic(jobName.charAt(0))) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Job Name");
                return;
            }
            for (int nx = 1; nx < commandSplit[1].length(); ++nx) {
                char ch = jobName.charAt(nx);
                if (!Character.isDigit(ch) && !(Character.isAlphabetic(ch))) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Invalid Job Name");
                    return;
                }
            }

            synchronized (_jobs) {
                if (_jobs.containsKey(jobName)) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Duplicate Job Name");
                    return;
                }

                Job job = new Job(jobName, _systemProcessor);
                _jobs.put(jobName, job);
                job.start();
            }
        }

        public void commandTerminate(
            final String[] commandSplit
        ) {
            _inputCount++;
            if (commandSplit.length != 2) {
                _systemProcessor.consoleSendReadOnlyMessage("  Invalid Input");
                return;
            }

            String jobName = commandSplit[1];
            Job job = null;
            synchronized (_jobs) {
                job = _jobs.get(jobName);
                if (job == null) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Job Not Found");
                } else if (job._state == JobState.Done) {
                    _systemProcessor.consoleSendReadOnlyMessage("  Job Is Already Done");
                    job = null;
                } else {
                    job._terminate = true;
                }
            }

            if (job != null) {
                synchronized (job) {
                    job.notify();
                }
            }
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
            List<Job> jobsLeft = new LinkedList<>();
            synchronized (_jobs) {
                for (Job job : _jobs.values()) {
                    if (job._state != JobState.Done) {
                        job._terminate = true;
                        jobsLeft.add(job);
                    }
                }
            }

            for (Job job : jobsLeft) {
                synchronized (job) {
                    job.notify();
                }
            }

            boolean jobWait = true;
            while (jobWait) {
                jobWait = false;
                synchronized (_jobs) {
                    for (Job job : _jobs.values()) {
                        if (job._state != JobState.Done) {
                            jobWait = true;
                            break;
                        }
                    }
                }
            }
            _jobs.clear();

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

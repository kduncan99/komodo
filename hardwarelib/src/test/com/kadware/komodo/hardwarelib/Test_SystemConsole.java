/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;

/**
 * For testing the SystemConsole (presumably the RESTSystemConsole, but good for any implementor)
 */
public class Test_SystemConsole {

    private static final Logger LOGGER = LogManager.getLogger("SYSTEM_CONSOLE_TESTER");

    private enum JobState {
        Init,
        Running,
        Done,
    }

    private static class Context {

        private int _inputCount = 0;
        private final Map<String, Job> _jobs = new HashMap<>();
        int _session = 0;               //  job monitor session
        boolean _stop = false;          //  stop the job monitor
        final SystemProcessor _systemProcessor;
        boolean _terminate = false;     //  terminate the exerciser

        public Context(
            final SystemProcessor systemProcessor
        ) {
            _systemProcessor = systemProcessor;
        }
    }

    private static class Job extends Thread {

        private final Context _context;
        private static int _nextNotificationId = 1;
        private String _pendingMessage;     //  from operator
        private final String _name;
        private JobState _state = JobState.Init;
        private boolean _terminate = false;
        private long _timeStartMillis = 0;  //  system time when job started
        private long _timeTermMillis = 0;   //  system time when job completed

        public Job(
            final String name,
            final Context context
        ) {
            _name = name;
            _context = context;
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

            int interval = 5000;
            int count = 10;

            while (!_terminate && (count > 0)) {
                synchronized (this) {
                    try {
                        this.wait(interval);
                    } catch (InterruptedException ex) {
                        LOGGER.catching(ex);
                    }
                }
                count--;
            }

            post("DONE");
            _state = JobState.Done;
            _timeTermMillis = System.currentTimeMillis();
        }

        private void post(
            final String message
        ) {
            _context._systemProcessor.consoleSendReadOnlyMessage("  " + _name + ":" + message,
                                                                 false,
                                                                 true);
        }

        private String postAndWait(
            final String notification,
            final int maxReplySize
        ) {
            int nid;
            synchronized (Job.class) {
                nid = _nextNotificationId++;
            }

            _context._systemProcessor.consoleSendReadReplyMessage(nid,
                                                                  "! " + _name + ":" + notification, maxReplySize);
            String reply = null;
            while (!_terminate && (reply == null)) {
                if (_pendingMessage != null) {
                    if (_pendingMessage.length() > maxReplySize) {
                        _context._systemProcessor.consoleSendReadOnlyMessage("  " + _name + ":Response Too Long",
                                                                             false,
                                                                             false);
                    } else {
                        reply = _pendingMessage;
                    }
                    _pendingMessage = null;
                } else {
                    synchronized (this) {
                        try {
                            this.wait(1000);
                        } catch (InterruptedException ex) {
                            //  nothing to do
                        }
                    }
                }
            }

            _context._systemProcessor.consoleCancelReadReplyMessage(nid, "  " + _name + ":" + notification);
            return reply;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Command handler classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private final static Map<String, CommandHandler> _commandHandlers = new LinkedHashMap<>();

    static {
        _commandHandlers.put("H", new HelpCommandHandler());
        _commandHandlers.put("L", new ListCommandHandler());
        _commandHandlers.put("M", new MessageCommandHandler());
        _commandHandlers.put("Q", new QuitCommandHandler());
        _commandHandlers.put("R", new RestartCommandHandler());
        _commandHandlers.put("S", new StartCommandHandler());
        _commandHandlers.put("T", new TerminateCommandHandler());
    }

    private interface CommandHandler {
        String getHelp();
        int getMaximumTokens();
        int getMinimumTokens();
        void handle(
            final Context context,
            final String[] commandSplit
        );
    }

    private static class HelpCommandHandler implements CommandHandler {

        @Override
        public String getHelp() {
            return "    H                     - display list of commands";
        }

        @Override
        public int getMaximumTokens() {
            return 1;
        }

        @Override
        public int getMinimumTokens() {
            return 1;
        }

        @Override
        public void handle(
            final Context context,
            final String[] commandSplit
        ) {
            context._systemProcessor.consoleSendReadOnlyMessage("  Commands:");
            for (CommandHandler ch : _commandHandlers.values()) {
                context._systemProcessor.consoleSendReadOnlyMessage(ch.getHelp());
            }
        }
    }

    private static class ListCommandHandler implements CommandHandler {

        @Override
        public String getHelp() {
            return "    L                     - list active jobs";
        }

        @Override
        public int getMaximumTokens() {
            return 1;
        }

        @Override
        public int getMinimumTokens() {
            return 1;
        }

        @Override
        public void handle(
            final Context context,
            final String[] commandSplit
        ) {
            synchronized (context._jobs) {
                if (context._jobs.size() == 0) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Job List Is Empty");
                } else {
                    long now = System.currentTimeMillis();
                    for (Job job : context._jobs.values()) {
                        long msec = (job._state == JobState.Done) ? (job._timeTermMillis - job._timeStartMillis)
                                                                  : now - job._timeStartMillis;
                        String msg = String.format("  %s Time:%dms State:%s Msg:%s",
                                                   job._name,
                                                   msec,
                                                   job._state,
                                                   job._pendingMessage == null ? "N" : "Y");
                        context._systemProcessor.consoleSendReadOnlyMessage(msg);
                    }
                }
            }
        }
    }

    private static class QuitCommandHandler implements CommandHandler {

        @Override
        public String getHelp() {
            return "    Q                     - quit exerciser";
        }

        @Override
        public int getMaximumTokens() {
            return 1;
        }

        @Override
        public int getMinimumTokens() {
            return 1;
        }

        @Override
        public void handle(
            final Context context,
            final String[] commandSplit
        ) {
            context._terminate = true;
        }
    }

    private static class MessageCommandHandler implements CommandHandler {

        @Override
        public String getHelp() {
            return "    M {jobName} {message} - send a message to a job";
        }

        @Override
        public int getMaximumTokens() {
            return 3;
        }

        @Override
        public int getMinimumTokens() {
            return 3;
        }

        @Override
        public void handle(
            final Context context,
            final String[] commandSplit
        ) {
            String jobName = commandSplit[1];
            String message = commandSplit[2];
            Job job;
            synchronized (context._jobs) {
                job = context._jobs.get(jobName);
                if (job == null) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Job Not Found");
                } else if (job._state == JobState.Done) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Job Is Already Done");
                    job = null;
                } else if (job._pendingMessage != null) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Job Has Not Retrieved Previous Message");
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
    }

    private static class RestartCommandHandler implements CommandHandler {

        @Override
        public String getHelp() {
            return "    R                     - restart job monitor";
        }

        @Override
        public int getMaximumTokens() {
            return 1;
        }

        @Override
        public int getMinimumTokens() {
            return 1;
        }

        @Override
        public void handle(
            final Context context,
            final String[] commandSplit
        ) {
            context._stop = true;
        }
    }

    private static class StartCommandHandler implements CommandHandler {

        @Override
        public String getHelp() {
            return "    S {jobName}           - start a job";
        }

        @Override
        public int getMaximumTokens() {
            return 2;
        }

        @Override
        public int getMinimumTokens() {
            return 2;
        }

        @Override
        public void handle(
            final Context context,
            final String[] commandSplit
        ) {
            String jobName = commandSplit[1].toUpperCase();
            if (!Character.isAlphabetic(jobName.charAt(0))) {
                context._systemProcessor.consoleSendReadOnlyMessage("  Invalid Job Name");
                return;
            }
            for (int nx = 1; nx < commandSplit[1].length(); ++nx) {
                char ch = jobName.charAt(nx);
                if (!Character.isDigit(ch) && !(Character.isAlphabetic(ch))) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Invalid Job Name");
                    return;
                }
            }

            synchronized (context._jobs) {
                if (context._jobs.containsKey(jobName)) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Duplicate Job Name");
                    return;
                }

                Job job = new Job(jobName, context);
                context._jobs.put(jobName, job);
                job.start();
            }
        }
    }

    private static class TerminateCommandHandler implements CommandHandler {

        @Override
        public String getHelp() {
            return "    T {jobName}           - terminate a job";
        }

        @Override
        public int getMaximumTokens() {
            return 2;
        }

        @Override
        public int getMinimumTokens() {
            return 2;
        }

        @Override
        public void handle(
            final Context context,
            final String[] commandSplit
        ) {
            String jobName = commandSplit[1];
            Job job;
            synchronized (context._jobs) {
                job = context._jobs.get(jobName);
                if (job == null) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Job Not Found");
                } else if (job._state == JobState.Done) {
                    context._systemProcessor.consoleSendReadOnlyMessage("  Job Is Already Done");
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
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Scheduled things
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class InputPoller {

        private static final int POLL_WAIT_MILLIS = 30000;
        private final Context _context;

        InputPoller(
            final Context context
        ) {
            _context = context;
        }

        private class InputPollerThread implements Runnable {

            public void run() {
                String msg = _context._systemProcessor.consolePollInputMessage(POLL_WAIT_MILLIS);
                if ((msg != null) && !msg.isEmpty()) {
                    String[] split = msg.toUpperCase().split(" ");
                    CommandHandler ch = _commandHandlers.get(split[0].toUpperCase());
                    if (ch == null) {
                        _context._systemProcessor.consoleSendReadOnlyMessage("  Command Not Recognized - Enter H for Help");
                    } else if ((split.length < ch.getMinimumTokens()) || (split.length > ch.getMaximumTokens())) {
                        _context._systemProcessor.consoleSendReadOnlyMessage("  Invalid Command Syntax");
                    } else {
                        ++_context._inputCount;
                        ch.handle(_context, split);
                    }
                }
            }
        }

        private final Runnable _runnable = new InputPollerThread();
        private final ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> _schedule = null;

        private void start() {
            EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                this.getClass().getSimpleName(),
                                                "start");
            _schedule = _scheduler.scheduleWithFixedDelay(_runnable, 1, 1, SECONDS);
            LOGGER.traceExit(em);
        }

        private void stop() {
            EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                this.getClass().getSimpleName(),
                                                "stop");
            _schedule.cancel(false);
            _schedule = null;
            LOGGER.traceExit(em);
        }
    }

    private static class FiveMinuteActions {

        private static final int INTERVAL_SECODNDS = 5 * 60;
        private final Context _context;

        FiveMinuteActions(
            final Context context
        ) {
            _context = context;
        }

        private class FiveMinuteActionThread implements Runnable {

            public void run() {
                EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                    this.getClass().getSimpleName(),
                                                    "run");

                String msg = String.format("T/D %s", Instant.now().toString().split("\\.")[0]);
                _context._systemProcessor.consoleSendReadOnlyMessage(msg, true, false);
                synchronized (_context._jobs) {
                    _context._jobs.entrySet().removeIf(entry -> entry.getValue()._state == JobState.Done);
                }

                LOGGER.traceExit(em);
            }
        }

        private final Runnable _runnable = new FiveMinuteActionThread();
        private final ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> _schedule = null;

        private void start() {
            EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                this.getClass().getSimpleName(),
                                                "start");
            _schedule = _scheduler.scheduleWithFixedDelay(_runnable,
                                                          INTERVAL_SECODNDS,
                                                          INTERVAL_SECODNDS,
                                                          SECONDS);
            LOGGER.traceExit(em);
        }

        private void stop() {
            EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                this.getClass().getSimpleName(),
                                                "stop");
            _schedule.cancel(false);
            _schedule = null;
            LOGGER.traceExit(em);
        }
    }

    private static class FiveSecondActions {

        private static final int INTERVAL_SECODNDS = 5;
        private final Context _context;

        FiveSecondActions(
            final Context context
        ) {
            _context = context;
        }

        private class FiveSecondActionThread implements Runnable {

            public void run() {
                String[] messages = new String[2];
                String timeStr = Instant.now().toString().split("\\.")[0];
                messages[0] = String.format("Time %s  Session %d", timeStr, _context._session);
                messages[1] = String.format("Inputs:%d  Jobs:%d", _context._inputCount, _context._jobs.size());
                _context._systemProcessor.consoleSendStatusMessage(messages);
            }
        }

        private final Runnable _runnable = new FiveSecondActionThread();
        private final ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> _schedule = null;

        private void start() {
            EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                this.getClass().getSimpleName(),
                                                "start");
            _schedule = _scheduler.scheduleWithFixedDelay(_runnable,
                                                          INTERVAL_SECODNDS,
                                                          INTERVAL_SECODNDS,
                                                          SECONDS);
            LOGGER.traceExit(em);
        }

        private void stop() {
            EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                                this.getClass().getSimpleName(),
                                                "stop");
            _schedule.cancel(false);
            _schedule = null;
            LOGGER.traceExit(em);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  The main job monitor which acts as a sort of operating system
    //  ----------------------------------------------------------------------------------------------------------------------------

    public void jobMonitor(
        final Context context
    ) {
        //  Initialize
        context._session++;
        context._inputCount = 0;
        context._terminate = false;
        context._stop = false;
        LOGGER.info(String.format("Job Monitor Session %d Starting", context._session));
        context._systemProcessor.consoleReset();
        context._systemProcessor.consoleSendReadOnlyMessage(String.format("Job Monitor Active - Session %d", context._session));

        //  Start up the scheduled things
        InputPoller poller = new InputPoller(context);
        FiveMinuteActions fiveMinuteActions = new FiveMinuteActions(context);
        FiveSecondActions fiveSecondActions = new FiveSecondActions(context);

        poller.start();
        fiveMinuteActions.start();
        fiveSecondActions.start();

        //  Wait until stop or terminate
        synchronized (context) {
            while (!context._stop && !context._terminate) {
                try {
                    context.wait(10000);
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }
        }

        //  Stop the scheduled things
        poller.stop();
        fiveMinuteActions.stop();
        fiveSecondActions.stop();

        //  Clean up
        context._systemProcessor.consoleSendReadOnlyMessage("  Shutting down jobs...");
        List<Job> jobsLeft = new LinkedList<>();
        synchronized (context._jobs) {
            for (Job job : context._jobs.values()) {
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
            synchronized (context._jobs) {
                for (Job job : context._jobs.values()) {
                    if (job._state != JobState.Done) {
                        jobWait = true;
                        break;
                    }
                }
            }
        }
        context._jobs.clear();

        //  Done
        context._systemProcessor.consoleSendReadOnlyMessage(String.format("Job Monitor Session %d Stopped", context._session));
        LOGGER.info(String.format("Job Monitor Session %d Stopped", context._session));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Here is the main code
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void exercise(
    ) throws MaxNodesException,
             IOException {
        System.setProperty("BASE_DIR", "../test/");
        Deployer testDeployer = new Deployer();
        testDeployer.deploy();

        //  Start up an SP
        Context context = new Context(InventoryManager.getInstance().createSystemProcessor());
        while (!context._systemProcessor.isReady()) {
            Thread.onSpinWait();
        }

        //  Run the monitor repeatedly until we're told to knock it off.
        while (!context._terminate) {
            jobMonitor(context);
        }

        context._systemProcessor.terminate();
        testDeployer.remove();
    }
}

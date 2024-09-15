/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.scheduleManager.BatchRun;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;

class BLKeyinHandler extends KeyinHandler {

    private static final String[] HELP_TEXT = {
        "BL      [D [nnn]]\n",
        "BL[,NU] ident[,...]\n",
        "  Lists runs currently in backlog.",
        "  N       Runs which meet the run-id filter are *not* displayed.",
        "  U       Indicates the id list consists of user-ids instead of run-ids.",
        "  D [nnn] Provides detailed information, optionally limited to first nnn runs.",
        "          Valid values are 1 through 999.",
        "  ident   Indicates the run-id or user-id for which information is displayed.",
        "          Wildcards */? are valid within the run-id"
    };

    private boolean _detailFlag;
    private boolean _filterReversedFlag;
    private final LinkedList<String> _identPatterns = new LinkedList<>();
    private Integer _limitCount;
    private boolean _useridFlag;

    public static final String COMMAND = "BL";

    public BLKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_options == null) {
            // Look for optional 'D' and 'nnn' arguments
            if (_arguments != null) {
                var split = _arguments.split(" ");
                if (split.length > 2) {
                    return false;
                }
                if (!split[0].equalsIgnoreCase("D")) {
                    return false;
                }
                _detailFlag = true;
                if (split.length > 1) {
                    if (split[1].length() > 3) {
                        return false;
                    }

                    try {
                        _limitCount = Integer.parseInt(split[1]);
                        if (_limitCount < 1) {
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
        } else {
            for (var ch : _options.toUpperCase().toCharArray()) {
                if (ch == 'N') {
                    _filterReversedFlag = true;
                } else if (ch == 'U') {
                    _useridFlag = true;
                } else {
                    return false;
                }
            }

            // We don't want to see options if there are no identifiers,
            // but if that happens we want to echo back something more than 'Syntax Error'
            // so we'll postpone that check until process() time where we can display a custom message.
            // For now, just check the syntax of the identifier list -- and there's not much we can check either.
            if (_arguments != null) {
                var split = _arguments.split(",");
                for (var pattern : split) {
                    var test = pattern.replace("*", "A").replace("?", "A");
                    if ((_useridFlag && !Parser.isValidUserId(test)) || (!_useridFlag && !Parser.isValidRunid(test))) {
                        return false;
                    }
                    _identPatterns.add(pattern);
                }
            }
        }

        return true;
    }

    @Override
    String getCommand() {
        return COMMAND;
    }

    @Override
    String[] getHelp() {
        return HELP_TEXT;
    }

    @Override
    boolean isAllowed() {
        var genf = Exec.getInstance().getGenFileInterface();
        return genf != null && genf.isReady();
    }

    @Override
    void process() {
        var exec = Exec.getInstance();
        var sch = exec.getScheduleManager();
        if ((_filterReversedFlag || _useridFlag) && _identPatterns.isEmpty()) {
            exec.sendExecReadOnlyMessage("BL KEYIN:Options N and U are not allowed without userid/runid list");
            return;
        }

        var backlog = new LinkedList<>(sch.getBacklogRuns());
        if (backlog.isEmpty()) {
            exec.sendExecReadOnlyMessage("BACKLOG: -NONE", _source);
        } else if (_detailFlag) {
            processDetailed(exec, backlog);
        } else if (_identPatterns.isEmpty()) {
            processNormal(exec, backlog);
        } else {
            processIdentifiers(exec, backlog);
        }
    }

    /**
     * Tests an identifier to see if it is satisfied by the given pattern.
     * The pattern contains characters which should match one-for-one, optional '*' characters which
     * can be satisfied by zero or more of any character, and optional '?' characters which must be
     * satisfied by exactly one instance of any character.
     * Thus
     * "SP?ON" would match "SPOON" but not "SPON" or "SPOOON"
     * while
     * "F*K" would match "FIK" and "FORK" (and others), but not "FIIIIKN"
     *
     * @param identifier identifier to be tested
     * @param pattern    pattern to use for testing
     * @return true if the identifier matches the given pattern
     */
    private static boolean matchesPattern(final String identifier, final String pattern) {
        var regex = pattern.replace("?", ".").replace("*", ".*");
        return regex.matches(identifier);
    }

    private boolean matchesAnyPattern(final String identifier) {
        return _identPatterns.stream()
                             .anyMatch(pattern -> matchesPattern(identifier, pattern));
    }

    private void processDetailed(final Exec exec, final LinkedList<BatchRun> backlog) {
        // sort backlog by priority, then truncate the result according to limit count.
        // tally up number of candidates/held along the way.
        var priorityMap = new LinkedHashMap<Character, LinkedList<BatchRun>>();
        var numberCandidates = 0;
        var numberHeld = 0;

        for (var run : backlog) {
            if (run.isHeld()) {
                numberHeld++;
            } else {
                numberCandidates++;
            }
            if (!priorityMap.containsKey(run.getSchedulingPriority())) {
                priorityMap.put(run.getSchedulingPriority(), new LinkedList<>());
            }
            priorityMap.get(run.getSchedulingPriority()).add(run);
        }

        var sortedList = new LinkedList<BatchRun>();
        for (var entry : priorityMap.entrySet()) {
            for (var run : entry.getValue()) {
                sortedList.add(run);
                if ((_limitCount != null) && (sortedList.size() == _limitCount)) {
                    break;
                }
            }
        }

        // show header
        var now = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        String dateTimeStr = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
        var header = String.format("BACKLOG: CAND./HOLD=%d/%d %s", numberCandidates, numberHeld, dateTimeStr);
        exec.sendExecReadOnlyMessage(header, _source);

        var backlogMsg = new StringBuilder();
        var totalCounter = 0;
        for (var run : backlog) {
            if ((totalCounter % 3) == 0) {
                backlogMsg.append("BACKLOG:");
            }

            backlogMsg.append(" ").append(run.getActualRunId()).append("/");
            var siteId = run.getSiteId() == null ? "" : run.getSiteId();
            backlogMsg.append(siteId);
            if (run.isHeld()) {
                backlogMsg.append("*").append(run.getHighestPriorityHoldCondition().getCode());
            }

            totalCounter++;
            if ((totalCounter % 3) == 0) {
                exec.sendExecReadOnlyMessage(backlogMsg.toString(), _source);
                backlogMsg.setLength(0);
            }
        }
        if (!backlogMsg.isEmpty()) {
            exec.sendExecReadOnlyMessage(backlogMsg.toString(), _source);
        }
    }

    private void processIdentifiers(final Exec exec, final LinkedList<BatchRun> backlog) {
        var iter = backlog.iterator();
        while (iter.hasNext()) {
            var batchRun = iter.next();
            var ident = _useridFlag ? batchRun.getUserId() : batchRun.getActualRunId();
            var match = matchesAnyPattern(ident);
            if (match == _filterReversedFlag) {
                iter.remove();
            }
        }

        if (backlog.isEmpty()) {
            exec.sendExecReadOnlyMessage("BACKLOG: -NONE", _source);
            return;
        }

        var backlogMsg = new StringBuilder();
        var totalCounter = 0;
        for (var run : backlog) {
            if ((totalCounter & 0x03) == 0) {
                backlogMsg.append("BACKLOG:");
            }
            backlogMsg.append(" ").append(run.getActualRunId());
            totalCounter++;
            if ((totalCounter & 0x03) == 0) {
                exec.sendExecReadOnlyMessage(backlogMsg.toString(), _source);
                backlogMsg.setLength(0);
            }
        }
        if (!backlogMsg.isEmpty()) {
            exec.sendExecReadOnlyMessage(backlogMsg.toString(), _source);
        }
    }

    private void processNormal(final Exec exec, final LinkedList<BatchRun> backlog) {
        var backlogMsg = new StringBuilder();
        var totalCounter = 0;
        for (var run : backlog) {
            if ((totalCounter & 0x03) == 0) {
                backlogMsg.append("BACKLOG:");
            }
            backlogMsg.append(" ").append(run.getActualRunId());
            totalCounter++;
            if ((totalCounter & 0x03) == 0) {
                exec.sendExecReadOnlyMessage(backlogMsg.toString(), _source);
                backlogMsg.setLength(0);
            }
        }
        if (!backlogMsg.isEmpty()) {
            exec.sendExecReadOnlyMessage(backlogMsg.toString(), _source);
        }
    }
}

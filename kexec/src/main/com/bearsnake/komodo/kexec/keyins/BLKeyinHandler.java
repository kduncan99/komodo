/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import java.util.LinkedList;

class BLKeyinHandler extends KeyinHandler {

    private static final String[] HELP_TEXT = {
        "BL[,NU] [D[nnn]] [run-id | user-id,...]\n",
        "  Lists runs currently in backlog.",
        "  N      Runs which meet the run-id filter are *not* displayed.",
        "  U      Indicates the id list consists of user-ids instead of run-ids.",
        "  D[nnn] Provides detailed information, optionally limited to first nnn runs.",
        "         Valid values are 1 through 999.",
        "  run-id Indicates the run-id or user-id for which information is displayed.",
        "         Wildcards */? are valid within the run-id"
    };

    private boolean _detailFlag;
    private boolean _filterReversedFlag;
    private final LinkedList<String> _idList = new LinkedList<>();
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
        if (_options != null) {
            for (var ch : _options.toUpperCase().toCharArray()) {
                if (ch == 'N') {
                    _filterReversedFlag = true;
                } else if (ch == 'U') {
                    _useridFlag = true;
                } else {
                    return false;
                }
            }
        }

        // We will have 0, 1, or 2 arguments.
        // If we have 1 argument, it could be Dnnn or it could be an identifier or a list of identifiers.
        // In this case, if the format exactly matches 'D' [n [n [n]]] we will treat it as Dnnn; else the
        // argument is considered to be an identifier or a list of identifiers.
        // If we have 2 arguments, the first *must* be Dnnn, and the second *must* be an identifier or
        // a list of identifiers.
        if (_arguments != null) {
            var split = _arguments.split(" ");
            if (split.length == 1) {
                // TODO
            } else if (split.length == 2) {
                // TODO
            } else {
                return false;
            }
        }

        return true;
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        var genf = Exec.getInstance().getGenFileInterface();
        return genf != null && genf.isReady();
    }

    @Override
    void process() {
        var exec = Exec.getInstance();
        var sch = exec.getScheduleManager();
        var backlog = new LinkedList<>(sch.getBacklogRuns());
        if (backlog.isEmpty()) {
            exec.sendExecReadOnlyMessage("BACKLOG IS EMPTY", _source);
            return;
        }

        // Filter down backlog according to the content in the list
        if (!_idList.isEmpty()) {
            if (_filterReversedFlag) {
                // TODO
            } else {
                // TODO
            }
        }

        if (_limitCount != null) {
            while (backlog.size() > _limitCount) {
                backlog.pollLast();
            }
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
}

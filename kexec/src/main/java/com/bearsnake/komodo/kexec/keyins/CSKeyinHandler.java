/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.util.HashSet;
import java.util.Set;

class CSKeyinHandler extends KeyinHandler {

    private enum Command {
        DemandHold,
        DemandRelease,
        GeneralHold,
        GeneralRelease,
        IndividualHold,
        IndividualRelease,
        ReleaseAll,
        SetRunAttribute,
        TerminalHold,
        TerminalRelease,
    }

    private enum SubCommand {
        ChangeDeadlineTime,
        ChangeProcessorPriority,
        ChangeSchedulingPriority,
        ChangeStartTime,
        Force,
    }

    private Command _command = null;
    private SubCommand _subCommand = null;
    private boolean _filterUserIds = false; // instead of run-ids
    private Set<String> _identifiers = new HashSet<String>();
    private boolean _reverseFilter = false; // exclude instead of include listed run-ids/user-ids

    private static final String[] HELP_TEXT = {
        "CS ALL",
        "  Removes the master and individual hold conditions on backlog",
        "CS [A | H]",
        "  Sets (A) or removes (H) the master hold on backlog",
        "CS [AD | HD]",
        "  Sets (AD) or removes (HD) the hold on demand scheduling",
        "CS [AT | HT]",
        "  Sets (AT) or removes (HT) the hold on inactive terminals",
        "CS[,NU] [A | H] ident[,...]",
        "  Sets (A) or removes (H) the individual hold condition on individual",
        "    runs specified by ident (which can include wildcards)",
        // TODO should CS A runid allow a specific run out of backlog, even if the master hold is still set?
        "  N: acts on runs which are *not* in the list",
        "  U: identifiers are interpreted as user-ids instead of run-ids",
        "CS runid*[Px | Dhhmm | Shhmm | Ly | F]",
        "  Px changes scheduling priority for the run",
        "  Dhhmm sets the deadline time to the given wall clock time",
        "  Shhmm sets the start time to the given wall clock time",
        "  Ly changes processing priority for the run",
        "  F  forces the run out of backlog",
    };

    private static final String[] SYNTAX_TEXT = {
        "CS [A | AD | ALL | AT | H | HD | HT]",
        "CS[,NU] [A | H] ident[,...]",
        "CS runid* [Px | Dhhmm | Shhmm | Ly | F]",
    };

    public static final String COMMAND = "CS";

    public CSKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_arguments == null) {
            return false;
        } else if (_arguments.contains("*")) {
            var split = _arguments.toUpperCase().split("\\*");
            if (split.length != 2) {
                return false;
            } else if (!Parser.isValidRunid(split[0])) {
                return false;
            }

            _identifiers.add(split[0]);
            _command = Command.SetRunAttribute;
            var key = split[1].charAt(0);
            var value = split[1].substring(1);
            switch (key) {
                case 'P' -> {
                    // TODO validate and store value
                    _subCommand = SubCommand.ChangeSchedulingPriority;
                }
                case 'D' -> {
                    // TODO validate and store value
                    _subCommand = SubCommand.ChangeDeadlineTime;
                }
                case 'S' -> {
                    // TODO validate and store value
                    _subCommand = SubCommand.ChangeStartTime;
                }
                case 'L' -> {
                    // TODO validate and store value
                    _subCommand = SubCommand.ChangeProcessorPriority;
                }
                case 'F' -> {
                    if (value.length() != 1) {
                        return false;
                    }
                    _subCommand = SubCommand.Force;
                }
                default -> { return false; }
            }
            // TODO
        } else {
            var split = _arguments.toUpperCase().split(" ");
            var subSplit = new String[0];
            if (split.length == 2) {
                subSplit = split[1].split(",");
            } else if (split.length > 2) {
                return false;
            }

            switch (split[0].toUpperCase()) {
                case "ALL" -> {
                    if ((_options != null) || (subSplit.length > 0)) {
                        return false;
                    }
                    _command = Command.ReleaseAll;
                }
                case "A" -> {
                    if (subSplit.length == 0) {
                        _command = Command.GeneralRelease;
                    } else {
                        _command = Command.IndividualRelease;
                        // TODO process list of identifiers
                    }
                }
                case "AD" -> {
                    if ((_options != null) || (subSplit.length > 0)) {
                        return false;
                    }
                    _command = Command.DemandRelease;
                }
                case "AT" -> {
                    if ((_options != null) || (subSplit.length > 0)) {
                        return false;
                    }
                    _command = Command.TerminalRelease;
                }
                case "H" -> {
                    if (subSplit.length == 0) {
                        _command = Command.GeneralHold;
                    } else {
                        _command = Command.IndividualHold;
                        // TODO process list of identifiers
                    }
                }
                case "HD" -> {
                    if ((_options != null) || (subSplit.length > 0)) {
                        return false;
                    }
                    _command = Command.DemandHold;
                }
                case "HT" -> {
                    if ((_options != null) || (subSplit.length > 0)) {
                        return false;
                    }
                    _command = Command.TerminalHold;
                }
            }
        }

        return true;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        var genf = Exec.getInstance().getGenFileInterface();
        return genf != null && genf.isReady();
    }

    @Override
    void process() {
        var exec = Exec.getInstance();
        var sch = exec.getScheduleManager();
        switch (_command) {
            case DemandHold -> sch.setDemandSchedulingHold(true);
            case DemandRelease -> sch.setDemandSchedulingHold(false);
            case GeneralHold -> sch.setGeneralHold(true);
            case GeneralRelease -> sch.setGeneralHold(false);
            case IndividualHold -> {}//TODO
            case IndividualRelease -> {}//TODO
            case ReleaseAll -> {}//TODO
            case SetRunAttribute -> {}//TODO
            case TerminalHold -> sch.setTerminalHold(true);
            case TerminalRelease -> sch.setTerminalHold(false);
        }
    }
}

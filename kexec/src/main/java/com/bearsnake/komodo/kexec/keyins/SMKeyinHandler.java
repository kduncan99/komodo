/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.symbionts.Symbiont;

class SMKeyinHandler extends KeyinHandler {

    private enum Command {
        Change,
        Display,
        Initialize,
        Lock,               // locks a device - if printing, the file is finished first
        Queue,              // re-queues the file, locking the device
        ReprintOrSkip,      // re-prints or re-punches the output, or skips the file on input
        Suspend,            // immediately stops, locking the device - SM I resumes printing/punching
        TerminateDevice,    // terminates file and locks device (SM T)
        TerminateFile,      // terminates file and moves to the next (SM E)
    }

    private static final String[] HELP_TEXT = {
        "SM symbiont_name [ C E I L Q R Rnnn R+nnn RALL S T ]",
        "  With no operations, displays the status of the symbiont device.",
        "C[HANGE],[ DEF[AULT] | sz,top,bot,lpi ]: Displays or changes the page format",
        "E: Creates an EOF, terminating the active symbiont file.",
        "I: Initiates an inactive symbiont, resumes operation, simulates ATTN.",
        "L: Locks out a symbiont.",
        "Q: Requeues the current file, locking out the symbiont.",
        "R: Creates an EOF, removing the runstream being read on the symbiont.",
        "Rnnn:  Reprints or repunches nnn pages or cards.",
        "R+nnn: Advances nnn pages or cards.",
        "RALL:  Reprints or repunches the entire file.",
        "S: Suspends symbiont operation",
        "T: Terminates and locks the device.",
    };

    private static final String[] SYNTAX_TEXT = {
        "SM symbiont_name [ C E I L Q R Rnnn R+nnn RALL S T ]",
    };

    /*
SM xx Illegal PROBE INTERRUPT
(Exec) An error was encountered while probing a card reader.
SM KEY ERROR : INCORRECT BOTTOM MARGIN
(Exec) The SM CHANGE keyin contained an error. Correct the keyin and try again.
SM KEY ERROR : INCORRECT DIGIT
(Exec) The SM CHANGE keyin contained an error. Correct the keyin and try again.
SM KEY ERROR : INCORRECT LPI NOT 6, 8, OR 12
(Exec) The SM CHANGE keyin contained an error in the lines-per-inch field. Correct the keyin and try again.
SM KEY ERROR : INCORRECT SIZE
(Exec) The SM CHANGE keyin contained an error in the first numeric field (number of lines per page). Correct the keyin and try again.
SM KEY ERROR : INCORRECT SYNTAX
(Exec) The SM CHANGE keyin contained an error. Correct the keyin and try again.
SM KEY ERROR : INCORRECT TOP MARGIN
(Exec) The SM CHANGE keyin contained an error in the second numeric field. Correct the keyin and try again.
SM KEY ERROR : INVALID CARTRIDGE ID
(Exec) The SM BAND keyin contained an error. Correct the keyin and try again.
SM KEY ERROR : symbiont NOT VALID DEVICE
(Exec) The SM CHANGE keyin contained an error. Correct the keyin and try again.
SM KEY ERROR : TOP AND BOTTOM MARGIN GE SIZE
(Exec) The SM CHANGE keyin contained an inconsistency; the top and bottom margins require more than a full page. Correct the keyin and try again.
SM KEY ERROR : UPDATE FAILED
(Exec) Your attempt to change the print format with the SM CHANGE keyin has failed. Correct the keyin and try again. If the error persists, the site administrator must contact the Unisys Support Center.
     */

    public static final String COMMAND = "SM";

    private Command _command;

    private Integer _changeBottomMargin;
    private Integer _changeLinesPerInch;
    private Integer _changeLinesPerPage;
    private Integer _changeTopMargin;
    private boolean _repositionAll;
    private int _repositionCount;
    private String _symbiontName;

    public SMKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_arguments == null || _options != null) {
            return false;
        }

        var split = _arguments.split(" ");
        if (split.length > 2) {
            return false;
        }

        _symbiontName = split[0].toUpperCase();
        if (!Parser.isValidNodeName(_symbiontName)) {
            return false;
        }

        if (split.length == 1) {
            _command = Command.Display;
            return true;
        } else {
            var upperStr = split[1].toUpperCase();
            return switch (upperStr.charAt(0)) {
                case 'C' -> checkSyntaxChange(upperStr);
                case 'E' -> checkSyntaxEnd();
                case 'I' -> checkSyntaxInitialize();
                case 'L' -> checkSyntaxLock();
                case 'Q' -> checkSyntaxQueue();
                case 'R' -> checkSyntaxReprint(upperStr);
                case 'S' -> checkSyntaxSuspend();
                case 'T' -> checkSyntaxTerminate();
                default -> false;
            };
        }
    }

    private boolean checkSyntaxChange(final String argStr) {
        _command = Command.Change;

        var split = argStr.split(",");
        if ((split[0].length() > 1) && !split[0].equals("CHANGE")) {
            return false;
        }

        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();

        try {
            if (split.length >= 2) {
                if (split[1].equals("*")) {
                    _changeLinesPerPage = (int) (long) cfg.getIntegerValue(Tag.SHDGSP);
                } else if (!split[1].isEmpty()) {
                    _changeLinesPerPage = Integer.parseInt(split[1]);
                    if (_changeLinesPerPage < 1 || _changeLinesPerPage > 999) {
                        return false;
                    }
                }
            }

            if (split.length >= 3) {
                if (split[2].equals("*")) {
                    _changeTopMargin = (int) (long) cfg.getIntegerValue(Tag.SAFHDG);
                } else if (!split[2].isEmpty()) {
                    _changeTopMargin = Integer.parseInt(split[2]);
                    if (_changeTopMargin < 0 || _changeTopMargin > 999) {
                        return false;
                    }
                }
            }

            if (split.length >= 4) {
                if (split[3].equals("*")) {
                    _changeBottomMargin = (int) (long) cfg.getIntegerValue(Tag.STDBOT);
                } else if (!split[3].isEmpty()) {
                    _changeBottomMargin = Integer.parseInt(split[3]);
                    if (_changeBottomMargin < 0 || _changeBottomMargin > 999) {
                        return false;
                    }
                }
            }

            if (split.length >= 5) {
                if (split[4].equals("*")) {
                    _changeLinesPerInch = 8;
                } else if (!split[4].isEmpty()) {
                    _changeLinesPerInch = Integer.parseInt(split[4]);
                    if ((_changeLinesPerInch != 6) && (_changeLinesPerInch != 8) &&  (_changeLinesPerInch != 12)) {
                        return false;
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }

    private boolean checkSyntaxEnd() {
        _command = Command.TerminateFile;
        return true;
    }

    private boolean checkSyntaxInitialize() {
        _command = Command.Initialize;
        return true;
    }

    private boolean checkSyntaxLock() {
        _command = Command.Lock;
        return true;
    }

    private boolean checkSyntaxQueue() {
        _command = Command.Queue;
        return true;
    }

    private boolean checkSyntaxReprint(final String argStr) {
        _command = Command.ReprintOrSkip;
        _repositionAll = false;
        _repositionCount = 0;

        if (argStr.length() == 1) {
            return true;
        } else if (argStr.equals("RALL")) {
            _repositionAll = true;
            return true;
        } else {
            var digits = 0;
            for (int i = 1; i < argStr.length(); i++) {
                var ch = argStr.charAt(i);
                if (!Character.isDigit(ch)) {
                    return false;
                } else {
                    _repositionCount = _repositionCount * 10 + (ch - '0');
                    digits++;
                }
            }
            return (digits > 0) && (digits <= 3) && (_repositionCount <= 504);
        }
    }

    private boolean checkSyntaxSuspend() {
        _command = Command.Suspend;
        return true;
    }

    private boolean checkSyntaxTerminate() {
        _command = Command.TerminateDevice;
        return true;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        return true; // TODO - allowed after some point early in initialization...?
    }

    @Override
    void process() {
        var exec = Exec.getInstance();
        var sym = exec.getSymbiontManager();
        var symInfo = sym.getSymbiontInfo(_symbiontName);
        if (symInfo == null) {
            var msg = String.format("SM KEY ERROR : %s NOT VALID DEVICE", _symbiontName);
            exec.sendExecReadOnlyMessage(msg, _source);
            return;
        }

        try {
            switch (_command) {
                case Change -> processChange(symInfo);                      // SM C
                case Display -> processDisplay(symInfo);
                case Initialize -> processInitialize(symInfo);              // SM I
                case Lock -> processLock(symInfo);                          // SM L
                case Queue -> processRequeue(symInfo);                      // SM Q
                case ReprintOrSkip -> processReprintOrSkip(symInfo);        // SM R
                case Suspend -> processSuspend(symInfo);                    // SM S
                case TerminateDevice -> processTerminateDevice(symInfo);    // SM T
                case TerminateFile -> processTerminateFile(symInfo);        // SM E
            }
        } catch (ExecStoppedException ex) {
            // forget about it
        }
    }

    // TODO console messages for state changes... maybe should happen in subclasses...

    private void processChange(final Symbiont symInfo) {
        // C only applies to print devices
        if (!symInfo.isPrintSymbiont()) {
            Exec.getInstance().sendExecReadOnlyMessage("SM C does not apply to " + _symbiontName, _source);
            return;
        }

        if (_changeTopMargin + _changeBottomMargin >= _changeLinesPerInch) {
            var msg = "SM KEY ERROR: Top and bottom margin >= size";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        symInfo.setPageGeometry(_changeLinesPerPage, _changeTopMargin, _changeBottomMargin, _changeLinesPerInch);
    }

    private void processDisplay(final Symbiont symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage(symInfo.getStateString(), _source);
    }

    private void processInitialize(final Symbiont symInfo) throws ExecStoppedException {
        // I only applies to onsite devices
        if (!symInfo.isOnSiteSymbiont()) {
            Exec.getInstance().sendExecReadOnlyMessage("SM I does not apply to " + _symbiontName, _source);
        } else {
            symInfo.initialize();
            Exec.getInstance().sendExecReadOnlyMessage(_symbiontName + " Unlocked", _source);
        }
    }

    private void processLock(final Symbiont symInfo) throws ExecStoppedException {
        // L only applies to onsite devices
        if (!symInfo.isOnSiteSymbiont()) {
            var msg = String.format("SM L does not apply to %s", _symbiontName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        } else {
            symInfo.lockDevice();
            var msg = String.format("%s Locked", _symbiontName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        }
    }

    private void processReprintOrSkip(final Symbiont symInfo) throws ExecStoppedException {
        // R with no count and no ALL is allowed only on input devices.
        if ((_repositionCount == 0) && !_repositionAll && !symInfo.isInputSymbiont()) {
            var msg = String.format("SM R does not apply to %s", _symbiontName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        } else {
            if (_repositionAll) {
                symInfo.repositionAll();
            } else {
                symInfo.reposition(_repositionCount);
            }
        }
    }

    private void processRequeue(final Symbiont symInfo) throws ExecStoppedException {
        if (symInfo.isInputSymbiont() || symInfo.isRemoteSymbiont()) {
            var msg = String.format("SM Q does not apply to %s", symInfo.getSymbiontName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        } else {
            symInfo.requeue();
        }
    }

    private void processSuspend(final Symbiont symInfo) {
        if (!symInfo.isOutputSymbiont()) {
            var msg = String.format("SM S does not apply to %s", symInfo.getSymbiontName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        } else {
            symInfo.suspend();
            var msg = String.format("%s Suspended", _symbiontName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        }
    }

    private void processTerminateDevice(final Symbiont symInfo) throws ExecStoppedException {
        symInfo.terminateDevice();
        Exec.getInstance().sendExecReadOnlyMessage(symInfo.getStateString(), _source);
    }

    private void processTerminateFile(final Symbiont symInfo) throws ExecStoppedException {
        symInfo.terminateFile();
        Exec.getInstance().sendExecReadOnlyMessage(symInfo.getStateString(), _source);
    }
}

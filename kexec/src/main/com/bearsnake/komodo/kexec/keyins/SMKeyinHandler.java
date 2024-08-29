/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.symbionts.SymbiontInfo;

class SMKeyinHandler extends KeyinHandler {

    private enum Command {
        Change,
        Display,
        EndOfFile,
        Initialize,
        Lock,       // locks a device - if printing, the file is finished first
        Queue,      // requeues the file, locking the device
        Remove,
        Reprint,
        Suspend,    // immediately stops, locking the device - SM I resumes printing/punching
        Terminate,
    }

    private static final String[] HELP_TEXT = {
        "SM symbiont_name",
        "  Displays the status of the symbiont device.",
        "SM symbiont_name operation",
        "  Invokes the indicated operation on the symbiont device.",
        "C[HANGE],[ DEF[AULT] | size,top,bottom,lpi ]",
        "   Displays or changes the page format",
        "E: Creates an EOF, terminating the active symbiont file.",
        "I: Initiates an inactive symbiont, resumes operation, simulates ATTN.",
        "L: Locks out a symbiont.",
        "Q: Requeues the current file, locking out the symbiont.",
        "R: Creates an EOF, removing the runstream being read on the symbiont.",
        "Rnnn:  Reprints or repunches nnn pages or cards.",
        "R+nnn: Advances nnn pages or cards.",
        "RALL:  Reprints or repunches the entire file.",
        "S: Suspends symbiont operation",
        "T: Terminates the device with an EOF, discarding the remainder of the file.",
        "   Locks out the device."
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
    private String _symbiontName;
    private boolean _reprintAll;
    private int _reprintCount;

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
        if (!Exec.isValidNodeName(_symbiontName)) {
            return false;
        }

        if (split.length == 1) {
            _command = Command.Display;
            return true;
        } else {
            switch (split[1].toUpperCase()) {
                case "C", "CHANGE" -> {
                    _command = Command.Change;
                    return true;
                }
                case "E" -> {
                    _command = Command.EndOfFile;
                    return true;
                }
                case "I" -> {
                    _command = Command.Initialize;
                    return true;
                }
                case "L" -> {
                    _command = Command.Lock;
                    return true;
                }
                case "Q" -> {
                    _command = Command.Queue;
                    return true;
                }
                case "R" -> {
                    _command = Command.Remove;
                    return true;
                }
                case "RALL" -> {
                    _command = Command.Reprint;
                    _reprintAll = true;
                    return true;
                }
                case "S" -> {
                    _command = Command.Suspend;
                    return true;
                }
                case "T" -> {
                    _command = Command.Terminate;
                    return true;
                }
                default -> {
                    if (split[1].charAt(0) == 'R') {
                        // TODO
                        _command = Command.Reprint;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        return true; // TODO
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
                case Change -> processChange(symInfo);
                case Display -> processDisplay(symInfo);
                case EndOfFile -> processEndOfFile(symInfo);
                case Initialize -> processInitialize(symInfo);
                case Lock -> processLock(symInfo);
                case Queue -> processQueue(symInfo);
                case Remove -> processRemove(symInfo);
                case Reprint -> processReprint(symInfo);
                case Suspend -> processSuspend(symInfo);
                case Terminate -> processTerminate(symInfo);
            }
        } catch (ExecStoppedException ex) {
            // forget about it
        }
    }

    private void processChange(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage("Not Implemented", _source); // TODO
    }

    private void processDisplay(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage(symInfo.getStateString(), _source);
    }

    private void processEndOfFile(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage("Not Implemented", _source); // TODO
    }

    private void processInitialize(final SymbiontInfo symInfo) throws ExecStoppedException {
        symInfo.initialize();
        Exec.getInstance().sendExecReadOnlyMessage(symInfo.getStateString(), _source);
    }

    private void processLock(final SymbiontInfo symInfo) throws ExecStoppedException {
        symInfo.lock();
        Exec.getInstance().sendExecReadOnlyMessage(symInfo.getStateString(), _source);
    }

    private void processQueue(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage("Not Implemented", _source); // TODO
    }

    private void processRemove(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage("Not Implemented", _source); // TODO
    }

    private void processReprint(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage("Not Implemented", _source); // TODO
    }

    private void processSuspend(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage("Not Implemented", _source); // TODO
    }

    private void processTerminate(final SymbiontInfo symInfo) {
        Exec.getInstance().sendExecReadOnlyMessage("Not Implemented", _source); // TODO
    }
}

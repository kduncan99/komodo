/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;

class SMKeyinHandler extends KeyinHandler {

    /*
SM sname
Displays the status of symbiont device sname.
SM sname operation
Controls the symbiont devices. where:
sname
is the symbiont device name.
operation
is the operation to be performed. If you do not specify an operation, the system displays the device status: suspended, locked, active, or inactive. You can use one of the following values:
11–192
7831 0281–035
B Displays the current print band cartridge-ID or allows dynamic changes of the default cartridge-ID for the specified device.
C Displays the current page format for a printer or changes the current page format for a printer.
E Creates an end-of-file (EOF), terminating the active symbiont file.
I Initiates an inactive onsite device; resumes onsite device operation;
simulates an attention interrupt. L Locks out an onsite device.
Q Requeues the current file and locks out the device.
R Creates an EOF, removing the runstream that the input device is currently reading.
Raaa Reprints or repunches aaa pages or cards, where aaa is a decimal number. R+aaa Advances aaa pages or cards and then begins printing or punching, where
aaa is a decimal number.
RALL Reprints or repunches the entire file.
S Suspends device operation.
T Terminates the device with an EOF, losing the remainder of the file. The device is locked out.     */
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

    public static final String COMMAND = "SM";

    public SMKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        return true;//TODO
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
        // TODO
    }
}

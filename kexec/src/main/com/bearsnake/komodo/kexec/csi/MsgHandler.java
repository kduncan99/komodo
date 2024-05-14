/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.consoles.ReadOnlyMessage;
import com.bearsnake.komodo.kexec.consoles.ReadReplyMessage;
import com.bearsnake.komodo.kexec.exec.Exec;
import java.util.Collection;
import java.util.LinkedList;

import static com.bearsnake.komodo.baselib.Word36.C_OPTION;
import static com.bearsnake.komodo.baselib.Word36.H_OPTION;
import static com.bearsnake.komodo.baselib.Word36.I_OPTION;
import static com.bearsnake.komodo.baselib.Word36.N_OPTION;
import static com.bearsnake.komodo.baselib.Word36.S_OPTION;
import static com.bearsnake.komodo.baselib.Word36.W_OPTION;

class MsgHandler extends Handler {

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return false; }

    @Override
    public boolean allowTIP() { return false; }

    @Override
    public String getCommand() { return "MSG"; }

    @Override
    public void handle(final HandlerPacket hp) {
        var allowed = C_OPTION | H_OPTION | I_OPTION | N_OPTION | S_OPTION | W_OPTION;
        var mutex = C_OPTION | H_OPTION | I_OPTION | S_OPTION;
        if (!checkIllegalOptions(hp, allowed) || !checkMutuallyExclusiveOptions(hp, mutex)) {
            postSyntaxError(hp);
            return;
        }

        if ((hp._optionWord & W_OPTION) != 0) {
            if (!checkMutuallyExclusiveOptions(hp, mutex)) {
                postSyntaxError(hp);
                return;
            }
        }

        if ((hp._optionWord & N_OPTION) != 0) {
            // TODO just post what would have been the console message, to the tail sheet for batch jobs only
        } else {
            var cm = Exec.getInstance().getConsoleManager();
            var msg = hp._statement._operandFields.get(new SubfieldSpecifier(0, 0));
            if ((hp._optionWord & W_OPTION) == 0) {
                // read-only console message with possibly multiple console types (or none)
                Collection<ConsoleType> types = new LinkedList<>();
                if ((hp._optionWord & C_OPTION) != 0) {
                    types.add(ConsoleType.Communications);
                }
                if ((hp._optionWord & H_OPTION) != 0) {
                    types.add(ConsoleType.HardwareConfidence);
                }
                if ((hp._optionWord & I_OPTION) != 0) {
                    types.add(ConsoleType.InputOutput);
                }
                if ((hp._optionWord & S_OPTION) != 0) {
                    types.add(ConsoleType.System);
                }

                var roMsg = new ReadOnlyMessage(hp._runControlEntry,
                                                types.isEmpty() ? null : types,
                                                hp._runControlEntry.getRunId(),
                                                msg,
                                                false);
                cm.sendReadOnlyMessage(roMsg);
            } else {
                // read-reply console message - might have *one* console type
                var masked = hp._optionWord & (C_OPTION | H_OPTION | I_OPTION | S_OPTION);
                ConsoleType consType = switch((int)masked) {
                    case (int)C_OPTION -> ConsoleType.Communications;
                    case (int)H_OPTION -> ConsoleType.HardwareConfidence;
                    case (int)I_OPTION -> ConsoleType.InputOutput;
                    case (int)S_OPTION -> ConsoleType.System;
                    default -> null;
                };

                var rrMsg = new ReadReplyMessage(hp._runControlEntry,
                                                 consType,
                                                 hp._runControlEntry.getRunId(),
                                                 msg,
                                                 false,
                                                 false, 0);
                cm.sendReadReplyMessage(rrMsg);
                // TODO check for exec stopped
                // TODO check for run aborted
                // TODO if batch and response is 'X', abort run
            }
        }
    }
}

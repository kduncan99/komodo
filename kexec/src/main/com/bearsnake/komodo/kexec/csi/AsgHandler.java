/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.kexec.facilities.FacStatusCode;

import static com.bearsnake.komodo.baselib.Word36.A_OPTION;
import static com.bearsnake.komodo.baselib.Word36.C_OPTION;
import static com.bearsnake.komodo.baselib.Word36.T_OPTION;
import static com.bearsnake.komodo.baselib.Word36.U_OPTION;

class AsgHandler extends Handler {

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return true; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public void handle(final HandlerPacket hp) {
        if (cleanOptions(hp)) {
            postSyntaxError(hp);
            return;
        }

        if (!checkMutuallyExclusiveOptions(hp, A_OPTION | C_OPTION | T_OPTION | U_OPTION)) {
            return;
        }

        boolean ok = false;
        if ((hp._optionWord & A_OPTION) != 0) {
            ok = handleAssignCatalogedFile(hp);
        } else if ((hp._optionWord & (C_OPTION | U_OPTION)) != 0) {
            ok = handleAssignToBeCatalogedFile(hp);
        } else if ((hp._optionWord & T_OPTION) != 0) {
            ok = handleAssignTemporary(hp);
        } else {
            // None of A, C, U, or T specified...
            // TODO don't know how to handle this yet - maybe A, maybe T
        }

        if (ok) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{"ASG"});
        }
    }

    private boolean handleAssignCatalogedFile(final HandlerPacket hp) {
        return true;// TODO
    }

    private boolean handleAssignToBeCatalogedFile(final HandlerPacket hp) {
        return true;// TODO
    }

    private boolean handleAssignTemporary(final HandlerPacket hp) {
        return true;// TODO
    }
}

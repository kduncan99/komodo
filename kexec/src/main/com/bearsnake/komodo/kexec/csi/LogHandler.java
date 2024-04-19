/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;

class LogHandler extends Handler {

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return false; }

    @Override
    public void handle(final HandlerPacket hp) {
        if (!cleanOptions(hp) || (hp._optionWord != 0)) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
            hp._statement._resultCode = 0_400000_000000L;
            return;
        }

        if (hp._statement._operandFields.size() != 1) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
            hp._statement._resultCode = 0_400000_000000L;
            return;
        }

        var opField = hp._statement._operandFields.get(0);
        if (opField.size() != 1) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
            hp._statement._resultCode = 0_400000_000000L;
            return;
        }

        LogManager.logInfo(hp._runControlEntry.getRunId(), opField.get(0));
    }
}

/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;

abstract class Handler {

    abstract boolean allowCSF();
    abstract boolean allowCSI();
    abstract void handle(final HandlerPacket hp);

    /**
     * Compares the options word in the handler packet to the allowed options word,
     * producing a fac message for each option set in the given word which does not appear
     * in the allowed word.
     * If errors exist and the source is ER CSF$/ACSF$/CSI$, we post a contingency
     * @return true if no illegal options were specified
     */
    protected boolean checkIllegalOptions(final HandlerPacket hp,
                                          final long allowedOptions) {
        long bit = Word36.A_OPTION;
        char letter = 'A';
        boolean result = true;

        while (true) {
            if (((bit & hp._optionWord) != 0) && ((bit & allowedOptions) == 0)) {
                var params = new String[]{ String.format("%c", letter) };
                hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalOption, params);
                result = false;
            }

            if (bit == Word36.Z_OPTION) {
                break;
            } else {
                letter++;
                bit >>= 1;
            }
        }

        if (!result && hp._sourceIsExecRequest) {
            hp._runControlEntry.postContingency(012, 04, 040);
        }

        return result;
    }

    /**
     * Scans the options field and produces a corresponding options word.
     * Posts a contingency if there is an error and the source is ER CSF$/ACSF$/CSI$.
     * The result of the scan (the option word) is populated to the handler packet.
     * @return true if successful, false if there is an error in the options field
     */
    protected boolean cleanOptions(final HandlerPacket hp) {
        hp._optionWord = 0;
        if (!hp._statement._optionsFields.isEmpty()) {
            var optStr = hp._statement._optionsFields.get(0);
            for (var ch : optStr.toUpperCase().toCharArray()) {
                if (!Character.isAlphabetic(ch)) {
                    LogManager.logWarning(hp._statement._mnemonic, "Error in options field:%s", optStr);
                    if (hp._sourceIsExecRequest) {
                        hp._runControlEntry.postContingency(012, 04, 040);
                    }
                    return false;
                }

                var shift = (ch - 'A'); // A==0, Z==25, etc
                hp._optionWord |= (Word36.A_OPTION >> shift);
            }
        }
        return true;
    }

    protected void postSyntaxError(final HandlerPacket hp) {
        hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
        hp._statement._resultCode |= 0_400000_000000L;
    }
}

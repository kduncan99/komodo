/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;

import java.util.List;

abstract class Handler {

    abstract boolean allowCSF();
    abstract boolean allowCSI();
    abstract boolean allowTIP();
    abstract String getCommand();
    abstract void handle(final HandlerPacket hp) throws ExecStoppedException;

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
                hp._statement._facStatusResult.mergeStatusBits(0_400000_400000L);
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
     * Scans the options field in the packet to determine if more than one of the options
     * in the exclusive options mask are set in the options set.
     * Posts a contingency if there is an violation and the source is ER CSF$/ACSF$/CSI$.
     * @return true if there are no exclusion violations, else false
     */
    protected boolean checkMutuallyExclusiveOptions(final HandlerPacket hp,
                                                    final long exclusiveOptions) {
        long bit = Word36.A_OPTION;
        char letter = 'A';
        boolean result = true;
        char previousLetterSet = 0;

        while (true) {
            if (((bit & hp._optionWord) != 0) && ((bit & exclusiveOptions) == 0)) {
                if (previousLetterSet == 0) {
                    previousLetterSet = letter;
                } else {
                    var params = new String[]{ String.format("%c", previousLetterSet), String.format("%c", letter) };
                    hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalOptionCombination, params);
                    hp._statement._facStatusResult.mergeStatusBits(0_400000_400000L);
                    result = false;
                    // stop now - only report first of the potential conflicts
                    break;
                }
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
            var optStr = hp._statement._optionsFields.getFirst();
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

    /**
     * Returns a particular subfield of the operands if it was specified.
     * Saves calling code a little bit of annoyance
     * @param operands list of operand fields
     * @param fieldIndex index of operand field
     * @param subFieldIndex index of operand subfield
     * @return content of indicated subfield if it exists, else null
     */
    protected String getSubField(final List<List<String>>operands,
                                 final int fieldIndex,
                                 final int subFieldIndex) {
        if (fieldIndex <= operands.size()) {
            var fld = operands.get(fieldIndex);
            if (subFieldIndex <= fld.size()) {
                return fld.get(subFieldIndex);
            }
        }
        return null;
    }

    protected void postComplete(
        final HandlerPacket hp
    ) {
        hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{ getCommand() });
    }

    protected static void postSyntaxError(final HandlerPacket hp) {
        hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage);
        hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
    }
}

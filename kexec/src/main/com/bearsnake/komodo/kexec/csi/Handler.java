/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;

import java.util.Set;

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
    protected static boolean checkIllegalOptions(
        final HandlerPacket hp,
        final long allowedOptions
    ) {
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
     * Determines whether all the subfields specified in a particular CSI statement are in valid
     * fields/subfields.
     * @param hp HandlerPacket
     * @param validSubfields specific subfields which are valid, by field index and subfield index
     * @param unboundFields indexes of fields which can have any number of subfields
     * @return true if all the specified subfields are legal; false otherwise. If we return false, we put
     * one or more appropriate fac results into the handler packet so that there's nothing else for the
     * caller to do other than return.
     */
    protected static boolean checkIllegalFieldsAndSubfields(
        final HandlerPacket hp,
        final Set<SubfieldSpecifier> validSubfields,
        final Set<Integer> unboundFields
    ) {
        for (var subSpec : hp._statement._operandFields.keySet()) {
            if (!unboundFields.contains(subSpec.getFieldIndex()) && (!validSubfields.contains(subSpec))) {
                hp._statement._facStatusResult.postMessage(FacStatusCode.UndefinedFieldOrSubfield);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_00000L);
                return false;
            }
        }

        return true;
    }

    /**
     * Scans the options field in the packet to determine if more than one of the options
     * in the exclusive options mask are set in the options set.
     * Posts a contingency if there is an violation and the source is ER CSF$/ACSF$/CSI$.
     * @return true if there are no exclusion violations, else false
     */
    protected static boolean checkMutuallyExclusiveOptions(
        final HandlerPacket hp,
        final long exclusiveOptions
    ) {
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
     * Check placement field syntax for the statement in the HandlerPacket
     * We accept:
     *  '*' cu
     * Where cu is the name of a channel node
     *  '*' device
     * where device is the name of a device node
     *  i
     * where i is a letter from A to Z indicating a logical channel - the mapping from the letter to a particular
     * controller does not persist from one run to the next... One can only count on getting a compatible controller.
     *  in
     * where i is a letter from A to Z as above, and n is a number from 1 to 15 indicating a logical device
     * on the selected logical controller.
     * @return true if there are no syntax problems, false if there are syntax issues.
     * If we return false, we've already updated the facStatusResult field.
     */
    protected static boolean checkPlacementFieldSyntax(
        final HandlerPacket hp
    ) {
        var result = true;
        var placement = hp._statement._operandFields.get(new SubfieldSpecifier(1, 4));
        if ((placement != null) && !placement.isEmpty()) {
            if (placement.startsWith("*")) {
                result = Exec.isValidNodeName(placement.substring(1));
            } else {
                if (!Character.isUpperCase(placement.charAt(0))) {
                    result = false;
                } else {
                    try {
                        var i = Integer.parseInt(placement.substring(1));
                        result = (i >= 1) && (i <= 15);
                    } catch (NumberFormatException ex) {
                        result = false;
                    }
                }
            }
        }

        if (!result) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalCharactersInPlacementField);
            hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
        }

        return result;
    }

    /**
     * Scans the options field and produces a corresponding options word.
     * Posts a contingency if there is an error and the source is ER CSF$/ACSF$/CSI$.
     * The result of the scan (the option word) is populated to the handler packet.
     * @return true if successful, false if there is an error in the options field
     */
    protected static boolean cleanOptions(
        final HandlerPacket hp
    ) {
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
     * @return content of indicated subfield if it exists, else null
     */
    protected static String getSubField(
        final HandlerPacket hp,
        final SubfieldSpecifier subfieldSpec) {
        return (hp._statement._operandFields.get(subfieldSpec));
    }

    /**
     * Returns a particular subfield of the operands if it was specified.
     * Saves calling code a little bit of annoyance
     * @param hp handler packet
     * @param fieldIndex index of operand field
     * @param subFieldIndex index of operand subfield
     * @return content of indicated subfield if it exists, else null
     */
    protected static String getSubField(
        final HandlerPacket hp,
        final int fieldIndex,
        final int subFieldIndex
    ) {
        return (hp._statement._operandFields.get(new SubfieldSpecifier(fieldIndex, subFieldIndex)));
    }

    /**
     * Returns a particular subfield of the operands if it was specified, or the default value otherwise
     * Saves calling code a little bit of annoyance
     */
    protected static String getSubFieldOr(
        final HandlerPacket hp,
        final int fieldIndex,
        final int subFieldIndex,
        final String defaultValue
    ) {
        var result = hp._statement._operandFields.get(new SubfieldSpecifier(fieldIndex, subFieldIndex));
        return result == null ? defaultValue : result;
    }

    protected void postComplete(
        final HandlerPacket hp
    ) {
        hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{ getCommand() });
    }

    protected static void postSyntaxError(
        final HandlerPacket hp
    ) {
        hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage);
        hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
    }
}

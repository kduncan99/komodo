/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;

class QualHandler extends Handler {

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

        if (!checkIllegalOptions(hp, Word36.D_OPTION | Word36.R_OPTION)) {
            return;
        }

        if (hp._statement._operandFields.size() > 1) {
            LogManager.logInfo(Interpreter.LOG_SOURCE,
                               "[%s] Too many operand fields:%s",
                               hp._runControlEntry.getRunId(),
                               hp._statement._originalStatement);
            postSyntaxError(hp);
            return;
        }

        String qualifier = null;
        if (hp._statement._operandFields.size() == 1) {
            if (hp._statement._operandFields.getFirst().size() != 1) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Too many operand subfields:%s",
                                   hp._runControlEntry.getRunId(),
                                   hp._statement._originalStatement);
                postSyntaxError(hp);
                return;
            }

            qualifier = hp._statement._operandFields.getFirst().getFirst();
            if (!Exec.isValidQualifier(qualifier)) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Invalid qualifier:%s",
                                   hp._runControlEntry.getRunId(),
                                   hp._statement._originalStatement);
                postSyntaxError(hp);
                return;
            }
        }

        if (hp._optionWord == 0) {
            // set implied qualifier
            if (qualifier == null) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Missing qualifier:%s",
                                   hp._runControlEntry.getRunId(),
                                   hp._statement._originalStatement);
                if (hp._sourceIsExecRequest) {
                    hp._runControlEntry.postContingency(012, 04, 040);
                }
                hp._statement._facStatusResult.postMessage(FacStatusCode.DirectoryOrQualifierMustAppear, null);
                hp._statement._resultCode |= 0_600000_000000L;
                return;
            }

            hp._runControlEntry.setImpliedQualifier(qualifier);
            hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{"QUAL"});
        } else if (hp._optionWord == Word36.D_OPTION) {
            // set default qualifier
            if (qualifier == null) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Missing qualifier:%s",
                                   hp._runControlEntry.getRunId(),
                                   hp._statement._originalStatement);
                if (hp._sourceIsExecRequest) {
                    hp._runControlEntry.postContingency(012, 04, 040);
                }
                hp._statement._facStatusResult.postMessage(FacStatusCode.DirectoryOrQualifierMustAppear, null);
                hp._statement._resultCode |= 0_600000_000000L;
                return;
            }

            hp._runControlEntry.setDefaultQualifier(qualifier);
            hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{"QUAL"});
        } else if (hp._optionWord == Word36.R_OPTION) {
            // reset default and implied qualifiers
            if (qualifier != null) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Qualifier should not be specified:%s",
                                   hp._runControlEntry.getRunId(),
                                   hp._statement._originalStatement);
                if (hp._sourceIsExecRequest) {
                    hp._runControlEntry.postContingency(012, 04, 040);
                }
                hp._statement._facStatusResult.postMessage(FacStatusCode.DirectoryAndQualifierMayNotAppear, null);
                hp._statement._resultCode |= 0_600000_000000L;
                return;
            }

            hp._runControlEntry.setDefaultQualifier(hp._runControlEntry.getProjectId());
            hp._runControlEntry.setImpliedQualifier(hp._runControlEntry.getProjectId());
            hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{"QUAL"});
        } else {
            // conflicting options
            LogManager.logInfo(Interpreter.LOG_SOURCE,
                               "[%s] Conflicting options:%s",
                               hp._runControlEntry.getRunId(),
                               hp._statement._originalStatement);
            if (hp._sourceIsExecRequest) {
                hp._runControlEntry.postContingency(012, 04, 040);
            }

            hp._statement._facStatusResult.postMessage(FacStatusCode.IllegalOptionCombination, new String[]{ "D", "R" });
            hp._statement._resultCode |= 0_600000_000000L;
        }
    }
}

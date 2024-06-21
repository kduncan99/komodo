/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;

import java.util.HashSet;
import java.util.Set;

import static com.bearsnake.komodo.baselib.Word36.D_OPTION;
import static com.bearsnake.komodo.baselib.Word36.R_OPTION;

class QualHandler extends Handler {

    private static final Set<SubfieldSpecifier> VALID_SUBFIELD_SPECS = new HashSet<>();
    static {
        VALID_SUBFIELD_SPECS.add(new SubfieldSpecifier(0, 0)); // qualifier
    }

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return true; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "QUAL"; }

    @Override
    public void handle(final HandlerPacket hp) {
        if (cleanOptions(hp)) {
            postSyntaxError(hp);
            return;
        }

        var allowedOpts = D_OPTION | R_OPTION;
        var mutexOpts = D_OPTION | R_OPTION;
        if (!checkIllegalOptions(hp, allowedOpts) || !checkMutuallyExclusiveOptions(hp, mutexOpts)) {
            return;
        }
        if (!checkIllegalFieldsAndSubfields(hp, VALID_SUBFIELD_SPECS, null)) {
            if (hp._sourceIsExecRequest) {
                hp._run.postContingency(012, 04, 040);
            }
            return;
        }

        String qualifier = getSubField(hp, 0, 0);
        if ((qualifier != null) && !Exec.isValidQualifier(qualifier)) {
            LogManager.logInfo(Interpreter.LOG_SOURCE,
                               "[%s] Invalid qualifier:%s",
                               hp._run.getActualRunId(),
                               hp._statement._originalStatement);
            postSyntaxError(hp);
            return;
        }

        if (hp._optionWord == 0) {
            // set implied qualifier
            if (qualifier == null) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Missing qualifier:%s",
                                   hp._run.getActualRunId(),
                                   hp._statement._originalStatement);
                if (hp._sourceIsExecRequest) {
                    hp._run.postContingency(012, 04, 040);
                }
                hp._statement._facStatusResult.postMessage(FacStatusCode.DirectoryOrQualifierMustAppear);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            hp._run.setImpliedQualifier(qualifier);
            hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{"QUAL"});
        } else if (hp._optionWord == D_OPTION) {
            // set default qualifier
            if (qualifier == null) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Missing qualifier:%s",
                                   hp._run.getActualRunId(),
                                   hp._statement._originalStatement);
                if (hp._sourceIsExecRequest) {
                    hp._run.postContingency(012, 04, 040);
                }
                hp._statement._facStatusResult.postMessage(FacStatusCode.DirectoryOrQualifierMustAppear);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            hp._run.setDefaultQualifier(qualifier);
            hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{"QUAL"});
        } else if (hp._optionWord == R_OPTION) {
            // reset default and implied qualifiers
            if (qualifier != null) {
                LogManager.logInfo(Interpreter.LOG_SOURCE,
                                   "[%s] Qualifier should not be specified:%s",
                                   hp._run.getActualRunId(),
                                   hp._statement._originalStatement);
                if (hp._sourceIsExecRequest) {
                    hp._run.postContingency(012, 04, 040);
                }
                hp._statement._facStatusResult.postMessage(FacStatusCode.DirectoryAndQualifierMayNotAppear);
                hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            hp._run.setDefaultQualifier(hp._run.getProjectId());
            hp._run.setImpliedQualifier(hp._run.getProjectId());
            hp._statement._facStatusResult.postMessage(FacStatusCode.Complete, new String[]{"QUAL"});
        }
    }
}

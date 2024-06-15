/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;

import java.util.HashSet;
import java.util.Set;

import static com.bearsnake.komodo.baselib.Word36.I_OPTION;

class UseHandler extends Handler {

    private static final Set<SubfieldSpecifier> VALID_SUBFIELD_SPECS = new HashSet<>();
    static {
        VALID_SUBFIELD_SPECS.add(new SubfieldSpecifier(0, 0)); // internal filename
        VALID_SUBFIELD_SPECS.add(new SubfieldSpecifier(1, 0)); // referenced filename
    }

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return true; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "USE"; }

    @Override
    public void handle(final HandlerPacket hp) {
        var rce = hp._run;
        var fsResult = hp._statement._facStatusResult;

        if (cleanOptions(hp)) {
            postSyntaxError(hp);
            return;
        }

        if (!checkIllegalOptions(hp, I_OPTION)) {
            return;
        }
        if (!checkIllegalFieldsAndSubfields(hp, VALID_SUBFIELD_SPECS, null)) {
            if (hp._sourceIsExecRequest) {
                rce.postContingency(012, 04, 040);
            }
            return;
        }

        String internal = getSubField(hp, 0, 0);
        if (internal == null) {
            fsResult.postMessage(FacStatusCode.InternalNameRequired);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }
        internal = internal.toUpperCase();
        if (!Exec.isValidFilename(internal)) {
            fsResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            fsResult.mergeStatusBits(0_400000_000000L);
            return;
        }

        String external = getSubField(hp, 1, 0);
        if (external == null) {
            fsResult.postMessage(FacStatusCode.FilenameIsRequired);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }
        var parser = new Parser(external.toUpperCase());
        FileSpecification fileSpec;
        try {
            fileSpec = FileSpecification.parse(parser, " ,.");
            // keep stupid IDE happy
            if (fileSpec == null) {
                return;
            }
        } catch (FileSpecification.InvalidFileCycleException ex) {
            fsResult.postMessage(FacStatusCode.IllegalValueForFCycle);
            fsResult.mergeStatusBits(0_600000_00000L);
            return;
        } catch (FileSpecification.Exception ex) {
            fsResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        fileSpec = rce.getFacilitiesItemTable().resolveInternalFilename(fileSpec);
        fileSpec = rce.resolveQualifier(fileSpec);

        var iOption = (hp._optionWord & I_OPTION) != 0;
        var fm = Exec.getInstance().getFacilitiesManager();
        fm.establishUseItem(rce, internal, fileSpec, iOption);
    }
}

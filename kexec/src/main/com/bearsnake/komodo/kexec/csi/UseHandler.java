/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;

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
        if (cleanOptions(hp)) {
            postSyntaxError(hp);
            return;
        }

        if (!checkIllegalOptions(hp, I_OPTION)) {
            return;
        }
        if (!checkIllegalFieldsAndSubfields(hp, VALID_SUBFIELD_SPECS, null)) {
            if (hp._sourceIsExecRequest) {
                hp._runControlEntry.postContingency(012, 04, 040);
            }
            return;
        }

        String internal = getSubField(hp, 0, 0);
        if (internal == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.InternalNameRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }
        internal = internal.toUpperCase();
        if (!Exec.isValidFilename(internal)) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
            return;
        }

        String external = getSubField(hp, 1, 0);
        if (external == null) {
            hp._statement._facStatusResult.postMessage(FacStatusCode.FilenameIsRequired);
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            return;
        }
        var parser = new Parser(external.toUpperCase());
        FileSpecification fileSpec;
        try {
            fileSpec = FileSpecification.parse(parser, " ,.");
        } catch (FileSpecification.Exception ex) {
            // TODO make this a little more specific
            // TODO post status message
            hp._statement._facStatusResult.mergeStatusBits(0_600000_000000L);
            if (hp._sourceIsExecRequest) {
                hp._runControlEntry.postContingency(012, 04, 040);
            }
            return;
        }

        var iOption = (hp._optionWord & I_OPTION) != 0;
        // TODO call fac mgr to finish the work (which means we have to add this ability to fac mgr)
        //   fm.use(hp._runControlEntry, internal, fileSpec, iOption, hp._statement._facStatusResult);
    }
}

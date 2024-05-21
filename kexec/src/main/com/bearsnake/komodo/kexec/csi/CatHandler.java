/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;

import static com.bearsnake.komodo.baselib.Word36.A_OPTION;
import static com.bearsnake.komodo.baselib.Word36.B_OPTION;
import static com.bearsnake.komodo.baselib.Word36.C_OPTION;
import static com.bearsnake.komodo.baselib.Word36.E_OPTION;
import static com.bearsnake.komodo.baselib.Word36.G_OPTION;
import static com.bearsnake.komodo.baselib.Word36.H_OPTION;
import static com.bearsnake.komodo.baselib.Word36.J_OPTION;
import static com.bearsnake.komodo.baselib.Word36.L_OPTION;
import static com.bearsnake.komodo.baselib.Word36.M_OPTION;
import static com.bearsnake.komodo.baselib.Word36.O_OPTION;
import static com.bearsnake.komodo.baselib.Word36.P_OPTION;
import static com.bearsnake.komodo.baselib.Word36.R_OPTION;
import static com.bearsnake.komodo.baselib.Word36.S_OPTION;
import static com.bearsnake.komodo.baselib.Word36.T_OPTION;
import static com.bearsnake.komodo.baselib.Word36.U_OPTION;
import static com.bearsnake.komodo.baselib.Word36.V_OPTION;
import static com.bearsnake.komodo.baselib.Word36.W_OPTION;
import static com.bearsnake.komodo.baselib.Word36.Z_OPTION;

class CatHandler extends Handler {

    private static final long COMMON_OPTIONS = G_OPTION | P_OPTION | R_OPTION | S_OPTION | V_OPTION | W_OPTION | Z_OPTION;
    private static final long MASS_STORAGE_OPTIONS = COMMON_OPTIONS | B_OPTION;
    private static final long TAPE_OPTIONS = COMMON_OPTIONS | E_OPTION | H_OPTION | J_OPTION | L_OPTION | M_OPTION | O_OPTION;

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return true; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "CAT"; }

    // MassStorage:
    // @CAT[,options] filename
    //   [,type/reserve/granule/maximum,
    //   pack-id-1/.../pack-id-n,,,ACR-name]
    // options: B,G,P,R,S,V,W,Z
    //
    // Tape:
    // @CAT,options filename,
    //   type[/units/log/noise/processor/tape/format/data-converter/block-numbering/data-compression/buffered-write/expanded-buffer,
    //   reel-1/reel-2/.../reel-n,
    //   expiration-period/mmspec,,ACR-name,CTL-pool]
    // --> E:256333 Units and logical subfields not allowed on CAT image.
    // options: E,G,H,J,L,M,O,P,R,S,V,W,Z
    //
    // If you omit type, the Exec uses sector-addressable mass storage. Appendix D, Table D-1 shows the standard type entries supplied by Unisys.
    // If you are creating a new cycle for an existing file cycle set, and you omit the type, the type is taken from
    //   the latest existing cycle of the file. If all cycles of the file are in a to-be-cataloged or to-be-dropped state,
    //   then the type from the highest cycle of this type is used.
    @Override
    public void handle(
        final HandlerPacket hp
    ) throws ExecStoppedException {
        var e = Exec.getInstance();
        var rce = hp._runControlEntry;
        var fsResult = hp._statement._facStatusResult;

        // get the filename field
        var fnField = getSubField(hp, 0, 0);
        if (fnField == null) {
            fsResult.postMessage(FacStatusCode.FilenameIsRequired);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        FileSpecification fileSpec;
        try {
            fileSpec = FileSpecification.parse(new Parser(fnField), ".,/ ");
        } catch (FileSpecification.InvalidFileCycleException ex) {
            fsResult.postMessage(FacStatusCode.IllegalValueForFCycle);
            fsResult.mergeStatusBits(0_600000_00000L);
            return;
        } catch (FileSpecification.Exception ex) {
            fsResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        if (fileSpec == null) {
            fsResult.postMessage(FacStatusCode.FilenameIsRequired);
            fsResult.mergeStatusBits(0_600000_000000L);
            return;
        }

        fileSpec = rce.getUseItemTable().resolveFileSpecification(fileSpec);
        fileSpec = rce.resolveQualifier(fileSpec);

        // Brief sanity check
        if (!checkMutuallyExclusiveOptions(hp, A_OPTION | C_OPTION | T_OPTION | U_OPTION)) {
            return;
        }

        var fm = e.getFacilitiesManager();

        // If the type is specified, determine whether it is disk or tape and invoke the proper
        // version of cataloging via the fac manager.
        var typeField = getSubField(hp, 1, 0);
        if (typeField != null) {
            if (typeField.length() > 6) {
                fsResult.postMessage(FacStatusCode.AssignMnemonicTooLong);
                fsResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            var mnemonicType = e.getConfiguration().getMnemonicType(typeField);
            if (mnemonicType == null) {
                fsResult.postMessage(FacStatusCode.MnemonicIsNotConfigured, new String[]{ typeField });
                fsResult.mergeStatusBits(0_600000_000000L);
                return;
            }

            switch (mnemonicType) {
                case SECTOR_ADDRESSABLE_DISK, WORD_ADDRESSABLE_DISK -> fm.catalogDiskFile(fileSpec, fsResult);
                case TAPE -> fm.catalogTapeFile(fileSpec, fsResult);
            }

            return;
        }

        // Type is not specified -- Facilities needs to figure out what to do.
        // We could, but we'd have to go to MFD, *then* fac, and this would introduce a potential race condition
        // since we're not locking between those two calls (and can't, at least not easily).
        fm.catalogFile(fileSpec, fsResult);
    }
}

/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.kex.kasm.exceptions.ParameterException;
import java.util.HashMap;

/**
 * Manages BankGeometry objects - map is keyed by the L,BDI value of each bank
 */
class BankGeometryMap extends HashMap<Integer, BankGeometry> {

    /**
     * Define bank geometry given a client's specified (or implied) bank declaration,
     * and the minimum size of the content of the bank - must account for:
     *      address collision avoidance (if requested)
     *      lower and upper limit granularity as defined by the large size setting in the declaration
     *      absolute min/max limits (including silly stuff, like negative lower limits)
     */
    void mapBankDeclaration(
        final BankDeclaration bankDeclaration,
        final int contentSize
    ) throws ParameterException {
        int lbdi = (bankDeclaration._bankLevel << 15) | bankDeclaration._bankDescriptorIndex;

        if (bankDeclaration._startingAddress < 0) {
            throw new ParameterException(String.format("Invalid starting address for bank %06o: %o",
                                                       lbdi,
                                                       bankDeclaration._startingAddress));
        }

        if (contentSize < 0) {
            throw new ParameterException(String.format("Invalid content size for bank %06o: %d", lbdi, contentSize));
        }

        if (!bankDeclaration._options.contains(BankDeclaration.BankDeclarationOption.DBANK)
            && bankDeclaration._startingAddress < 01000) {
            throw new ParameterException(String.format("IBank %06o has invalid starting address: %o",
                                                       lbdi,
                                                       bankDeclaration._startingAddress));
        }

        int lower = bankDeclaration._startingAddress;
        if (bankDeclaration._avoidCollision) {
            for (BankGeometry existing : values()) {
                if (lower <= existing._upperLimit) {
                    lower = existing._upperLimit + 1;
                }
            }
        }

        int llmask = bankDeclaration._largeBank ? 077777 : 0777;
        if ((lower & llmask) != 0) {
            lower = (lower | llmask) + 1;
        }

        int llAdjusted = bankDeclaration._largeBank ? (lower >> 15) : (lower >> 9);
        if (llAdjusted > 0777) {
            throw new ParameterException(String.format("Resulting lower limit for bank %06o is out of range: %o",
                                                       lbdi,
                                                       llAdjusted));
        }

        int upper = lower + contentSize - 1;
        if (contentSize > 0) {
            if ((upper >= 0) && (bankDeclaration._largeBank)) {
                upper |= 077;
            }

            int ulAdjusted = bankDeclaration._largeBank ? (upper >> 6) : upper;
            if (ulAdjusted > 0_777_777_777) {
                throw new ParameterException(String.format("Resulting upper limit for bank %06o is out of range: %o",
                                                           lbdi,
                                                           ulAdjusted));
            }
        }

        put(lbdi, new BankGeometry(lower, upper));
    }
}

/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

/**
 * Interim object describing the calculated address limits / size / etc of a particular bank.
 * Bridges the gap between the BankDeclaration provided by the client, and the LoadableBank
 * created toward the end of the linking process
 */
class BankGeometry {

    final int _lowerLimit;
    final int _upperLimit;  //  will be -1 for lowerlimit == 0 and content length == 0

    BankGeometry(
        int lowerLimit,
        int upperLimit
    ) {
        _lowerLimit = lowerLimit;
        _upperLimit = upperLimit;
    }
}

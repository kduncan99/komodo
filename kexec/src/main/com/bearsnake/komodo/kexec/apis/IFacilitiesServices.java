/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.apis;

import com.bearsnake.komodo.kexec.exceptions.KExecException;

public interface IFacilitiesServices {

    boolean isValidPackName(String packName);
    boolean isValidPrepFactor(int value);
    void startup() throws KExecException;
}

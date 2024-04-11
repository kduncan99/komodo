/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.apis;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.mfd.MFDRelativeAddress;

public interface IMFDManager {

    void recoverMassStorage() throws ExecStoppedException;
    void setFileToBeDeleted(MFDRelativeAddress mainItem0Address);

}

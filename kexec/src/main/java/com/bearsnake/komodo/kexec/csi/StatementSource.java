/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

public enum StatementSource {
    ECL,            // from batch or demand control mode
    ER_CSF,         // from ER CSF$/ACSF$ in a program
    ER_CSI,         // from ER CSI$ in a program
    Transparent,    // @@ from demand
}

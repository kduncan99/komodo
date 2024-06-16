/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import java.util.Arrays;

public enum SDFFileType {
    FTP(001),
    READ$(010),
    PUNCH$(010),
    AT_FILE(016),
    PRINT$(025),
    Generic(030),
    PCIOS(035);

    private final int _code;

    SDFFileType(final int code) {
        _code = code;
    }

    public static SDFFileType getSDFFileType(
        final int code
    ) {
        return Arrays.stream(SDFFileType.values())
                     .filter(sft -> sft._code == code)
                     .findFirst()
                     .orElse(Generic);
    }
}

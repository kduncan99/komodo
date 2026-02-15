/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import java.util.Arrays;

public enum ItemType {

    FreeItem(0),
    InputQueueItem(1),
    OutputQueueItem(2),
    PartNameItem(3),
    SystemItem(077);

    private final int _code;

    ItemType(
        final int code
    ) {
        _code = code;
    }

    public int getCode() { return _code; }

    public static ItemType getItemType(
        final int code
    ) {
        return Arrays.stream(ItemType.values())
                     .filter(itemType -> itemType._code == code)
                     .findFirst()
                     .orElse(null);
    }
}

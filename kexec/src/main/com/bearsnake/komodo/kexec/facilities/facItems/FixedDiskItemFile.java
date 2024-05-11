/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import com.bearsnake.komodo.hardwarelib.Node;

public class FixedDiskItemFile extends DiskFileFacilitiesItem {

    public FixedDiskItemFile(
        final Node node,
        final String requestedPackName
    ) {
        super(node, requestedPackName);
    }
}

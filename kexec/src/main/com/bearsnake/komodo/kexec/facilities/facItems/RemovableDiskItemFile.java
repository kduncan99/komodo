/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import com.bearsnake.komodo.hardwarelib.Node;

public class RemovableDiskItemFile extends DiskFileFacilitiesItem {

    public RemovableDiskItemFile(
        final Node node,
        final String requestedPackName
    ) {
        super(node, requestedPackName);
    }
}

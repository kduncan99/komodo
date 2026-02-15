/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

public enum ReleaseBehavior {
    // If this is an internal name, release the underlying file and all internal names
    Normal,

    // If this is an internal name, release only the internal name
    // Otherwise, assume Normal behavior
    ReleaseUseItemOnly,

    // If this is an internal name, release it.
    // If it is the only internal name for the referenced file, release the referenced file as well
    ReleaseUseItemOnlyUnlessLast,

    // Retain use items, but release the referenced file.
    RetainUseItems
}

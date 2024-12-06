/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

public class PlacementInfo {

    public final PlacementType _placementType;
    public final int _nodeIdentifier;

    public PlacementInfo(
        final PlacementType placementType,
        final int nodeIdentifier
    ) {
        _placementType = placementType;
        _nodeIdentifier = nodeIdentifier;
    }
}

/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.facilities.facItems.FacilitiesItem;

public class UseItem {

    private final String _internalName;
    private final FileSpecification _fileSpecification;
    private final boolean _releaseFileOnTaskEnd;
    private FacilitiesItem _facilitiesItem; // if null, this is a name-only item

    public UseItem(
        final String internalName,
        final FileSpecification fileSpecification,
        final FacilitiesItem facilitiesItem,
        final boolean releaseFileOnTaskEnd
    ) {
        _internalName = internalName;
        _fileSpecification = fileSpecification;
        _releaseFileOnTaskEnd = releaseFileOnTaskEnd;
        _facilitiesItem = facilitiesItem;
    }

    public UseItem(
        final String internalName,
        final FileSpecification fileSpecification,
        final boolean releaseFileOnTaskEnd
    ) {
        this(internalName, fileSpecification, null, releaseFileOnTaskEnd);
    }

    public String getInternalName() { return _internalName; }
    public FileSpecification getFileSpecification() { return _fileSpecification; }
    public FacilitiesItem getFacilitiesItem() { return _facilitiesItem; }
    public boolean releaseFileOnTaskEnd() { return _releaseFileOnTaskEnd; }
    public void setFacilitiesItem(final FacilitiesItem item) { _facilitiesItem = item; }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(_internalName);
        if (_releaseFileOnTaskEnd) {
            sb.append("[,I]");
        }
        sb.append(" = ").append(_fileSpecification.toString());
        if (_facilitiesItem == null) {
            sb.append(" (name item)");
        } else {
            sb.append("->").append(_fileSpecification);
        }
        return sb.toString();
    }
}

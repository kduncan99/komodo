/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;

import java.util.HashMap;

public class UseItemTable {

    private static final HashMap<String, UseItem> _content = new HashMap<>();

    public boolean addUseItem(
        final UseItem useItem
    ) {
        // TODO
        //   fail if item already exists...?
        _content.put(useItem.getInternalName(), useItem);
        return true;
    }

    public UseItem getUseItem(
        final String internalName
    ) {
        return _content.get(internalName);
    }

    /**
     * Resolves a file specification by chasing use names.
     * @return original or updated file specification, as appropriate
     */
    public FileSpecification resolveFileSpecification(
        final FileSpecification originalFileSpecification
    ) {
        var fs = originalFileSpecification;
        while (fs.couldBeInternalName()) {
            var ui = _content.get(fs.getFilename());
            if (ui == null) {
                break;
            }
            fs = ui.getFileSpecification();
        }
        return fs;
    }
}

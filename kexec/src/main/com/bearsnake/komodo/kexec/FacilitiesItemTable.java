/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.kexec.facilities.facItems.FacilitiesItem;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FacilitiesItemTable {

    private final ConcurrentLinkedQueue<FacilitiesItem> _content = new ConcurrentLinkedQueue<>();

    public void addFacilitiesItem(final FacilitiesItem facItem) { _content.add(facItem); }
    public ConcurrentLinkedQueue<FacilitiesItem> getFacilitiesItems() { return _content; }
    public void removeFacilitiesItem(final FacilitiesItem facItem) { _content.remove(facItem); }

    public void dump(final PrintStream out,
                     final String indent,
                     final boolean verbose) {
        for (var fi : _content) {
            fi.dump(out, indent + "  ");
        }
    }

    /**
     * Searches for facilities item which is an exact match on the given file specification.
     * There must be an exact match on qualifier and filename.
     * If there is no file cycle spec in the file specification, the facilities item must have:
     *      no absolute or relative file cycle (is this possible?)
     *      or an absolute file cycle of zero
     *      or a relative file cycle of zero
     * If there is an absolute file cycle spec in the file specification, the facilities item must have
     * a matching absolute file cycle.
     * If there is a relative file cycle spec in the file specification, the facilities item must have
     * a matching relative file cycle.
     * This is used for (but maybe not exclusively for) checking whether a particular potential
     * temporary file assignment refers to a pre-existing entry in the facitem list.
     */
    public FacilitiesItem getExactFacilitiesItem(
        final FileSpecification fileSpecification
    ) {
        var filename = fileSpecification.getFilename();
        var qualifier = fileSpecification.getQualifier();
        var cycleSpec = fileSpecification.getFileCycleSpecification();
        for (var fi : _content) {
            if (fi.getQualifier().equals(qualifier) && (fi.getFilename().equals(filename))) {
                if (cycleSpec == null) {
                    if (!fi.hasAbsoluteCycle() && !fi.hasRelativeCycle()) {
                        return fi;
                    }
                    if (fi.hasAbsoluteCycle() && fi.getAbsoluteCycle() == 0) {
                        return fi;
                    } else if (fi.hasRelativeCycle() && fi.getRelativeCycle() == 0) {
                        return fi;
                    }
                } else if (fi.hasAbsoluteCycle() && cycleSpec.isAbsolute() && fi.getAbsoluteCycle() == cycleSpec.getCycle()) {
                    return fi;
                } else if (fi.hasRelativeCycle() && cycleSpec.isRelative() && fi.getRelativeCycle() == cycleSpec.getCycle()) {
                    return fi;
                }
            }
        }

        return null;
    }

}

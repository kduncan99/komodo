/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.kexec.FileCycleSpecification;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.facilities.facItems.FacilitiesItem;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FacilitiesItemTable {

    private static final ConcurrentLinkedQueue<FacilitiesItem> _content = new ConcurrentLinkedQueue<>();

    /**
     * Adds the facilities item to the table.
     * @param facItem new facilities item
     */
    void addFacilitiesItem(
        final FacilitiesItem facItem
    ) {
        _content.add(facItem);
    }

    public void dump(final PrintStream out,
                     final String indent,
                     final boolean verbose) {
        for (var fi : _content) {
            fi.dump(out, indent + "  ");
        }
    }

    /**
     * Searches for facilities item which is an exact match on the given file specification.
     * This is useful primarily for dealing with temporary file assignments.
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
    synchronized FacilitiesItem getExactFacilitiesItem(
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
                } else if (fi.hasAbsoluteCycle()
                    && cycleSpec.isAbsolute()
                    && (fi.getAbsoluteCycle() == cycleSpec.getCycle())) {
                    return fi;
                } else if (fi.hasRelativeCycle()
                    && cycleSpec.isRelative()
                    && (fi.getRelativeCycle() == cycleSpec.getCycle())) {
                    return fi;
                }
            }
        }

        return null;
    }

    /**
     * Retrieves the facilities item which matches the given qualifier, filename, and absolute file cycle
     * @return facilities item if found, else null
     */
    synchronized FacilitiesItem getFacilitiesItemByAbsoluteCycle(
        final String qualifier,
        final String filename,
        final int absoluteCycle
    ) {
        for (var fi : _content) {
            if (fi.getQualifier().equals(qualifier)
                && fi.getFilename().equals(filename)
                && fi.getAbsoluteCycle() == absoluteCycle) {
                return fi;
            }
        }
        return null;
    }

    /**
     * We return the first facilities item which has a matching filename component.
     * @param filename filename component
     * @return facilities item if found, else null
     */
    synchronized FacilitiesItem getFacilitiesItemByFilename(
        final String filename
    ) {
        return _content.stream()
                       .filter(fi -> fi.getFilename().equalsIgnoreCase(filename))
                       .findFirst()
                       .orElse(null);
    }

    /**
     * Retrieves the facilities item which matches the given qualifier, filename, and relative file cycle
     * @return facilities item if found, else null
     */
    synchronized FacilitiesItem getFacilitiesItemByRelativeCycle(
        final String qualifier,
        final String filename,
        final int relativeCycle
    ) {
        for (var fi : _content) {
            if (fi.getQualifier().equalsIgnoreCase(qualifier)
                && fi.getFilename().equalsIgnoreCase(filename)
                && fi.hasRelativeCycle()
                && fi.getRelativeCycle() == relativeCycle) {
                return fi;
            }
        }
        return null;
    }

    /**
     * If filename is an internal name for a facilities item create a new FileSpec representing
     * Otherwise, return the first facilities item with a matching filename component.
     * @param fileSpecification original FileSpecification
     * @return new FileSpecification representing the external qual*file *if* the given file spec is an internal name;
     *          otherwise we just return the input.
     */
    public synchronized FileSpecification resolveInternalFilename(
        final FileSpecification fileSpecification
    ) {
        if (fileSpecification.couldBeInternalName()) {
            for (var fi : _content) {
                if (fi.hasInternalName(fileSpecification.getFilename())) {
                    FileCycleSpecification fcs = null;
                    if (fi.hasAbsoluteCycle()) {
                        fcs = FileCycleSpecification.newAbsoluteSpecification(fi.getAbsoluteCycle());
                    }
                    return new FileSpecification(fi.getQualifier(), fi.getFilename(), fcs, null, null);
                }
            }
        }

        return fileSpecification;
    }

    /**
     * Removes the facilities from the table.
     * @param facItem new facilities item
     */
    void removeFacilitiesItem(
        final FacilitiesItem facItem
    ) {
        _content.remove(facItem);
    }

}

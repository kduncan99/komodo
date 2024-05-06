/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;

public abstract class FacilitiesItem {

    private Integer _absoluteCycle;
    private String _filename;
    private final HashSet<String> _internalNames = new HashSet<>();
    private boolean _isTemporary;
    private long _optionsWord;
    private String _qualifier;
    private Integer _relativeCycle;
    private boolean _releaseOnTaskEnd;

    public void dump(
        final PrintStream out,
        final String indent) {
        var sb = new StringBuilder();
        sb.append(_qualifier).append("*").append(_filename);
        if (_absoluteCycle != null) {
            sb.append(" abs=").append(_absoluteCycle);
        }
        if (_relativeCycle != null) {
            sb.append(" rel=").append(_relativeCycle);
        }
        out.printf("%s%s\n", indent, sb);

        if (hasInternalNames()) {
            var nameStr = String.join(" ", getInternalNames());
            out.printf("%s  IntNames:%s\n", indent, nameStr);
        }
    }

    public final int getAbsoluteCycle() { return _absoluteCycle; }
    public final String getFilename() { return _filename; }
    public final Collection<String> getInternalNames() { return new HashSet<>(_internalNames); }
    public final long getOptionsWord() { return _optionsWord; }
    public final int getRelativeCycle() { return _relativeCycle; }
    public final String getQualifier() { return _qualifier; }
    public final boolean getReleaseOnTaskEnd() { return _releaseOnTaskEnd; }
    public final boolean isTemporary() { return _isTemporary; }

    public final FacilitiesItem setAbsoluteCycle(final int cycle) { _absoluteCycle = cycle; return this; }
    public final FacilitiesItem setFilename(final String filename) { _filename = filename; return this; }
    public final FacilitiesItem setIsTemporary(final boolean value) { _isTemporary = value; return this; }
    public final FacilitiesItem setOptionsWord(final long value) { _optionsWord = value; return this; }
    public final FacilitiesItem setQualifier(final String qualifier) { _qualifier = qualifier; return this; }
    public final FacilitiesItem setRelativeCycle(final int cycle) { _relativeCycle = cycle; return this; }
    public final FacilitiesItem setReleaseOnTaskEnd(final boolean value) { _releaseOnTaskEnd = value; return this; }

    public final boolean hasAbsoluteCycle() { return _absoluteCycle != null; }
    public final boolean hasInternalNames() { return !_internalNames.isEmpty(); }
    public final boolean hasRelativeCycle() { return _relativeCycle != null; };
}

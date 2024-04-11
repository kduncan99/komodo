/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

/**
 * Describes an MFD file-relative address. The address refers to a particular directory sector.
 * These addresses are essentially the same thing as any file-relative address for a sector addressable file.
 * There is some particularity to them, however...
 * The format of the address is LLLLTTTTSS (in octal) where
 *   LLLL is the LDAT index of the pack which physically contains the directory track which contains the address.
 *   TTTT is the track-id relative to the containing LDAT - 0000 is the first directory track on the containing pack
 *      note that this is *not* any indication of where the track is physically located on the pack
 *   SS is the sector number within the track, from 000 to 077.
 */
public class MFDRelativeAddress implements Comparable<MFDRelativeAddress> {

    private long _value;

    public MFDRelativeAddress() {
        _value = 0;
    }

    public MFDRelativeAddress(long value) {
        _value = value;
    }

    public MFDRelativeAddress(MFDRelativeAddress source) {
        _value = source._value;
    }

    public MFDRelativeAddress(final long ldatIndex,
                              final long trackId,
                              final long sectorId) {
        _value = ((ldatIndex & 07777) << 18) | ((trackId & 07777) << 6) | (sectorId & 077);
    }

    public final long getLDATIndex() { return (_value >> 18) & 07777; }
    public final long getTrackId() { return (_value >> 6) & 07777; }
    public final long getSectorId() { return _value & 077; }
    public void increment() { _value = (_value + 1) & 07777777777; }
    public void setLDATIndex(long value) { _value = (value & 0777777) | ((value & 07777) << 18); }
    public void setTrackId(long value) { _value = (value & 07777000077) | ((value & 07777) << 6); }
    public void setSectorId(long value) { _value = (value & 07777777700) | (value & 077); }

    @Override
    public String toString() {
        return String.format("%012o", _value);
    }

    @Override
    public int compareTo(MFDRelativeAddress obj) {
        return Long.compare(_value, obj._value);
    }
}

/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;

/**
 * Describes a buffer for an IO - a single IO may use multiple buffers.
 * See ChannelProgram class
 *
 * ACW format (see AccessControlWord object):
 *      +----------+----------+----------+----------+----------+----------+
 * +00: | INC_DEC  |          |          |          BUFFER_SIZE           |
 *      +----------+----------+----------+----------+----------+----------+
 * +01: |                          BUFFER_ADDRESS                         |
 * +02: |                    (2 word absolute address)                    |
 *      +----------+----------+----------+----------+----------+----------+
 *
 *      INC_DEC: 00 = Increment
 *               01 = No increment or decrement
 *               02 = Decrement
 *               03 = Skip data (no data transferred to storage)
 */
@SuppressWarnings("Duplicates")
public class AccessControlWord extends ArraySlice {

    /**
     * Represents the 'G' field in an Exec IO Packet
     */
    public enum AddressModifier {
        Increment(0),
        NoChange(1),
        Decrement(2),
        SkipData(3),
        InvalidValue(077);

        private final int _code;

        AddressModifier(int code) { _code = code; }

        public int getCode() { return _code; }

        public static AddressModifier getValue(
            final int code
        ) {
            switch (code) {
                case 0:     return Increment;
                case 1:     return NoChange;
                case 2:     return Decrement;
                case 3:     return SkipData;
                default:    return InvalidValue;
            }
        }
    }

    public AccessControlWord(
        final ArraySlice baseArray,
        final int offset
    ) {
        super(baseArray, offset, 3);
    }

    public AddressModifier getAddressModifier() { return AddressModifier.getValue((int) Word36.getS1(get(0))); }
    public AbsoluteAddress getBufferAddress() { return new AbsoluteAddress(this, 1); }
    public int getBufferSize() { return (int) Word36.getH2(get(0)); }
}

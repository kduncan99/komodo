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

    /**
     * Represents an ACW in storage
     * @param baseArray storage ArraySlice
     * @param offset offset from start of baseArray, where this ACW is located
     */
    public AccessControlWord(
        final ArraySlice baseArray,
        final int offset
    ) {
        super(baseArray, offset, 3);
    }

    public AddressModifier getAddressModifier() { return AddressModifier.getValue((int) Word36.getS1(get(0))); }
    public AbsoluteAddress getBufferAddress() { return new AbsoluteAddress(this, 1); }
    public int getBufferSize() { return (int) Word36.getH2(get(0)); }

    /**
     * Update the data in an ArraySlice to produce a valid ACW
     * @param arena defines an arena of memory, possibly the storage from an MSP
     * @param offset offset from the start of the arena, where we place the 3-word ACW
     * @param bufferAddress absolute address of the buffer to be described
     * @param bufferSize size of the indicated buffer, in words
     * @param modifier modifer to be encoded
     */
    public static void populate(
        final ArraySlice arena,
        final int offset,
        final AbsoluteAddress bufferAddress,
        final int bufferSize,
        final AddressModifier modifier
    ) {
        long word0 = ((long)(modifier._code)) << 30;
        word0 |= bufferSize & 0777777;
        arena.set(offset, word0);
        bufferAddress.populate(arena, offset + 1);
    }

    @Override
    public String toString() {
        return String.format("addr:%s size:%d mod:%s ",
                             getBufferAddress().toString(),
                             getBufferSize(),
                             getAddressModifier().toString());
    }
}

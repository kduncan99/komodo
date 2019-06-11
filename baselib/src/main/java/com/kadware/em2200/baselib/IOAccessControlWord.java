/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

import com.kadware.em2200.baselib.exceptions.*;

/**
 * Describes a buffer for an IO - a single IO may use multiple buffers.
 * See IOAccessControlList
 */
public class IOAccessControlWord {

    /**
     * Represents the 'G' field in an Exec IO Packet
     */
    public static enum AddressModifier {
        Increment(0),
        NoChange(1),
        Decrement(2),
        SkipData(3);

        private final int _value;

        AddressModifier(
            final int value
        ) {
            _value = value;
        }
    }

    /**
     * Indicates what should happen on successive word transfers, to the buffer address index
     */
    private final AddressModifier _addressModifier;

    /**
     * Reference to the array of 36-bit values which holds the buffer.
     * This MUST be private - callers are allowed access to this array ONLY through getWord() and setWord()
     */
    private final ArraySlice _array;

    /**
     * Index of the word of _array which is to be returned when the access index is 0.
     * For working forward through the buffer, this is likely to be 0.
     * For working backward through the buffer, this is likely to be one less than the size of the buffer.
     */
    private final int _bufferStart;

    /**
     * Standard constructor
     * <p>
     * @param array                 reference to an ArraySlice containing the IO buffer
     * @param bufferStart           index into the buffer, of the word to be retrieved for accessIndex of zero
     * @param addressModifier       indicates whether to traverse the buffer forward, backward, or not at all
     */
    public IOAccessControlWord(
        final ArraySlice array,
        final int bufferStart,
        final AddressModifier addressModifier
    ) {
        _array = array;
        _bufferStart = bufferStart;
        _addressModifier = addressModifier;
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if ((obj != null) && (obj instanceof IOAccessControlWord)) {
            IOAccessControlWord comp = (IOAccessControlWord)obj;
            if ((_array.equals(comp._array))
                    && (_bufferStart == comp._bufferStart)
                    && (_addressModifier == comp._addressModifier)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public AddressModifier getAddressModifier(
    ) {
        return _addressModifier;
    }

    /**
     * Translates a requested accessIndex to an effectiveIndex, in the context of our address modifier
     * <p>
     * @param accessIndex
     * <p>
     * @return
     */
    private int getEffectiveIndex(
        final int accessIndex
    ) {
        int effectiveIndex = _bufferStart;
        if ((_addressModifier == AddressModifier.Increment)
                || (_addressModifier == AddressModifier.SkipData)) {
            effectiveIndex += accessIndex;
        } else if (_addressModifier == AddressModifier.Decrement) {
            effectiveIndex -= accessIndex;
        }

        if ((effectiveIndex < 0) || (effectiveIndex >= _array.getSize())) {
            throw new InvalidArgumentRuntimeException(String.format("accessIndex is out of range:%d", accessIndex));
        }

        return effectiveIndex;
    }

    /**
     * Retrieves the value of the word referred to by wordIndex, which advances from zero to one less than bufferCount.
     * Our job is to observe the buffer address modifier, and retrieve a value which is {n} words forward from the bufferStart
     * index, or backward, or simply returning the same word value over and over.
     * <p>
     * @param accessIndex
     * <p>
     * @return reference to the particular value requested.
     */
    public long getValue(
        final int accessIndex
    ) {
        return _array.get(getEffectiveIndex(accessIndex));
    }

    /**
     * Retrieves the word referred to by the wordIndex value, which advances from zero to one less than bufferCount.
     * Our job is to observe the buffer address modifier, and retrieve a Word36 value which is {n} words forward from
     * the bufferStart index, or backward, or simply returning the same word over and over.
     * <p>
     * @param accessIndex
     * <p>
     * @return reference to the particular Word36 object requested.
     */
    public Word36 getWord(
        final int accessIndex
    ) {
        return new Word36(getValue(accessIndex));
    }

    /**
     * Writes the content of our array to the provided logger
     * <p>
     * @param logger
     * @param logLevel
     * @param caption
     */
    public void logBuffer(
        final org.apache.logging.log4j.Logger logger,
        final org.apache.logging.log4j.Level logLevel,
        final String caption
    ) {
        _array.logOctal(logger, logLevel, caption);
    }

    /**
     * Sets the value of the word referred to by wordIndex, which advances from zero to one less than bufferCount.
     * Our job is to observe the buffer address modifier, and retrieve a value which is {n} words forward from the bufferStart
     * index, or backward, or simply returning the same word value over and over.
     * <p>
     * @param accessIndex
     * @param value
     */
    public void setValue(
        final int accessIndex,
        final long value
    ) {
        _array.set(getEffectiveIndex(accessIndex), value);
    }

    /**
     * Sets the value of the word referred to by wordIndex, which advances from zero to one less than bufferCount.
     * Our job is to observe the buffer address modifier, and retrieve a value which is {n} words forward from the bufferStart
     * index, or backward, or simply returning the same word value over and over.
     * <p>
     * @param accessIndex
     * @param value
     */
    public void setWord(
        final int accessIndex,
        final Word36 value
    ) {
        setValue(accessIndex, value.getW());
    }

    /**
     * For logging, mostly
     * <p>
     * @return
     */
    @Override
    public String toString(
    ) {
        return String.format("BufferSize=0%o  BufferStart=0%o  Modifier=%s",
                             _array.getSize(),
                             _bufferStart,
                             _addressModifier.toString());
    }
}

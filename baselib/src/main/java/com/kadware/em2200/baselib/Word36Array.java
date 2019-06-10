/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

import com.kadware.em2200.baselib.exceptions.*;
import org.apache.logging.log4j.*;

/**
 * Manages a semi-opaque array of 36=bit values.
 * This exists because a real array of Word36 objects would take a ridiculous amount of space, considering how many of them
 * we would have laying about, and how big they would be.
 * We keep all the values in an array of intrinsic long's, and translate in and out as necessary.
 * As long as the values are stored here, they're in ones-complement format.
 */
public class Word36Array {

    final long[] _array;

    /**
     * Standard constructor
     * @param size requesed size of the array
     */
    public Word36Array(
        final int size
    ) {
        _array = new long[size > 0 ? size : 0];
    }

    /**
     * Constructor which uses a pre-existing array of long's
     * @param array reference to base array
     */
    public Word36Array(
        final long[] array
    ) {
        _array = array;
        for (int ax = 0; ax < _array.length; ++ax) {
            _array[ax] &= OnesComplement.BIT_MASK_36;
        }
    }

    /**
     * Constructor which pulls data from a true array of Word36 objects
     * @param array reference to base array
     */
    public Word36Array(
        final Word36[] array
    ) {
        _array = new long[array.length];
        for (int ax = 0; ax < _array.length; ++ax) {
            _array[ax] = array[ax].getW();
        }
    }

    /**
     * tests for equality of content
     * @param obj comparison object
     * @return value
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof Word36Array) {
            Word36Array comp = (Word36Array)obj;
            if (getArraySize() == comp.getArraySize()) {
                for (int ax = 0; ax < getArraySize(); ++ax) {
                    if (getValue(ax) != comp.getValue(ax)) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the size of the array
     * All methods in this class which need to do this, should invoke this method instead of accessing _array directly,
     * in case we are being overloaded by a subclass which redefines the geometry of the underlying array.
     *     DO NOT REMOVE THIS - IT IS OVERRIDDEN BY Word36ArraySlice
     * @return value
     */
    public int getArraySize() { return _array.length; }

    /**
     * Retrieves the 36-bit value indicated by the index from this Array.
     * All methods in this class which need to do this, should invoke this method instead of accessing _array directly,
     * in case we are being overloaded by a subclass which redefines the geometry of the underlying array.
     *     DO NOT REMOVE THIS - IT IS OVERRIDDEN BY Word36ArraySlice
     * @param index index of interest
     * @return value
     */
    public long getValue(int index) { return _array[index]; }

    /**
     * Indicates the offset of this array's view of the base array.
     * Because this is *not* a slice, the offset is zero.
     * Slices should override this appropriately.
     */
    public int getOffset() { return 0; }

    /**
     * Retrieves a copy of the underlying values for this array
     * @return copy of the array (or slice, for the Word36ArraySlice object)
     */
    public final long[] getValues() {
        long[] newArray = new long[getArraySize()];
        for (int ax = 0; ax < _array.length; ++ax) {
            newArray[ax] = getValue(ax);
        }
        return newArray;
    }

    /**
     * Retrieves a Word36 object representing a particular value within the Array
     * All methods in this class which need to do this, should invoke this method instead of accessing _array directly,
     * in case we are being overloaded by a subclass which redefines the geometry of the underlying array.
     *     DO NOT REMOVE THIS - IT IS OVERRIDDEN BY Word36ArraySlice
     * @param index index of interest
     * @return value
     */
    public Word36 getWord36(
        final int index
    ) {
        if ((index < 0) || (index >= getArraySize())) {
            throw new InvalidArgumentRuntimeException(String.format("index is out of range:%d", index));
        }

        return new Word36(_array[index]);
    }

    /**
     * Sets the value of a particular item in this array
     * All methods in this class which need to do this, should invoke this method instead of accessing _array directly,
     * in case we are being overloaded by a subclass which redefines the geometry of the underlying array.
     *     DO NOT REMOVE THIS - IT IS OVERRIDDEN BY Word36ArraySlice
     * @param index index of interest
     * @param value value
     */
    public void setValue(
        final int index,
        final long value
    ) {
        _array[index] = value & OnesComplement.BIT_MASK_36;
    }

    /**
     * Sets the value of a particular item in this array
     * All methods in this class which need to do this, should invoke this method instead of accessing _array directly,
     * in case we are being overloaded by a subclass which redefines the geometry of the underlying array.
     *     DO NOT REMOVE THIS - IT IS OVERRIDDEN BY Word36ArraySlice
     * @param index index of interest
     * @param value value
     */
    public void setWord36(
        final int index,
        final Word36 value
    ) {
        _array[index] = value.getW();
    }

    /**
     * Loads values from a subset of a given source Word35Array into some portion of this array
     * @param destinationOffset where we start populating this object's array
     * @param source source array
     */
    public void load(
        final int destinationOffset,
        final Word36Array source
    ) {
        if (destinationOffset + source.getArraySize() > getArraySize()) {
            throw new InvalidArgumentRuntimeException(String.format("destOffset(%d) + sourceLen(%d) > array size(%d)",
                                                             destinationOffset,
                                                             source.getArraySize(),
                                                             getArraySize()));
        }

        for (int sx = 0; sx < source.getArraySize(); ++sx) {
            _array[destinationOffset + sx] = source.getValue(sx);
        }
    }

    /**
     * Loads values from a subset of a given source array into some portion of this array
     * @param destinationOffset where we start populating this object's array
     * @param source source array
     * @param sourceOffset offset into the source array from which we start taking values
     * @param sourceLength number of values to be loaded
     */
    public void load(
        final int destinationOffset,
        final long[] source,
        final int sourceOffset,
        final int sourceLength
    ) {
        if ((destinationOffset + sourceLength > getArraySize() || (sourceOffset < 0))) {
            throw new InvalidArgumentRuntimeException(String.format("destOffset(%d) + sourceLen(%d) > array size(%d)",
                                                             destinationOffset,
                                                             sourceLength,
                                                             getArraySize()));
        }

        for (int x = 0; x < sourceLength; ++x) {
            _array[destinationOffset + x] = source[sourceOffset + x];
        }
    }

    /**
     * Loads values from the given source array into some portion of this array
     * @param destinationOffset where we start populating this object's array
     * @param source source array
     */
    public void load(
        final int destinationOffset,
        final long[] source
    ) {
        load(destinationOffset, source, 0, source.length);
    }

    /**
     * Logs the contents of a particular buffer of 36-bit values in multi-format mode...
     * That is, four words per line in octal, then fieldata, then ASCII, with offset indicators on the left.
     * @param logger destination for the output
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     */
    public void logBufferMultiFormat(
        final Logger logger,
        final Level logLevel,
        final String caption
    ) {
        if (!caption.isEmpty()) {
            logger.printf(logLevel, "--[ %s ]--", caption);
        }

        int bufferIndex = 0;
        int remainingWords = getArraySize();
        final int wordsPerRow = 4;
        for (int rowIndex = 0;
             remainingWords > 0;
             rowIndex += wordsPerRow, bufferIndex += wordsPerRow, remainingWords -= wordsPerRow) {
            //  Get a subset of the buffer
            int wordBufferSize = remainingWords > wordsPerRow ? wordsPerRow : remainingWords;
            Word36ArraySlice subset = new Word36ArraySlice(this, bufferIndex, wordBufferSize);

            //  Build octal string
            StringBuilder octalBuilder = new StringBuilder();
            octalBuilder.append(subset.toOctal(true));
            for (int wx = subset.getArraySize(); wx < wordsPerRow; ++wx) {
                octalBuilder.append("             ");
            }

            //  Build fieldata string
            StringBuilder fieldataBuilder = new StringBuilder();
            fieldataBuilder.append(subset.toFieldata(true));
            for (int wx = subset.getArraySize(); wx < wordsPerRow; ++wx) {
                fieldataBuilder.append("       ");
            }

            //  Build ASCII string
            StringBuilder asciiBuilder = new StringBuilder();
            asciiBuilder.append(subset.toASCII(true));
            for (int wx = subset.getArraySize(); wx < wordsPerRow; ++wx) {
                asciiBuilder.append("     ");
            }

            //  Log the output
            logger.printf(logLevel, String.format("%06o:%s  %s  %s",
                                                  rowIndex,
                                                  octalBuilder.toString(),
                                                  fieldataBuilder.toString(),
                                                  asciiBuilder.toString()));
        }
    }

    /**
     * Logs the contents of a particular buffer of 36-bit values in octal, seven words per line.
     * @param logger destination for the output
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     */
    public void logBufferOctal(
        final Logger logger,
        final Level logLevel,
        final String caption
    ) {
        if (!caption.isEmpty()) {
            logger.printf(logLevel, "--[ %s ]--", caption);
        }

        int bufferIndex = 0;
        int remainingWords = getArraySize();
        for (int rowIndex = 0; remainingWords > 0; rowIndex += 7, bufferIndex += 7, remainingWords -= 7) {
            //  Get a subset of the buffer
            Word36ArraySlice subset = new Word36ArraySlice(this, bufferIndex, 7);

            //  Build octal string
            String octalString = subset.toOctal(true);

            //  Log the output
            logger.printf(logLevel, String.format("%06o:%s", rowIndex, octalString));
        }
    }


    //  Formatting for display -----------------------------------------------------------------------------------------------------

    /**
     * Creates a string containing the representation of this buffer in consecutive 4-character strings,
     * possibly delimited by spaces.
     * @param delimitFlag true to delimit between words with a blank character
     * @return display string
     */
    public String toASCII(
        final boolean delimitFlag
    ) {
        StringBuilder builder = new StringBuilder();

        for (int wx = 0; wx < getArraySize(); ++wx) {
            if (delimitFlag && (wx != 0)) {
                builder.append(" ");
            }
            builder.append(Word36.toASCII(getValue(wx)));
        }

        return builder.toString();
    }

    /**
     * Creates a string containing the representation of this buffer in consecutive 6-character strings,
     * possibly delimited by spaces.
     * @param delimitFlag true to delimit between words with a blank character
     * @return display string
     */
    public String toFieldata(
        final boolean delimitFlag
    ) {
        StringBuilder builder = new StringBuilder();

        for (int wx = 0; wx < getArraySize(); ++wx) {
            if (delimitFlag && (wx != 0)) {
                builder.append(" ");
            }
            builder.append(Word36.toFieldata(getValue(wx)));
        }

        return builder.toString();
    }

    /**
     * Creates a string containing the representation of this buffer in consecutive 12-digit octal strings,
     * possibly delimited by spaces.
     * @param delimitFlag true to delimit between words with a blank character
     * @return display string
     */
    public String toOctal(
        final boolean delimitFlag
    ) {
        StringBuilder builder = new StringBuilder();

        for (int wx = 0; wx < getArraySize(); ++wx) {
            if (delimitFlag && (wx != 0)) {
                builder.append(" ");
            }
            builder.append(Word36.toOctal(getValue(wx)));
        }

        return builder.toString();
    }


    //  text conversions -----------------------------------------------------------------------------------------------------------

    /**
     * Populates this object with quarter-words derived from the ASCII characters in the source string.
     * The last word is padded with ascii spaces if so needed.
     * @param source string to be converted
     * @return converted data
     */
    public static Word36Array stringToWord36ASCII(
        final String source
    ) {
        int words = source.length() / 4;
        if (source.length() % 4 > 0) {
            words++;
        }

        Word36[] temp = new Word36[words];
        int tx = 0;
        for (int sx = 0; sx < source.length(); sx += 4) {
            temp[tx++] = Word36.stringToWord36ASCII(source.substring(sx));
        }

        return new Word36Array(temp);
    }

    /**
     * Populates this object with sixth-words representing the fieldata characters in the source string.
     * The last word is padded with fieldata spaces if so needed.
     * @param source string to be converted
     * @return converted data
     */
    public static Word36Array stringToWord36Fieldata(
        final String source
    ) {
        int words = source.length();
        if (source.length() % 6 > 0) {
            words++;
        }

        Word36[] temp = new Word36[words];
        int tx = 0;
        for (int sx = 0; sx < source.length(); sx += 6) {
            temp[tx++] = Word36.stringToWord36Fieldata(source.substring(sx));
        }

        return new Word36Array(temp);
    }


    //  Word36 <-> byte conversions ------------------------------------------------------------------------------------------------

    /**
     * Packs this entire array as pairs of 36-bit words into groups of 9 bytes.
     * We highly suggest that this array contains an even number of words, for calling this method.
     * @param destination array where we place byte output
     * @param destinationOffset index of byte within the destination, where we begin placing packed output
     * @return number of COMPLETE words packed - will be less than getArraySize() if we hit the end of the destination buffer
     */
    public int pack(
        byte[] destination,
        final int destinationOffset
    ) {
        if (destinationOffset >= destination.length) {
            return 0;
        }

        int wordsLeft = getArraySize();
        int sx = 0;
        int bytesLeft = destination.length - destinationOffset;
        int dx = destinationOffset;
        int count = 0;
        int partial = 0;

        while ((wordsLeft > 0) && (bytesLeft > 0)) {
            switch (partial) {
                case 0:
                    destination[dx++] = (byte)(getValue(sx) >> 28);
                    --bytesLeft;
                    break;

                case 1:
                    destination[dx++] = (byte)(getValue(sx) >> 20);
                    --bytesLeft;
                    break;

                case 2:
                    destination[dx++] = (byte)(getValue(sx) >> 12);
                    --bytesLeft;
                    break;

                case 3:
                    destination[dx++] = (byte)(getValue(sx) >> 4);
                    --bytesLeft;
                    break;

                case 4:
                    destination[dx] = (byte)((getValue(sx) & 017) << 4);
                    --bytesLeft;
                    ++sx;
                    --wordsLeft;
                    ++count;
                    if (wordsLeft > 0) {
                        destination[dx] |= (byte)(getValue(sx) >> 32);
                    }
                    ++dx;
                    break;

                case 5:
                    destination[dx++] = (byte)(getValue(sx) >> 24);
                    --bytesLeft;
                    break;

                case 6:
                    destination[dx++] = (byte)(getValue(sx) >> 16);
                    --bytesLeft;
                    break;

                case 7:
                    destination[dx++] = (byte)(getValue(sx) >> 8);
                    --bytesLeft;
                    break;

                case 8:
                    destination[dx++] = (byte)getValue(sx);
                    --bytesLeft;
                    ++sx;
                    --wordsLeft;
                    ++count;
                    break;
            }

            ++partial;
            if (partial == 9) {
                partial = 0;
            }
        }

        return count;
    }

    /**
     * Packs this entire array as pairs of 36-bit words into groups of 9 bytes.
     * We highly suggest that this array contains an even number of words, for calling this method.
     * Convenience wrapper for the above method.
     * @param destination array where we place byte output
     * @return number of COMPLETE words packed - will be less than getArraySize() if we hit the end of the destination buffer
     */
    public int pack(
        byte[] destination
    ) {
        return pack(destination, 0);
    }

    /**
     * unpacks groups of 9-bytes of data into 36-bit word pairs into this array.
     * @param source array containing byte input
     * @param sourceOffset index of first byte to be converted
     * @param sourceCount number of bytes to be converted (should be divisible by 9)
     * @return number of bytes unpacked - will be less than sourceCount if we run out of space in this object
     */
    public int unpack(
        final byte[] source,
        final int sourceOffset,
        final int sourceCount
    ) {
        if (sourceOffset >= source.length) {
            return 0;
        }

        int bytesLeft = source.length - sourceOffset;
        if (bytesLeft > sourceCount) {
            bytesLeft = sourceCount;
        }

        int count = 0;
        int sx = sourceOffset;
        int dx = 0;
        int wordsLeft = getArraySize();
        int partial = 0;

        while ((bytesLeft > 0) && (wordsLeft > 0)) {
            switch (partial) {
                case 0:
                    setValue(dx, (getValue(dx) & 0_001777_777777L) | ((long)source[sx++] << 28));
                    ++count;
                    --bytesLeft;
                    break;

                case 1:
                    setValue(dx, (getValue(dx) & 0_776003_777777L) | ((long)source[sx++] << 20));
                    ++count;
                    --bytesLeft;
                    break;

                case 2:
                    setValue(dx, (getValue(dx) & 0_777774_007777L) | ((long)source[sx++] << 12));
                    ++count;
                    --bytesLeft;
                    break;

                case 3:
                    setValue(dx, (getValue(dx) & 0_777777_770017L) | ((long)source[sx++] << 4));
                    ++count;
                    --bytesLeft;
                    break;

                case 4:
                    setValue(dx, (getValue(dx) & 0_777777_777760L) | ((long)source[sx] >> 4));
                    ++dx;
                    --wordsLeft;
                    if (wordsLeft > 0) {
                        setValue(dx, (getValue(dx) & 0_037777_777777L) | (((long)source[sx] & 017) << 32));
                        ++count;
                    }
                    ++sx;
                    --bytesLeft;
                    break;

                case 5:
                    setValue(dx, (getValue(dx) & 0_740077_777777L) | ((long)source[sx++] << 24));
                    ++count;
                    --bytesLeft;
                    break;

                case 6:
                    setValue(dx, (getValue(dx) & 0_777700_177777L) | ((long)source[sx++] << 16));
                    ++count;
                    --bytesLeft;
                    break;

                case 7:
                    setValue(dx, (getValue(dx) & 0_777777_600377L) | ((long)source[sx++] << 8));
                    ++count;
                    --bytesLeft;
                    break;

                case 8:
                    setValue(dx, (getValue(dx) & 0_777777_777400L) | (long)source[sx++]);
                    ++count;
                    --bytesLeft;
                    ++dx;
                    --wordsLeft;
                    break;
            }

            ++partial;
            if (partial == 9) {
                partial = 0;
            }
        }

        return count;
    }

    /**
     * unpacks groups of 9-bytes of data into 36-bit word pairs into this array.
     * Convenience wrapper for the method above.
     * @param source array containing byte input
     * @return number of bytes unpacked - will be less than sourceCount if we run out of space in this object
     */
    public int unpack(
        final byte[] source
    ) {
        return unpack(source, 0, source.length);
    }
}

/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import com.bearsnake.komodo.baselib.exceptions.InvalidArgumentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * Represents a subset of a base array of elements of type T
 */
public class ArraySlice {

    private static final Logger LOGGER = LogManager.getLogger(ArraySlice.class);

    public final long[] _array;     //  base array of which this slice is a (possibly complete) subset
    public final int _length;       //  length of this array (must be <= length of base array)
    public final int _offset;       //  offset into the base array, at which this slice begins
                                    //      _length + _offset must not exceed the range of the base array

    /**
     * Constructor to produce a slice of a full array
     * @param array base array
     */
    public ArraySlice(
        final long[] array
    ) {
        _array = array;
        _offset = 0;
        _length = array.length;
    }

    /**
     * Constructor to produce a slice representing a subset of a full array
     * @param array base array
     * @param offset offset into the base array at which point this subset begins
     * @param length length of this subset
     */
    public ArraySlice(
        final long[] array,
        final int offset,
        final int length
    ) {
        if ((offset + length > array.length) || (offset < 0) || (length < 0)) {
            String msg = String.format("Invalid arguments array size=%d requested offset=%d length=%d",
                                       array.length,
                                       offset,
                                       length);
            throw new InvalidArgumentException(msg);
        }

        _array = array;
        _offset = offset;
        _length = length;
    }

    /**
     * Constructor to produce a slice representing a subset of another slice.
     * @param baseSlice base slice
     * @param offset offset into the slice's subset of the base array, at which this slice's subset begins
     * @param length length of this subset
     */
    public ArraySlice(
        final ArraySlice baseSlice,
        final int offset,
        final int length
    ) {
        if ((offset + length > baseSlice._length) || (offset < 0) || (length < 0)) {
            String msg = String.format("Invalid arguments baseSliceSize=%d requestedOffset=%d length=%d",
                                       baseSlice._length,
                                       offset,
                                       length);
            throw new InvalidArgumentException(msg);
        }

        _array = baseSlice._array;
        _offset = offset + baseSlice._offset;
        _length = length;
    }

    /**
     * Clears the slice to zero
     */
    public void clear() {
        for (int ax = _offset; ax < _offset + _length; ++ax) {
            _array[ax] = 0;
        }
    }

    /**
     * Creates a new ArraySlice of the indicated size, containing a copy of the content of this object.
     * If the requested size is larger, the additional content is zeros.
     * If the requested size is smaller, the extra space from the original is discarded.
     * @param newSize new size to be established
     * @return newly-allocated ArraySlice object
     */
    public ArraySlice copyOf(
        final int newSize
    ) {
        return new ArraySlice(Arrays.copyOf(_array, newSize));
    }

    /**
     * For debugging
     */
    public void dump() {
        int ax = _offset;
        while (ax % 8 > 0) { --ax; }
        int alimit = _length + _offset;
        while (alimit % 8 > 0) { ++alimit; }

        System.out.println("ArraySlice content:");
        while (ax < alimit) {
            StringBuilder sb = new StringBuilder();
            for (int ay = 0; ay < 8; ++ay) {
                if ((ax + ay < _offset) || (ax + ay >= _length + _offset)) {
                    sb.append("............  ");
                } else {
                    sb.append(String.format("%012o  ", _array[ax + ay]));
                }
            }
            System.out.println(sb.toString());
            ax += 8;
        }
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof ArraySlice as) {
            if (as._length == _length) {
                for (int objx = as._offset, thisx = _offset, x = 0; x < as._length; ++objx, ++thisx, ++x) {
                    if (as._array[objx] != _array[thisx]) {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the value at the given index
     * @param index of the value
     * @return the value
     */
    public long get(
        final int index
    ) {
        if ((index < 0) || (index >= _length)) {
            throw new RuntimeException(String.format("Invalid index=%d slice length=%d", index, _length));
        }

        return _array[index + _offset];
    }

    // partial word getters
    public long getH1(final int index) { return Word36.getH1(get(index)); }
    public long getH2(final int index) { return Word36.getH2(get(index)); }
    public long getQ1(final int index) { return Word36.getQ1(get(index)); }
    public long getQ2(final int index) { return Word36.getQ2(get(index)); }
    public long getQ3(final int index) { return Word36.getQ3(get(index)); }
    public long getQ4(final int index) { return Word36.getQ4(get(index)); }
    public long getS1(final int index) { return Word36.getS1(get(index)); }
    public long getS2(final int index) { return Word36.getS2(get(index)); }
    public long getS3(final int index) { return Word36.getS3(get(index)); }
    public long getS4(final int index) { return Word36.getS4(get(index)); }
    public long getS5(final int index) { return Word36.getS5(get(index)); }
    public long getS6(final int index) { return Word36.getS6(get(index)); }
    public long getT1(final int index) { return Word36.getT1(get(index)); }
    public long getT2(final int index) { return Word36.getT2(get(index)); }
    public long getT3(final int index) { return Word36.getT3(get(index)); }

    /**
     * Create a new array representing (but not backed by) the values of this subset
     * @return new array
     */
    public long[] getAll() {
        long[] result = new long[_length];
        for (int ax = _offset, rx = 0; rx < _length; ++ax, ++rx) {
            result[rx] = _array[ax];
        }
        return result;
    }

    /**
     * Getter
     * @return size of this slice
     */
    public int getSize() {
        return _length;
    }

    @Override
    public int hashCode(
    ) {
        int result = _length;
        for (int ax = 0; (ax < 8) && (ax < _length); ++ax) {
            result ^= _array[_offset + ax];
        }
        return result;
    }

    /**
     * Loads values from the source array to the end of the source array, or to the end of this subset of the base array,
     * whichever comes first.
     * @param source source array
     */
    public void load(
        final long[] source
    ) {
        int slimit = Math.min(source.length, _length);
        for (int sx = 0, ax = _offset; sx < slimit; ++sx, ++ax) {
            _array[ax] = source[sx];
        }
    }

    /**
     * Loads values from a subset of a source array into this slice at the indicated index into the slice
     * @param source source array
     * @param sourceIndex index into source array of first value to be loaded
     * @param sourceLength number of values to be loaded
     * @param destinationIndex index into destination slice of first value to be stored
     */
    public void load(
        final long[] source,
        final int sourceIndex,
        final int sourceLength,
        final int destinationIndex
    ) {
        if (sourceIndex + sourceLength > source.length) {
            throw new RuntimeException(
                String.format("Invalid parameter source array length:%d source index:%d source length:%d",
                              source.length,
                              sourceIndex,
                              sourceLength));
        }

        if (destinationIndex + sourceLength > _length) {
            throw new RuntimeException(
                String.format("Invalid parameter slice length:%d destination index:%d source length:%d",
                              _length,
                              destinationIndex,
                              sourceLength));
        }

        int slimit = sourceIndex + sourceLength;
        for (int sx = sourceIndex, ax = _offset + destinationIndex; sx < slimit; ++sx, ++ax) {
            _array[ax] = source[sx];
        }
    }

    /**
     * Loads values from the source array to the end of the source array, or to the end of this subset of the base array,
     * whichever comes first.
     * @param source source array slice
     */
    public void load(
        final ArraySlice source,
        final int destinationIndex
    ) {
        if (destinationIndex + source._length > _length) {
            throw new RuntimeException(
                String.format("Invalid parameter source length:%d destination index:%d destination length:%d",
                              source._length,
                              destinationIndex,
                              _length));
        }

        for (int sx = source._offset, dx = _offset + destinationIndex, x = 0; x < source._length; ++sx, ++dx, ++x) {
            _array[dx] = source._array[sx];
        }
    }

    /**
     * Loads values from a subset of a source array into this slice at the indicated index into the slice
     * @param source source array
     * @param sourceIndex index into source array of first value to be loaded
     * @param sourceLength number of values to be loaded
     * @param destinationIndex index into destination slice of first value to be stored
     */
    public void load(
        final ArraySlice source,
        final int sourceIndex,
        final int sourceLength,
        final int destinationIndex
    ) {
        load(source._array, source._offset + sourceIndex, sourceLength, destinationIndex);
    }

    /**
     * Logs the contents of a particular buffer of 36-bit values in multi-format mode...
     * That is, four words per line in octal, then fieldata, then ASCII, with offset indicators on the left.
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     */
    public void logMultiFormat(
        final Level logLevel,
        final String source,
        final String caption
    ) {
        if (!caption.isEmpty()) {
            LOGGER.log(logLevel,  "--[ {}:{} ]--", source, caption);
        }

        int bufferIndex = 0;
        int remainingWords = _length;
        final int wordsPerRow = 4;
        for (int rowIndex = 0;
             remainingWords > 0;
             rowIndex += wordsPerRow, bufferIndex += wordsPerRow, remainingWords -= wordsPerRow) {
            //  Get a subset of the buffer
            int wordBufferSize = Math.min(remainingWords, wordsPerRow);
            ArraySlice subset = new ArraySlice(this, bufferIndex, wordBufferSize);

            //  Build octal string
            StringBuilder octalBuilder = new StringBuilder();
            octalBuilder.append(subset.toOctal(true));
            octalBuilder.append("             ".repeat(Math.max(0, wordsPerRow - subset.getSize())));

            //  Build fieldata string
            StringBuilder fieldataBuilder = new StringBuilder();
            fieldataBuilder.append(subset.toFieldata(true));
            fieldataBuilder.append("       ".repeat(Math.max(0, wordsPerRow - subset.getSize())));

            //  Build ASCII string
            StringBuilder asciiBuilder = new StringBuilder();
            asciiBuilder.append(subset.toASCII(true));
            asciiBuilder.append("     ".repeat(Math.max(0, wordsPerRow - subset.getSize())));

            //  Log the output
            LOGGER.log(logLevel, String.format("%06o:%s  %s  %s", rowIndex, octalBuilder, fieldataBuilder, asciiBuilder));
        }
    }

    /**
     * Logs the contents of a particular buffer of 36-bit values in octal, seven words per line.
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param source module, class or other identifier
     * @param caption description of the log data.  No caption is produced if this value is empty.
     */
    public void logOctal(
        final Level logLevel,
        final String source,
        final String caption
    ) {
        if (!caption.isEmpty()) {
            LOGGER.log(logLevel,  "--[ {}:{} ]--", source, caption);
        }

        int bufferIndex = 0;
        int remainingWords = _length;
        for (int rowIndex = 0; remainingWords > 0; rowIndex += 7, bufferIndex += 7, remainingWords -= 7) {
            //  Get a subset of the buffer
            ArraySlice subset = new ArraySlice(this, bufferIndex, 7);

            //  Build octal string
            String octalString = subset.toOctal(true);

            //  Log the output
            LOGGER.log(logLevel, String.format("%06o:%s", rowIndex, octalString));
        }
    }

    /**
     * Packs this entire array as pairs of 36-bit words into groups of 9 bytes.
     * We highly suggest that this array contains an even number of words, for calling this method.
     * @param destination array where we place byte output
     * @param destinationOffset index of byte within the destination, where we begin placing packed output
     * @param skipOnSectorBoundary true if the byte data is organized as 28 words packed into 128 bytes (with 2 bytes of slop at the end)
     * @return number of COMPLETE words packed - will be less than getArraySize() if we hit the end of the destination buffer
     */
    public int pack(
        byte[] destination,
        final int destinationOffset,
        final boolean skipOnSectorBoundary
    ) {
        if (destinationOffset >= destination.length) {
            return 0;
        }

        int wordsLeft = _length;
        int sx = _offset;
        int bytesLeft = destination.length - destinationOffset;
        int dx = destinationOffset;
        int count = 0;
        int partial = 0;

        while ((wordsLeft > 0) && (bytesLeft > 0)) {
            if (skipOnSectorBoundary && (dx % 128 == 126)) {
                dx += 2;
            }
            switch (partial) {
                case 0:
                    destination[dx++] = (byte)(get(sx) >> 28);
                    --bytesLeft;
                    break;

                case 1:
                    destination[dx++] = (byte)(get(sx) >> 20);
                    --bytesLeft;
                    break;

                case 2:
                    destination[dx++] = (byte)(get(sx) >> 12);
                    --bytesLeft;
                    break;

                case 3:
                    destination[dx++] = (byte)(get(sx) >> 4);
                    --bytesLeft;
                    break;

                case 4:
                    destination[dx] = (byte)((get(sx) & 0_17) << 4);
                    --bytesLeft;
                    ++sx;
                    --wordsLeft;
                    ++count;
                    if (wordsLeft > 0) {
                        destination[dx] |= (byte)(get(sx) >> 32);
                    }
                    ++dx;
                    break;

                case 5:
                    destination[dx++] = (byte)(get(sx) >> 24);
                    --bytesLeft;
                    break;

                case 6:
                    destination[dx++] = (byte)(get(sx) >> 16);
                    --bytesLeft;
                    break;

                case 7:
                    destination[dx++] = (byte)(get(sx) >> 8);
                    --bytesLeft;
                    break;

                case 8:
                    destination[dx++] = (byte)get(sx);
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

    public int pack(
        byte[] destination,
        final int destinationOffset
    ) {
        return pack(destination, destinationOffset, false);
    }

    public int pack(
        byte[] destination
    ) {
        return pack(destination, 0);
    }

    /**
     * Places quarter words, sans MSBit, into the destination byte array beginning at the given offset.
     * @param destination array where we store bytes
     * @param destinationOffset offset within the array at which we begin storing bytes
     * @return number of bytes written
     */
    public int packQuarterWords(
        byte[] destination,
        final int destinationOffset
    ) {
        int sx = _offset;
        int dx = destinationOffset;
        int qwx = 0;
        int count = 0;
        boolean stop = false;
        int wordsLeft = _length;
        while ((wordsLeft > 0) && (dx < destination.length) && (!stop)) {
            switch (qwx++) {
                case 0:
                    if ((Word36.getQ1(_array[sx]) & 0_400) != 0) {
                        stop = true;
                    } else {
                        destination[dx++] = (byte) (Word36.getQ1(_array[sx]) & 0xFF);
                        ++count;
                    }
                    break;

                case 1:
                    if ((Word36.getQ2(_array[sx]) & 0_400) != 0) {
                        stop = true;
                    } else {
                        destination[dx++] = (byte) (Word36.getQ2(_array[sx]) & 0xFF);
                        ++count;
                    }
                    break;

                case 2:
                    if ((Word36.getQ3(_array[sx]) & 0_400) != 0) {
                        stop = true;
                    } else {
                        destination[dx++] = (byte) (Word36.getQ3(_array[sx]) & 0xFF);
                        ++count;
                    }
                    break;

                case 3:
                    if ((Word36.getQ4(_array[sx]) & 0_400) != 0) {
                        stop = true;
                    } else {
                        destination[dx++] = (byte) (Word36.getQ4(_array[sx]) & 0xFF);
                        ++count;
                        ++sx;
                        --wordsLeft;
                        qwx = 0;
                    }
                    break;

            }
        }

        return count;
    }

    public int packQuarterWords(
        byte[] destination
    ) {
        return packQuarterWords(destination, 0);
    }

    /**
     * Places sixth words into the destination byte array beginning at the given offset.
     * @param destination array where we store bytes
     * @param destinationOffset offset within the array at which we begin storing bytes
     * @return number of bytes written
     */
    public int packSixthWords(
        byte[] destination,
        final int destinationOffset
    ) {
        int sx = _offset;
        int dx = destinationOffset;
        int count = 0;
        int swx = 0;
        int wordsLeft = _length;
        while ((sx < _length) && (dx < destination.length)) {
            switch (swx++) {
                case 0:
                    destination[dx++] = (byte) (Word36.getS1(_array[sx]) & 0x3F);
                    ++count;
                    break;

                case 1:
                    destination[dx++] = (byte) (Word36.getS2(_array[sx]) & 0x3F);
                    ++count;
                    break;

                case 2:
                    destination[dx++] = (byte) (Word36.getS3(_array[sx]) & 0x3F);
                    ++count;
                    break;

                case 3:
                    destination[dx++] = (byte) (Word36.getS4(_array[sx]) & 0x3F);
                    ++count;
                    break;

                case 4:
                    destination[dx++] = (byte) (Word36.getS5(_array[sx]) & 0x3F);
                    ++count;
                    break;

                case 6:
                    destination[dx++] = (byte) (Word36.getS6(_array[sx]) & 0x3F);
                    ++count;
                    ++sx;
                    --wordsLeft;
                    swx = 0;
                    break;

            }
        }

        return count;
    }

    public int packSixthWords(
        byte[] destination
    ) {
        return packSixthWords(destination, 0);
    }

    /**
     * Sets a value into the array at the given index, which is offset further by this subset's offset
     * @param index index into the subset at which the value should be stored
     * @param value value to be stored
     */
    public ArraySlice set(
        final int index,
        final long value
    ) {
        if ((index < 0) || (index >= _length)){
            throw new RuntimeException(String.format("Invalid index=%d slice length=%d", index, _length));
        }

        _array[index + _offset] = value;
        return this;
    }

    // partial word setters
    public ArraySlice setH1(final int index, final long partial) { set(index, Word36.setH1(get(index), partial)); return this; }
    public ArraySlice setH2(final int index, final long partial) { set(index, Word36.setH2(get(index), partial)); return this; }
    public ArraySlice setQ1(final int index, final long partial) { set(index, Word36.setQ1(get(index), partial)); return this; }
    public ArraySlice setQ2(final int index, final long partial) { set(index, Word36.setQ2(get(index), partial)); return this; }
    public ArraySlice setQ3(final int index, final long partial) { set(index, Word36.setQ3(get(index), partial)); return this; }
    public ArraySlice setQ4(final int index, final long partial) { set(index, Word36.setQ4(get(index), partial)); return this; }
    public ArraySlice setS1(final int index, final long partial) { set(index, Word36.setS1(get(index), partial)); return this; }
    public ArraySlice setS2(final int index, final long partial) { set(index, Word36.setS2(get(index), partial)); return this; }
    public ArraySlice setS3(final int index, final long partial) { set(index, Word36.setS3(get(index), partial)); return this; }
    public ArraySlice setS4(final int index, final long partial) { set(index, Word36.setS4(get(index), partial)); return this; }
    public ArraySlice setS5(final int index, final long partial) { set(index, Word36.setS5(get(index), partial)); return this; }
    public ArraySlice setS6(final int index, final long partial) { set(index, Word36.setS6(get(index), partial)); return this; }
    public ArraySlice setT1(final int index, final long partial) { set(index, Word36.setT1(get(index), partial)); return this; }
    public ArraySlice setT2(final int index, final long partial) { set(index, Word36.setT2(get(index), partial)); return this; }
    public ArraySlice setT3(final int index, final long partial) { set(index, Word36.setT3(get(index), partial)); return this; }

    /**
     * Produces a new object with quarter-words derived from the ASCII characters in the source string.
     * The last word is padded with ascii spaces if so needed.
     * @param source string to be converted
     * @return converted data
     */
    public static ArraySlice stringToWord36ASCII(
        final String source
    ) {
        int words = source.length() / 4;
        if (source.length() % 4 > 0) {
            words++;
        }

        long[] temp = new long[words];
        int tx = 0;
        for (int sx = 0; sx < source.length(); sx += 4) {
            temp[tx++] = Word36.stringToWordASCII(source.substring(sx));
        }

        return new ArraySlice(temp);
    }

    /**
     * Populates this object with sixth-words representing the fieldata characters in the source string.
     * The last word is padded with fieldata spaces if so needed.
     * @param source string to be converted
     * @return converted data
     */
    public static ArraySlice stringToWord36Fieldata(
        final String source
    ) {
        int words = source.length() / 6;
        if (source.length() % 6 > 0) {
            words++;
        }

        long[] temp = new long[words];
        int tx = 0;
        for (int sx = 0; sx < source.length(); sx += 6) {
            temp[tx++] = Word36.stringToWordFieldata(source.substring(sx));
        }

        return new ArraySlice(temp);
    }

    /**
     * Creates a string containing the representation of this buffer in consecutive 4-character strings
     * @return display string
     */
    public String toASCII() {
        StringBuilder builder = new StringBuilder();
        for (int wx = 0; wx < getSize(); ++wx) {
            builder.append(Word36.toStringFromASCII(get(wx)));
        }
        return builder.toString();
    }

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

        for (int wx = 0; wx < getSize(); ++wx) {
            if (delimitFlag && (wx != 0)) {
                builder.append(" ");
            }

            builder.append(Word36.toStringFromASCII(get(wx)));
        }

        return builder.toString();
    }

    /**
     * Creates a string containing the representation of this buffer in consecutive 4-character strings,
     * possibly delimited by spaces.
     * @param offset how far into the visible portion of this slice we should begin
     * @param words how many words we should process
     * @return display string
     */
    public String toASCII(
        final int offset,
        final int words
    ) {
        StringBuilder builder = new StringBuilder();

        for (int wx = offset; wx < offset + words; ++wx) {
            if (wx > getSize()) { break; }
            builder.append(Word36.toStringFromASCII(get(wx)));
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

        for (int wx = 0; wx < getSize(); ++wx) {
            if (delimitFlag && (wx != 0)) {
                builder.append(" ");
            }
            builder.append(Word36.toStringFromFieldata(get(wx)));
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

        for (int wx = 0; wx < getSize(); ++wx) {
            if (delimitFlag && (wx != 0)) {
                builder.append(" ");
            }
            builder.append(Word36.toOctal(get(wx)));
        }

        return builder.toString();
    }

    /**
     * unpacks groups of 9-bytes of data into 36-bit word pairs into this array.
     * @param source array containing byte input
     * @param sourceOffset index of first byte to be converted
     * @param sourceCount number of bytes to be converted (should be divisible by 9)
     * @param skipOnSectorBoundary true if the byte data is organized as 28 words packed into 128 bytes (with 2 bytes of slop at the end)
     * @return number of bytes unpacked - will be less than sourceCount if we run out of space in this object
     */
    public int unpack(
        final byte[] source,
        final int sourceOffset,
        final int sourceCount,
        final boolean skipOnSectorBoundary
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
        int dx = _offset;
        int wordsLeft = _length;
        int partial = 0;

        while ((bytesLeft > 0) && (wordsLeft > 0)) {
            if (skipOnSectorBoundary && (sx % 128 == 126)) {
                sx += 2;
            }
            switch (partial) {
                case 0:
                    set(dx, (get(dx) & 0_001777_777777L) | (((long) source[sx] & 0xFF)  << 28));
                    ++count;
                    --bytesLeft;
                    break;

                case 1:
                    set(dx, (get(dx) & 0_776003_777777L) | (((long) source[sx] & 0xFF) << 20));
                    ++count;
                    --bytesLeft;
                    break;

                case 2:
                    set(dx, (get(dx) & 0_777774_007777L) | (((long) source[sx] & 0xFF) << 12));
                    ++count;
                    --bytesLeft;
                    break;

                case 3:
                    set(dx, (get(dx) & 0_777777_770017L) | (((long) source[sx] & 0xFF) << 4));
                    ++count;
                    --bytesLeft;
                    break;

                case 4:
                    set(dx, (get(dx) & 0_777777_777760L) | (((long)source[sx] & 0xF0) >> 4));
                    ++dx;
                    --wordsLeft;
                    if (wordsLeft > 0) {
                        set(dx, (get(dx) & 0_037777_777777L) | (((long)source[sx] & 0x0F) << 32));
                        ++count;
                    }
                    --bytesLeft;
                    break;

                case 5:
                    set(dx, (get(dx) & 0_740077_777777L) | (((long)source[sx] & 0xFF) << 24));
                    ++count;
                    --bytesLeft;
                    break;

                case 6:
                    set(dx, (get(dx) & 0_777700_177777L) | (((long)source[sx] & 0xFF) << 16));
                    ++count;
                    --bytesLeft;
                    break;

                case 7:
                    set(dx, (get(dx) & 0_777777_600377L) | (((long)source[sx] & 0xFF) << 8));
                    ++count;
                    --bytesLeft;
                    break;

                case 8:
                    set(dx, (get(dx) & 0_777777_777400L) | ((long)source[sx] & 0xFF));
                    ++count;
                    --bytesLeft;
                    ++dx;
                    --wordsLeft;
                    break;
            }

            ++sx;
            ++partial;
            if (partial == 9) {
                partial = 0;
            }
        }

        return count;
    }

    public int unpack(
        final byte[] source,
        final int sourceOffset,
        final int sourceCount
    ) {
        return unpack(source, sourceOffset, sourceCount, false);
    }

    public int unpack(
        final byte[] source
    ) {
        return unpack(source, 0, source.length, false);
    }

    /**
     * Unpacks quarter-words into our underlying array.
     * We'll obviously stop if we hit the end of said underlying array, so plz don't make us do that.
     * @return number of quarter words processed
     */
    public int unpackQuarterWords(
        final byte[] source,
        final int sourceOffset,
        final int sourceCount
    ) {
        int quarterWord = 0;
        int byteCount = 0;
        int sx = sourceOffset;
        for (int wordCount = 0, dx = _offset;
             (wordCount < _length) && (sx < source.length) && (sx < sourceCount);
             sx++, byteCount++) {
            switch (quarterWord) {
                case 0:
                    // We do not SetQ1 here intentionally - we WANT the rest of the word zeroed out.
                    _array[dx] = (((long) source[sx]) & 0_377) << 27;
                    ++quarterWord;
                    break;

                case 1:
                    _array[dx] = Word36.setQ2(_array[dx], source[sx] & 0_377);
                    ++quarterWord;
                    break;

                case 2:
                    _array[dx] = Word36.setQ3(_array[dx], source[sx] & 0_377);
                    ++quarterWord;
                    break;

                case 3:
                    _array[dx] = Word36.setQ4(_array[dx], source[sx] & 0_377);
                    ++dx;
                    ++wordCount;
                    quarterWord = 0;
            }
        }

        return byteCount;
    }

    /**
     * Unpacks quarter-words into our underlying array.
     * We'll obviously stop if we hit the end of said underlying array, so plz don't make us do that.
     * @return number of sixth words processed
     */
    public int unpackQuarterWords(
        final byte[] source
    ) {
        return unpackQuarterWords(source, 0, source.length);
    }

    /**
     * Unpacks sixth-words into our underlying array.
     * We'll obviously stop if we hit the end of said underlying array, so plz don't make us do that.
     * @return number of sixth words processed
     */
    public int unpackSixthWords(
        final byte[] source,
        final int sourceOffset,
        final int sourceCount
    ) {
        int sixthWord = 0;
        int byteCount = 0;
        int sx = sourceOffset;
        for (int wordCount = 0, dx = _offset;
             (wordCount < _length) && (sx < source.length) && (sx < sourceCount);
             sx++, byteCount++) {
            switch (sixthWord) {
                case 0:
                    // We do not SetS1 here intentionally - we WANT the rest of the word zeroed out.
                    _array[dx] = (((long) source[sx]) & 0_77) << 30;
                    ++sixthWord;
                    break;

                case 1:
                    _array[dx] = Word36.setS2(_array[dx], source[sx] & 0_77);
                    ++sixthWord;
                    break;

                case 2:
                    _array[dx] = Word36.setS3(_array[dx], source[sx] & 0_77);
                    ++sixthWord;
                    break;

                case 3:
                    _array[dx] = Word36.setS4(_array[dx], source[sx] & 0_77);
                    ++sixthWord;
                    break;

                case 4:
                    _array[dx] = Word36.setS5(_array[dx], source[sx] & 0_77);
                    ++sixthWord;
                    break;

                case 5:
                    _array[dx] = Word36.setS6(_array[dx], source[sx] & 0_77);
                    ++dx;
                    ++wordCount;
                    sixthWord = 0;
            }
        }

        return byteCount;
    }

    /**
     * Unpacks sixth-words into our underlying array.
     * We'll obviously stop if we hit the end of said underlying array, so plz don't make us do that.
     * @return number of sixth words processed
     */
    public int unpackSixthWords(
        final byte[] source
    ) {
        return unpackSixthWords(source, 0, source.length);
    }
}

/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.InvalidArgumentRuntimeException;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Represents a subset of a base array of elements of type T
 */
public class ArraySlice {

    private final long[] _array;    //  base array of which this slice is a (possibly complete) subset
    private final int _length;      //  length of this array (must be <= length of base array)
    private final int _offset;      //  offset into the base array, at which this slice begins
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
     * @throws InvalidArgumentRuntimeException if offset or index (or the combination thereof) is invalid
     */
    public ArraySlice(
        final long[] array,
        final int offset,
        final int length
    ) throws InvalidArgumentRuntimeException {
        if ((offset + length > array.length) || (offset < 0) || (length < 0)) {
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid arguments array size=%d requested offset=%d length=%d",
                              array.length,
                              offset,
                              length));
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
     * @throws InvalidArgumentRuntimeException if offset or index (or the combination thereof) is invalid
     */
    public ArraySlice(
        final ArraySlice baseSlice,
        final int offset,
        final int length
    ) throws InvalidArgumentRuntimeException {
        if ((offset + length > baseSlice._length) || (offset < 0) || (length < 0)) {
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid arguments base slice size=%d requested offset=%d length=%d",
                              baseSlice._length,
                              offset,
                              length));
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

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof ArraySlice) {
            ArraySlice asObj = (ArraySlice) obj;
            if (asObj._length == _length) {
                for (int objx = asObj._offset, thisx = _offset, x = 0; x < asObj._length; ++objx, ++thisx, ++x) {
                    if (asObj._array[objx] != _array[thisx]) {
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
     * @throws InvalidArgumentRuntimeException if index is invalid
     */
    public long get(
        final int index
    ) throws InvalidArgumentRuntimeException {
        if ((index < 0) || (index >= _length)){
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid index=%d slice length=%d",
                              index,
                              _length));
        }

        return _array[index + _offset];
    }

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
        int slimit = source.length > _length ? _length : source.length;
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
     * @throws InvalidArgumentRuntimeException if any parameter or combination of parameters doesn't make sense
     */
    public void load(
        final long[] source,
        final int sourceIndex,
        final int sourceLength,
        final int destinationIndex
    ) throws InvalidArgumentRuntimeException {
        if (sourceIndex + sourceLength > source.length) {
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid parameter source array length:%d source index:%d source length:%d",
                              source.length,
                              sourceIndex,
                              sourceLength));
        }

        if (destinationIndex + sourceLength > _length) {
            throw new InvalidArgumentRuntimeException(
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
    ) throws InvalidArgumentRuntimeException {
        if (destinationIndex + source._length > _length) {
            throw new InvalidArgumentRuntimeException(
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
     * @throws InvalidArgumentRuntimeException if any parameter or combination of parameters doesn't make sense
     */
    public void load(
        final ArraySlice source,
        final int sourceIndex,
        final int sourceLength,
        final int destinationIndex
    ) throws InvalidArgumentRuntimeException {
        load(source._array, source._offset + sourceIndex, sourceLength, destinationIndex);
    }

    /**
     * Logs the contents of a particular buffer of 36-bit values in multi-format mode...
     * That is, four words per line in octal, then fieldata, then ASCII, with offset indicators on the left.
     * @param logger destination for the output
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     */
    public void logMultiFormat(
        final Logger logger,
        final Level logLevel,
        final String caption
    ) {
        if (!caption.isEmpty()) {
            logger.printf(logLevel, "--[ %s ]--", caption);
        }

        int bufferIndex = 0;
        int remainingWords = _length;
        final int wordsPerRow = 4;
        for (int rowIndex = 0;
             remainingWords > 0;
             rowIndex += wordsPerRow, bufferIndex += wordsPerRow, remainingWords -= wordsPerRow) {
            //  Get a subset of the buffer
            int wordBufferSize = remainingWords > wordsPerRow ? wordsPerRow : remainingWords;
            ArraySlice subset = new ArraySlice(this, bufferIndex, wordBufferSize);

            //  Build octal string
            StringBuilder octalBuilder = new StringBuilder();
            octalBuilder.append(subset.toOctal(true));
            for (int wx = subset.getSize(); wx < wordsPerRow; ++wx) {
                octalBuilder.append("             ");
            }

            //  Build fieldata string
            StringBuilder fieldataBuilder = new StringBuilder();
            fieldataBuilder.append(subset.toFieldata(true));
            for (int wx = subset.getSize(); wx < wordsPerRow; ++wx) {
                fieldataBuilder.append("       ");
            }

            //  Build ASCII string
            StringBuilder asciiBuilder = new StringBuilder();
            asciiBuilder.append(subset.toASCII(true));
            for (int wx = subset.getSize(); wx < wordsPerRow; ++wx) {
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
    public void logOctal(
        final Logger logger,
        final Level logLevel,
        final String caption
    ) {
        if (!caption.isEmpty()) {
            logger.printf(logLevel, "--[ %s ]--", caption);
        }

        int bufferIndex = 0;
        int remainingWords = _length;
        for (int rowIndex = 0; remainingWords > 0; rowIndex += 7, bufferIndex += 7, remainingWords -= 7) {
            //  Get a subset of the buffer
            ArraySlice subset = new ArraySlice(this, bufferIndex, 7);

            //  Build octal string
            String octalString = subset.toOctal(true);

            //  Log the output
            logger.printf(logLevel, String.format("%06o:%s", rowIndex, octalString));
        }
    }

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

        int wordsLeft = getSize();
        int sx = 0;
        int bytesLeft = destination.length - destinationOffset;
        int dx = destinationOffset;
        int count = 0;
        int partial = 0;

        while ((wordsLeft > 0) && (bytesLeft > 0)) {
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
                    destination[dx] = (byte)((get(sx) & 017) << 4);
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
     * Sets a value into the array at the given index, which is offset further by this subset's offset
     * @param index index into the subset at which the value should be stored
     * @param value value to be stored
     * @throws InvalidArgumentRuntimeException if the index is invalid
     */
    public void set(
        final int index,
        final long value
    ) throws InvalidArgumentRuntimeException {
        if ((index < 0) || (index >= _length)){
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid index=%d slice length=%d",
                              index,
                              _length));
        }

        _array[index + _offset] = value;
    }

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
            temp[tx++] = Word36.stringToWord36ASCII(source.substring(sx)).getW();
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
        int words = source.length();
        if (source.length() % 6 > 0) {
            words++;
        }

        long[] temp = new long[words];
        int tx = 0;
        for (int sx = 0; sx < source.length(); sx += 6) {
            temp[tx++] = Word36.stringToWord36Fieldata(source.substring(sx)).getW();
        }

        return new ArraySlice(temp);
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
            builder.append(Word36.toASCII(get(wx)));
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
            builder.append(Word36.toFieldata(get(wx)));
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
        int wordsLeft = getSize();
        int partial = 0;

        while ((bytesLeft > 0) && (wordsLeft > 0)) {
            switch (partial) {
                case 0:
                    set(dx, (get(dx) & 0_001777_777777L) | ((long)source[sx++] << 28));
                    ++count;
                    --bytesLeft;
                    break;

                case 1:
                    set(dx, (get(dx) & 0_776003_777777L) | ((long)source[sx++] << 20));
                    ++count;
                    --bytesLeft;
                    break;

                case 2:
                    set(dx, (get(dx) & 0_777774_007777L) | ((long)source[sx++] << 12));
                    ++count;
                    --bytesLeft;
                    break;

                case 3:
                    set(dx, (get(dx) & 0_777777_770017L) | ((long)source[sx++] << 4));
                    ++count;
                    --bytesLeft;
                    break;

                case 4:
                    set(dx, (get(dx) & 0_777777_777760L) | ((long)source[sx] >> 4));
                    ++dx;
                    --wordsLeft;
                    if (wordsLeft > 0) {
                        set(dx, (get(dx) & 0_037777_777777L) | (((long)source[sx] & 017) << 32));
                        ++count;
                    }
                    ++sx;
                    --bytesLeft;
                    break;

                case 5:
                    set(dx, (get(dx) & 0_740077_777777L) | ((long)source[sx++] << 24));
                    ++count;
                    --bytesLeft;
                    break;

                case 6:
                    set(dx, (get(dx) & 0_777700_177777L) | ((long)source[sx++] << 16));
                    ++count;
                    --bytesLeft;
                    break;

                case 7:
                    set(dx, (get(dx) & 0_777777_600377L) | ((long)source[sx++] << 8));
                    ++count;
                    --bytesLeft;
                    break;

                case 8:
                    set(dx, (get(dx) & 0_777777_777400L) | (long)source[sx++]);
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

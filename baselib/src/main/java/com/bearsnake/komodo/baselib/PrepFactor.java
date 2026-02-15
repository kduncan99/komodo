/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

/**
 * Wraps an integer in a thin wrapper to help enhance type checking of things which are really just ints or longs
 */
public class PrepFactor {

    //  Not instantiable
    private PrepFactor() {}

    /**
     * Determines how many 36-bit blocks, of the size indicated by the value of this object,
     * will be required to make up one track (which is comprised of 1792 36-bit words).
     */
    public static int getBlocksPerTrack(
        final int prepFactor
    ) {
        return 1792 / prepFactor;
    }

    /**
     * Indicates the required size of a byte buffer to contain a 36-bit block sized according to the
     * value of this object, presuming said 36-bit words are packed 2 words -> 9 bytes.
     * This presumes the byte buffer size is required to be a power of two; the resulting value will contain some unused bytes.
     */
    public static int getContainingByteBlockSize(
        final int prepFactor
    ) {
        switch (prepFactor) {
            case 28:    return 128;
            case 56:    return 256;
            case 112:   return 512;
            case 224:   return 1024;
            case 448:   return 2048;
            case 896:   return 4096;
            case 1792:  return 8192;
        }

        return 0;
    }

    /**
     * In consideration of the commentary for getContaingByteBlockSize(), this method indicates how many unused
     * bytes there are at the end of the containing byte buffer.
     */
    public static int getContainerByteBlockSlop(
        final int prepFactor
    ) {
        return switch (prepFactor) {
            case 28 -> 2;
            case 56 -> 4;
            case 112 -> 8;
            case 224 -> 16;
            case 448 -> 32;
            case 896 -> 64;
            case 1792 -> 128;
            default -> 0;
        };
    }

    /**
     * Converts a disk pack byte block size to a prep factor
     */
    public static int getPrepFactorFromBlockSize(
        final long blockSize
    ) {
        return switch ((int) blockSize) {
            case 128 -> 28;
            case 256 -> 56;
            case 512 -> 112;
            case 1024 -> 224;
            case 2048 -> 448;
            case 4096 -> 896;
            case 8192 -> 1792;
            default -> 0;
        };
    }

    /**
     * Indicates whether the value of this object is a valid prep factor
     */
    public static boolean isValid(
        final int size
    ) {
        return switch (size) {   //  non-standard, but allowed
            case 28, 56, 112, 224, 448, 896, 1792 -> true;
            default -> false;
        };
    }
}

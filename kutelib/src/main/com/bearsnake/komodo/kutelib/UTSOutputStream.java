/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class UTSOutputStream extends ByteArrayOutputStream {

    public UTSOutputStream() {
        super();
    }

    public UTSOutputStream(int i) {
        super(i);
    }

    public enum FCCFormat {
        FCC_BASIC,
        FCC_EXTENDED,
        FCC_COLOR_FG,
        FCC_COLOR_BG,
        FCC_COLOR_FG_BG_ONE_BYTE,
        FCC_COLOR_FG_BG_TWO_BYTES,
    }

    public byte[] getBuffer() {
        return Arrays.copyOf(this.buf, this.count);
    }

    public UTSOutputStream write(final String string) {
        for (var b : string.getBytes()) {
            write(b);
        }
        return this;
    }

    public UTSOutputStream write(final byte b) {
        super.write(b);
        return this;
    }

    private void writeCoordinate(final int coordinate) {
        if (coordinate <= 80) {
            write((byte) (coordinate + 31));
        } else {
            var slop = coordinate - 81;
            write((slop >> 4) + 0x75);
            write((byte) (slop & 0x0F));
        }
    }

    public UTSOutputStream writeCursorToHome() {
        write(Constants.ASCII_ESC);
        write('e');
        return this;
    }

    public UTSOutputStream writeCursorPosition(final int row,
                                               final int column) {
        write(Constants.ASCII_ESC);
        write(Constants.ASCII_VT);
        writeCoordinate(row);
        writeCoordinate(column);
        write(Constants.ASCII_NUL);
        write(Constants.ASCII_SI);
        return this;
    }

    public UTSOutputStream writeCursorPosition(final Coordinates coordinates) {
        return writeCursorPosition(coordinates.getRow(), coordinates.getColumn());
    }

    public UTSOutputStream writeEraseDisplay() {
        write(Constants.ASCII_ESC);
        write('M');
        return this;
    }

    public UTSOutputStream writeFCC(final int row,
                                    final int column,
                                    final FieldAttributes attributes,
                                    final FCCFormat format) {
        write(Constants.ASCII_US);
        writeCoordinate(row);
        writeCoordinate(column);
        writeFCCCodes(attributes, format);
        return this;
    }

    public UTSOutputStream writeFCC(final FieldAttributes attributes,
                                    final FCCFormat format) {
        write(Constants.ASCII_EM);
        writeFCCCodes(attributes, format);
        return this;
    }

    private void writeFCCCodes(final FieldAttributes attributes,
                               final FCCFormat format) {
        switch (format) {
            case FCC_BASIC -> writeFCCCodesBasic(attributes);
            case FCC_EXTENDED -> writeFCCCodesExtended(attributes);
            case FCC_COLOR_FG -> writeFCCCodesColorFG(attributes);
            case FCC_COLOR_BG -> writeFCCCodesColorBG(attributes);
            case FCC_COLOR_FG_BG_ONE_BYTE -> writeFCCCodesColorOneByte(attributes);
            case FCC_COLOR_FG_BG_TWO_BYTES -> writeFCCCodesColorTwoBytes(attributes);
        }
    }

    private void writeFCCCodesBasic(final FieldAttributes attributes) {
        byte m = 0x30;
        byte n = 0x30;

        if (!attributes.isTabStop()) m |= 0x08;
        if (attributes.isChanged()) m |= 0x04;
        if (attributes.isBlinking()) {
            m |= 0x03;
        } else if (attributes.getIntensity() != Intensity.NONE) {
            m |= 0x01;
        } else if (attributes.getIntensity() == Intensity.LOW) {
            m |= 0x02;
        }

        if (attributes.isRightJustified()) n |= 0x04;
        if (attributes.isAlphabeticOnly()) {
            n |= 01;
        } else if (attributes.isNumericOnly()) {
            n |= 02;
        } else if (attributes.isProtected()) {
            n |= 03;
        }

        write(m);
        write(n);
    }

    private void writeFCCCodesExtended(final FieldAttributes attributes) {
        byte m = 0x30;
        byte n = 0x30;


        write(m);
        write(n);
    }

    private void writeFCCCodesColorBG(final FieldAttributes attributes) {
        write(0x22);
        writeFCCCodesExtended(attributes);
        write(attributes.getBackgroundColor().getByteValue() | 0x40);
    }

    private void writeFCCCodesColorFG(final FieldAttributes attributes) {
        write(0x21);
        writeFCCCodesExtended(attributes);
        write(attributes.getTextColor().getByteValue() | 0x40);
    }

    private void writeFCCCodesColorOneByte(final FieldAttributes attributes) {
        write(0x20);
        writeFCCCodesExtended(attributes);
        byte c1 = (byte)(attributes.getBackgroundColor().getByteValue() << 3);
        c1 |= attributes.getTextColor().getByteValue();
        c1 |= 0x40;
        write(c1);
    }

    private void writeFCCCodesColorTwoBytes(final FieldAttributes attributes) {
        write(0x23);
        writeFCCCodesExtended(attributes);
        write(attributes.getTextColor().getByteValue() | 0x40);
        write(attributes.getBackgroundColor().getByteValue() | 0x40);
    }
}

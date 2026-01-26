/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.exceptions.FCCIncompleteSequenceException;
import com.bearsnake.komodo.kutelib.exceptions.FCCSequenceException;

import java.io.ByteArrayInputStream;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class UTSInputStream extends ByteArrayInputStream {

    public UTSInputStream(byte[] buf) {
        super(buf);
    }

    public UTSInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    public boolean atEnd() {
        return available() == 0;
    }

    public int readCoordinate() throws CoordinateException {
        if (atEnd()) {
            throw new CoordinateException("Incomplete or missing coordinate");
        }

        var ch = (byte) read();
        if ((ch >= 0x20) && (ch <= 0x6f)) {
            return (ch - 0x20 + 1);
        } else if (ch >= 0x75) {
            if (atEnd()) {
                throw new CoordinateException("Incomplete or missing coordinate");
            }

            var ch2 = (byte) read();
            if (ch2 >= 70) {
                return 81 + ((ch - 0x75) << 4) + (ch2 & 0x0F);
            }
        }

        throw new CoordinateException("Invalid coordinate");
    }

    /**
     * Reads the cursor position escape sequence from the stream and returns a corresponding Coordinates object.
     * @return Coordinates if successful, null if we do not encounter an initial ESC VT sequence.
     * @throws CoordinateException if something is wrong with the sequence
     */
    public Coordinates readCursorPosition() throws CoordinateException {
        mark(0);
        var esc = (byte) read();
        if (esc != ASCII_ESC) {
            reset();
            return null;
        }
        var vt = (byte) read();
        if (vt != ASCII_VT) {
            reset();
            return null;
        }

        // we are now committed to a good return or an exception
        int row = readCoordinate();
        int column = readCoordinate();
        var si = (byte) read();
        if (si != ASCII_SI) {
            throw new CoordinateException("Missing SI at end of ESC VT sequence");
        }

        return new Coordinates(row, column);
    }

    /**
     * Reads the field control character sequence from the stream and returns a corresponding Field object.
     * If the stream does not contain a valid FCC sequence, returns null.
     * If this is an immediate FCC sequence, the Coordinate attribute of the Field object will be null.
     * @return Field if successful, null if we do not encounter an initial EM sequence.
     * @throws CoordinateException if something is wrong with the sequence
     */
    public Field readFCC()
        throws CoordinateException, FCCSequenceException {
        // EM [ O ] M N -- We've already ingested EM
        // US row col [ O ] M N [ C1 ... ] 
        mark(0);
        var fChar = (byte) read();
        Coordinates coordinates = null;
        if (fChar == ASCII_US) {
            int row = readCoordinate();
            int column = readCoordinate();
            coordinates = new Coordinates(row, column);
        } else if (fChar != ASCII_EM) {
            reset();
            return null;
        }

        var field = new ExplicitField(coordinates);
        if (atEnd()) {
            throw new FCCIncompleteSequenceException();
        }
        var ch = (byte) read();
        var oChar = (byte) 0;
        if ((ch >= 0x20) && (ch <= 0x2F)) {
            oChar = Byte.valueOf(ch);
            if (atEnd()) {
                throw new FCCIncompleteSequenceException();
            }
            ch = (byte)read();
        }

        var mChar = ch;
        if (atEnd()) {
            throw new FCCIncompleteSequenceException();
        }
        var nChar = (byte) read();
        if ((mChar >= 0x30) && (mChar <= 0x3f) && (nChar >= 0x30) && (nChar <= 0x3f)) {
            // UTS400 compatible FCC sequence
            switch (mChar & 0x03) {
                case 0x00 -> field.setIntensity(Intensity.NORMAL);
                case 0x01 -> field.setIntensity(Intensity.NONE);
                case 0x02 -> field.setIntensity(Intensity.LOW);
                case 0x03 -> field.setBlinking(true);
            }
            field.setChanged((mChar & 0x04) == 0x00);
            field.setTabStop((mChar & 0x08) == 0x00);
            switch (nChar & 0x03) {
                case 0x00 -> {}
                case 0x01 -> field.setAlphabeticOnly(true);
                case 0x02 -> field.setNumericOnly(true);
                case 0x03 -> field.setProtected(true);
            }
            field.setRightJustified((nChar & 0x04) == 0x04);
        } else if ((mChar >= 0x40) && (nChar >= 0x40)) {
            // Expanded FCC sequence
            if ((mChar & 0x01) == 0x01) { field.setIntensity(Intensity.NONE); }
            if ((mChar & 0x02) == 0x02) { field.setIntensity(Intensity.LOW); }
            field.setChanged((mChar & 0x04) == 0x00);
            field.setTabStop((mChar & 0x08) == 0x00);
            field.setProtectedEmphasis((mChar & 0x20) == 0x20);
            switch (nChar & 0x03) {
                case 0x00 -> {}
                case 0x01 -> field.setAlphabeticOnly(true);
                case 0x02 -> field.setNumericOnly(true);
                case 0x03 -> field.setProtected(true);
            }
            field.setRightJustified((nChar & 0x04) == 0x04);
            field.setBlinking((nChar & 0x08) == 0x08);
            field.setReverseVideo((nChar & 0x10) == 0x10);
        } else {
            throw new FCCSequenceException(mChar, nChar);
        }

        if (oChar == 0x20) {
            // next char is 0b01gggttt ggg=background color, ttt=text color
            if (atEnd()) {
                throw new FCCIncompleteSequenceException();
            }
            var ch2 = (byte) read();
            field.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
            field.setBackgroundColor(UTSColor.fromByte((byte)((ch2 >> 3) & 0x07)));
        } else if (oChar == 0x21) {
            // next char is text color in lower 3 bits
            if (atEnd()) {
                throw new FCCIncompleteSequenceException();
            }
            var ch2 = (byte) read();
            field.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
        } else if (oChar == 0x22) {
            // next char is bg color in lower 3 bits
            if (atEnd()) {
                throw new FCCIncompleteSequenceException();
            }
            var ch2 = (byte) read();
            field.setBackgroundColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
        } else if (oChar == 0x23) {
            // next chars are text color in lower 3 bits, then bg color in lower 3 bits
            if (atEnd()) {
                throw new FCCIncompleteSequenceException();
            }
            var ch2 = (byte) read();
            field.setTextColor(UTSColor.fromByte((byte)(ch2 & 0x07)));
            var ch3 = (byte) read();
            field.setBackgroundColor(UTSColor.fromByte((byte)(ch3 & 0x07)));
        } else {
            // reserved color code - error for now
            throw new FCCSequenceException("Invalid O byte", ch);
        }

        return field;
    }
}

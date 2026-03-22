/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.Register;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Byte to Decimal instruction
 * (BDE) Converts an ASCII or External-Computational-3 character string from U in storage (not GRS)
 * to a decimal value, stored in one to three A registers.
 */
public class BDEFunction extends DecimalFunction {

    public static final BDEFunction INSTANCE = new BDEFunction();

    protected static final int BDE_POSITIVE_SIGN = 012;
    protected static final int BDE_NEGATIVE_SIGN = 015;

    private static class AsciiReadBuffer {
        public final long[] _asciiString;
        public int _wordIndex;
        public int _charIndex;

        public AsciiReadBuffer(final long[] asciiString) {
            _asciiString = asciiString;
            _wordIndex = 0;
            _charIndex = 0;
        }

        public boolean atEnd() {
            return _wordIndex >= _asciiString.length;
        }

        public boolean atLast() {
            return (_wordIndex == (_asciiString.length - 1)) && (_charIndex == 3);
        }

        public int getNextChar() {
            var result = peekNextChar();
            _charIndex++;
            if (_charIndex == 4) {
                _wordIndex++;
                _charIndex = 0;
            }
            return result & 0777;
        }

        public int peekNextChar() {
            if (_wordIndex >= _asciiString.length) {
                throw new IllegalStateException("Unexpected value: " + _wordIndex);
            }
            return switch (_charIndex) {
                case 0-> Word36.getQ1(_asciiString[_wordIndex]);
                case 1-> Word36.getQ2(_asciiString[_wordIndex]);
                case 2-> Word36.getQ3(_asciiString[_wordIndex]);
                case 3-> Word36.getQ4(_asciiString[_wordIndex]);
                default-> throw new IllegalStateException("Unexpected value: " + _charIndex);
            };
        }

        public void skip(
            final int skipCount
        ) {
            int max = Math.min(_asciiString.length * 4, skipCount);
            _wordIndex += max / 4;
            _charIndex += max % 4;
        }
    }

    private static class ComputationalReadBuffer {
        public final long[] _computationalString;
        public int _wordIndex;
        public int _charIndex;

        public ComputationalReadBuffer(final long[] computationalString) {
            _computationalString = computationalString;
            _wordIndex = 0;
            _charIndex = 0;
        }

        public boolean atEnd() {
            return _wordIndex >= _computationalString.length;
        }

        public boolean atLast() {
            return (_wordIndex == (_computationalString.length - 1)) && (_charIndex == 7);
        }

        public int getNextChar() {
            var result = peekNextChar();
            _charIndex++;
            if (_charIndex == 8) {
                _wordIndex++;
                _charIndex = 0;
            }
            return result & 017;
        }

        public int peekNextChar() {
            if (_wordIndex >= _computationalString.length) {
                throw new IllegalStateException("Unexpected value: " + _wordIndex);
            }
            var qw = switch (_charIndex) {
                case 0, 1 -> Word36.getQ1(_computationalString[_wordIndex]);
                case 2, 3 -> Word36.getQ2(_computationalString[_wordIndex]);
                case 4, 5 -> Word36.getQ3(_computationalString[_wordIndex]);
                case 6, 7 -> Word36.getQ4(_computationalString[_wordIndex]);
                default-> throw new IllegalStateException("Unexpected value: " + _charIndex);
            };
            if ((_charIndex & 01) == 01) {
                return qw & 017;
            } else {
                return (qw >> 4) & 017;
            }
        }

        public void skip(
            final int skipCount
        ) {
            int max = Math.min(_computationalString.length * 8, skipCount);
            _wordIndex += max / 8;
            _charIndex += max % 8;
        }
    }

    private static class ByteWriteBuffer {
        public final Register[] _registers;
        public int _registerIndex;
        public int _byteIndex;
        public boolean _allZeros;

        public ByteWriteBuffer(final Register[] registers) {
            _registers = registers;
            _registerIndex = 0;
            _byteIndex = 0;
            _allZeros = true;
        }

        public boolean atSignCell() {
            return (_registerIndex == (_registers.length - 1) && (_byteIndex == 8));
        }

        public void putByte(
            final int value
        ) {
            if (_registerIndex >= _registers.length) {
                throw new IllegalStateException("Unexpected value: " + _registerIndex);
            }

            var reg = _registers[_registerIndex];
            var shift = (8 - _byteIndex) * 4;
            var shiftedValue = ((long)value & 017) << shift;
            var notMask = (~(017L << shift)) & 0_777777_777777L;
            var newValue = (reg.getW() & notMask) | shiftedValue;
            reg.setW(newValue);

            _byteIndex++;
            if (_byteIndex == 9) {
                _registerIndex++;
                _byteIndex = 0;
            }

            if (value != 0) {
                _allZeros = false;
            }
        }
    }

    private BDEFunction() {
        super("BDE");
        setBasicModeFunctionCode(new FunctionCode(037).setJField(015));
        setExtendedModeFunctionCode(new FunctionCode(072).setJField(010));

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var aReg = engine.getExecOrUserARegister(ci.getA());
        var xReg = engine.getExecOrUserXRegister(ci.getX());

        var cf = (aReg.getW() >> 16) & 01;
        if (cf == 0) {
            return doASCII(engine, aReg, xReg);
        } else {
            return doExternalComputational3(engine, aReg, xReg);
        }
    }

    public boolean doASCII(
        final Engine engine,
        final Register aReg,
        final Register xReg
    ) throws MachineInterrupt {
        // The first character we care about is indicated by sourceCharIndex.
        //  0 = Q1, 1 = Q2, etc
        var startCharIndexLocation = aReg.getW() >> 35;
        var sourceCharIndex = startCharIndexLocation == 1 ? xReg.getS1() & 03 : aReg.getH1() & 03;

        // signLocation values:
        //  0:3 no sign
        //  4   trailing included
        //  5   trailing separate
        //  6   leading included
        //  7   leading separate
        var signLocation = (aReg.getW() >> 13) & 07;
        var digitCount = (int)aReg.getW() & 037;

        // Calculate the number of words we're going to need from U.
        // This is the sum of the digit count, the number of ignored characters in
        // the first word, and the separate sign, all divided by the number of characters
        // in a word rounded up. Then go get them.
        var uChars = digitCount + sourceCharIndex;
        if ((signLocation == 5) || (signLocation == 7)) {
            uChars++;
        }
        var uWords = uChars / 4 + (uChars % 4 == 0 ? 0 : 1);

        var operands = engine.getConsecutiveOperands(false, uWords);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        // Number of zeros we place in the highest-order digits of the result.
        var skipCount = (int)(aReg.getW() >> 6) & 037;

        // Number of A registers we're going to use.
        var numberOfRegisters = (int)(aReg.getW() >> 11) & 03;
        if (numberOfRegisters == 0) {
            // not sure what to do here... so we'll just do this.
            return true;
        }

        // Create an array of references to the output registers,
        // and initialize them to zero.
        var destRegNumber = (int)(aReg.getW() >> 22) & 017;
        var destRegisters = new Register[numberOfRegisters];
        for (var i = 0; i < numberOfRegisters; i++) {
            destRegisters[i] = engine.getExecOrUserARegister(destRegNumber + i);
            destRegisters[i].setW(0);
        }

        // We're going to need a read buffer which can serve up a character at a time.
        var asciiReadBuffer = new AsciiReadBuffer(operands);
        asciiReadBuffer.skip(sourceCharIndex);

        // ... and a write buffer
        var byteWriteBuffer = new ByteWriteBuffer(destRegisters);
        var capacity = (numberOfRegisters * 9) - 1;
        var limit = capacity - digitCount;
        if (skipCount > limit) {
            limit = skipCount;
        }
        for (var i = 0; i < limit; i++) {
            byteWriteBuffer.putByte(0);
        }

        // Do we have a leading sign? Is it separate or included?
        var isNegative = false;
        if (!asciiReadBuffer.atEnd()) {
            if (signLocation == 6) {
                // leading included
                var sign = asciiReadBuffer.peekNextChar();
                isNegative = (sign == 041) || (sign == 0175) || ((sign >= 0112) && (sign <= 0122));
            } else if (signLocation == 7) {
                // leading separate
                var sign = asciiReadBuffer.getNextChar();
                isNegative = (sign & 0_004) != 0;
            }
        }

        // Start translating characters to BCD.
        var trailingIncluded = (signLocation == 4);
        var trailingSeparate = (signLocation == 5);
        var lastCharInput = 0;
        for (var i = 0; i < digitCount; i++) {
            lastCharInput = asciiReadBuffer.getNextChar();
            if (!byteWriteBuffer.atSignCell()) {
                byteWriteBuffer.putByte(lastCharInput);
            }

            if (trailingSeparate) {
                if (byteWriteBuffer.atSignCell() || asciiReadBuffer.atLast()) {
                    break;
                }
            }  else {
                if (asciiReadBuffer.atEnd()) {
                    break;
                }
            }
        }
        if (trailingIncluded) {
            isNegative = (lastCharInput == 041) || (lastCharInput == 0175) || ((lastCharInput >= 0112) && (lastCharInput <= 0122));
        }

        // Code logic guarantees we have exactly one character left IF we are trailing separate.
        if (trailingSeparate) {
            var sign = asciiReadBuffer.getNextChar();
            isNegative = (sign & 0_004) != 0;
        }

        // Do we need to pad the result?
        while (!byteWriteBuffer.atSignCell()) {
            byteWriteBuffer.putByte(0);
        }

        // Write the sign character to the result.
        // If we've only ever written zeros, then the result is positive.
        byteWriteBuffer.putByte((isNegative && !byteWriteBuffer._allZeros) ? BDE_NEGATIVE_SIGN : BDE_POSITIVE_SIGN);
        return true;
    }

    public boolean doExternalComputational3(
        final Engine engine,
        final Register aReg,
        final Register xReg
    ) throws MachineInterrupt {
        // The first character we care about is indicated by sourceQWIndex and digitCount.
        // The former indicates which quarter-word contains the character, 0=Q1, 1=Q2, etc.
        // An even value for digitCount indicates the character in bits 1-4 of the quarter word,
        // while an odd value indicates the character in bits 5-8.
        var startCharIndexLocation = aReg.getW() >> 35;
        var sourceQWIndex = startCharIndexLocation == 1 ? xReg.getS1() & 03 : aReg.getH1() & 03;
        var digitCount = (int)aReg.getW() & 037;
        var sourceCharIndex = (sourceQWIndex * 2) + ((digitCount & 01) == 0 ? 0 : 1);

        // signLocation values:
        //  0:3 no sign
        //  4:7 trailing separate
        var trailingSign = (aReg.getW() >> 13) > 03;

        // Calculate the number of words we're going to need from U.
        // This is the sum of the digit count, the number of ignored characters in
        // the first word, and the separate sign (if any), all divided by the number
        // of characters in a word rounded up. Then go get them.
        var uChars = digitCount + sourceCharIndex;
        if (trailingSign) {
            uChars++;
        }
        var uWords = uChars / 8 + (uChars % 8 == 0 ? 0 : 1);

        var operands = engine.getConsecutiveOperands(false, uWords);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        // Number of A registers we're going to use.
        var numberOfRegisters = (int)(aReg.getW() >> 11) & 03;
        if (numberOfRegisters == 0) {
            // not sure what to do here... so we'll just do this.
            return true;
        }

        var skipCount = (int)(aReg.getW() >> 6) & 037;

        // Create an array of references to the output registers,
        // and initialize them to zero.
        var destRegNumber = (int)(aReg.getW() >> 22) & 017;
        var destRegisters = new Register[numberOfRegisters];
        for (var i = 0; i < numberOfRegisters; i++) {
            destRegisters[i] = engine.getExecOrUserARegister(destRegNumber + i);
            destRegisters[i].setW(0);
        }

        // We're going to need a read buffer which can serve up a character at a time.
        var compReadBuffer = new ComputationalReadBuffer(operands);
        compReadBuffer.skip(sourceCharIndex);

        // ... and a write buffer
        var byteWriteBuffer = new ByteWriteBuffer(destRegisters);
        while (skipCount > 0 && !byteWriteBuffer.atSignCell()) {
            byteWriteBuffer.putByte(0);
            skipCount--;
        }

        // Start translating characters to BCD.
        for (var i = 0; i < digitCount; i++) {
            var lastCharInput = compReadBuffer.getNextChar();
            if (!byteWriteBuffer.atSignCell()) {
                byteWriteBuffer.putByte(lastCharInput);
            }

            if (trailingSign) {
                if (byteWriteBuffer.atSignCell() || compReadBuffer.atLast()) {
                    break;
                }
            }
        }

        // Code logic guarantees we have exactly one character left IF we are trailing separate.
        var isNegative = false;
        if (trailingSign) {
            var sign = compReadBuffer.getNextChar();
            isNegative = (sign == 013) || (sign == 015);
        }

        // Do we need to pad the result?
        while (!byteWriteBuffer.atSignCell()) {
            byteWriteBuffer.putByte(0);
        }

        // Write the sign character to the result.
        // If we've only ever written zeros, then the result is positive.
        byteWriteBuffer.putByte((isNegative && !byteWriteBuffer._allZeros) ? BDE_NEGATIVE_SIGN : BDE_POSITIVE_SIGN);
        return true;
    }
}

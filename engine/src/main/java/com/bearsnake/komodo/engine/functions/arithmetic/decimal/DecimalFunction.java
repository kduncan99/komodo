package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.functions.Function;

public abstract class DecimalFunction extends Function {

    // Single word decimal format consists of 9 4-bit cells.
    // Each cell is a digit from 0 to 9, most significant digit first.
    // The last cell indicates the sign of the value, while the first eight indicate the magnitude.
    // See isNegative() and isPositive() for values accepted for sign.

    /*
External-Computational-3 Data Format
External-Computational-3 data is two decimal digits packed into each quarter word with each
digit occupying four bits and the most significant bit of each quarter word being ignored on input
and quarter wordmost significant bit := 0 on output.
The decimal digits 0 through 9 are represented by the binary numbers 0000 through 1001.
External-Computational-3 may be selected to use for input for Byte to Decimal (BDE) and may be
selected for output for Decimal to Byte (DEB).
An External-Computational-3 string may have either no sign, in which case the number is
assumed to be positive, or Trailing Separate sign. The valid Trailing Separate sign octal codes
are:
012, 014, 016, 017 for positive sign
013, 015 for negative sign
For the BDE instruction, the Digit Count specified on input is always equal to the number of input
decimal digits to be converted plus one. When no-sign is specified, the character following the
last decimal digit converted is not considered and a positive sign (014) is written into bits 32–35
of the last A-Register specified.
Invalid sign codes produce Architecturally_Undefined results.
The octal sign codes generated as a result of a Decimal to Byte conversion when the destination
is in External-Computational-3 format are:
014 for positive sign
015 for negative sign
017 for no-sign
--------------------------------------
ASCII Numeric Data Format
The format for ASCII Numeric Data is one ASCII digit per quarter word. The decimal digits 0
through 9 are represented by the characters 060 through 071.
ASCII Numeric Data is always used for output for Edit Decimal (EDDE). ASCII Numeric Data may
be selected for input for Byte to Decimal (BDE). ASCII Numeric Data may be selected for output
for Decimal to Byte (DEB).
The ASCII Numeric Data can have the following sign locations:
• No Sign (in which case the number is assumed to be positive)
• Leading Separate Sign
• Trailing Separate Sign
• Leading Included Sign
• Trailing Included Sign.

In the Separate Sign cases, the sign occupies a quarter word by itself. This quarter word is part
of the string and either precedes the most significant digit of the number (Leading Separate Sign)
or follows the least significant digit (Trailing Separate Sign). On input, if the quarter word that
holds the separate signbit 6 = 0, the sign is treated as positive; if the quarter word that holds the
separate signbit 6 = 1, the sign is treated as negative (thus '+', ' ', '0', and binary zero are treated
as positive and '-' as negative). Octal sign codes generated on output are:
053 (ASCII for '+') for positive sign
055 (ASCII for '–') for negative sign
The sign for the Included Sign cases is combined with either the leading digit (Leading Included
Sign) or the trailing digit (Trailing Included Sign).
When converting from ASCII to decimal, the following considerations apply:
• When the ASCII digit is not in the included sign position, only the low order four bits are used
to determine the decimal digit, that is, hardware ignores the upper 5 bits. If the four bits
used to determine the digit contain binary values 1010 through 1111, the results are
Architecturally_Undefined.
• When the ASCII digit is in the included sign, the following table shows the valid values. All
other values are invalid digits and produce Architecturally_Undefined results.
 000000000 bin zero
 000100000 ' '
 000110000 '0'
 000101011 '+'
 000101101 '-'
b000000n00 negative sign bit

Characters Accepted on Input
Decimal Digit   Positive Sign (octal)   Negative Sign (octal)
0               See Note                 041, 0175
1               061, 0101                0112
2               062, 0102                0113
3               063, 0103                0114
4               064, 0104                0115
5               065, 0105                0116
6               066, 0106                0117
7               067, 0107                0120
8               070, 0110                0121
9               071, 0111                0122
Note: Values accepted as zero are 060, 077, 0173, and any digit that has the low order 5 bits zero.

Characters Generated on Output
Decimal Digit   Positive Sign (octal)   Negative Sign (octal)
0               0173                    0175
1               0101                    0112
2               0102                    0113
3               0103                    0114
4               0104                    0115
5               0105                    0116
6               0106                    0117
7               0107                    0120
8               0110                    0121
9               0111                    0122

Instruction Repertoire
Characters Generated on Output (No Sign)
Decimal Digit     Character
0                 060
1                 061
2                 062
3                 063
4                 064
5                 065
6                 066
7                 067
8                 070
9                 071
*/

    // Specific values we use for specifying the sign for a decimal value.
    protected static final int NEGATIVE_SIGN = 015;
    protected static final int POSITIVE_SIGN = 014;

    protected DecimalFunction(
        final String name
    ) {
        super(name);
    }

    /**
     * Converts the decimal value in operand to a 64-bit two's complement binary value.
     * Negative zero decimal values are converted to positive zero binary.
     * @param operand decimal operand
     * @return 64-bit two's complement binary value
     */
    protected static long toBinary(
        final long operand
    ) {
        var value = ((operand >> 32) & 017) * 10000000L + ((operand >> 28) & 017) * 1000000L
                    + ((operand >> 24) & 017) * 100000L + ((operand >> 20) & 017) * 10000L
                    + ((operand >> 16) & 017) * 1000L + ((operand >> 12) & 017) * 100L
                    + ((operand >> 8) & 017) * 10 + ((operand >> 4) & 017);
        if (isNegative(operand) && (value != 0)) {
            value = -value;
        }
        return value;
    }

    /**
     * Converts the decimal value in double-word operand to a 64-bit two's complement binary value.
     * Negative zero decimal values are converted to positive zero binary.
     * @param operandHigh 9-digit high-value decimal operand
     * @param operandLow 8-digit low-value decimal operand with sign
     * @return 64-bit two's complement binary value
     */
    protected static long doubleToBinary(
        final long operandHigh,
        final long operandLow
    ) {
        var value0 = ((operandHigh >> 32) & 017) * 100000000L + ((operandHigh >> 28) & 017) * 10000000L
                     + ((operandHigh >> 24) & 017) * 1000000L + ((operandHigh >> 20) & 017) * 100000L
                     + ((operandHigh >> 16) & 017) * 10000L + ((operandHigh >> 12) & 017) * 1000L
                     + ((operandHigh >> 8) & 017) * 100 + ((operandHigh >> 4) & 017) * 10 + (operandHigh & 017);

        var value1 = ((operandLow >> 32) & 017) * 10000000L + ((operandLow >> 28) & 017) * 1000000L
                     + ((operandLow >> 24) & 017) * 100000L + ((operandLow >> 20) & 017) * 10000L
                     + ((operandLow >> 16) & 017) * 1000L + ((operandLow >> 12) & 017) * 100L
                     + ((operandLow >> 8) & 017) * 10 + ((operandLow >> 4) & 017);

        var value = (value0 * 100000000L) + value1;

        if (isNegative(operandLow) && (value != 0)) {
            value = -value;
        }

        return value;
    }

    /**
     * Converts the binary value in the operand to a decimal value, stored in the decimal parameter.
     * @param binary 64-bit two's complement binary value
     * @param decimal decimal value to store the result
     * @return true if the binary value cannot fit in the decimal value (overflow condition)
     */
    protected static boolean toDecimal(
        final long binary,
        final Word36 decimal
    ) {
        var sign = binary < 0 ? NEGATIVE_SIGN : POSITIVE_SIGN;
        var magnitude = Math.abs(binary);
        long value = (sign); // bottom 8 decimal digits and sign digit

        var shift = 4;
        var over = false;
        while (magnitude != 0) {
            var digit = magnitude % 10;
            magnitude /= 10;
            value |= (digit & 017) << shift;

            shift += 4;
            if (shift == 36) {
                over = true;
                break;
            }
        }

        decimal.setW(value);
        return over;
    }

    /**
     * Converts the binary value in the operand to a double-word decimal value, stored in the decimal parameter.
     * @param binary 64-bit two's complement binary value
     * @param decimalHigh decimal value to store the 9 high-order digits
     * @param decimalLow decimal value to store the 8 low-order digits with the sign
     * @return true if the binary value cannot fit in the decimal value (overflow condition)
     */
    protected static boolean doubleToDecimal(
        final long binary,
        final Word36 decimalHigh,
        final Word36 decimalLow
    ) {
        var sign = binary < 0 ? NEGATIVE_SIGN : POSITIVE_SIGN;
        var magnitude = Math.abs(binary);
        long high = 0; // top 9 digits
        long low = (sign); // bottom 8 decimal digits and sign digit

        var shift = 4;
        var over = false;
        var wx = 1;
        while (magnitude > 0) {
            var digit = magnitude % 10;
            magnitude /= 10;

            if (wx == 1) {
                low |= (digit & 017) << shift;
            } else {
                high |= (digit & 017) << shift;
            }

            shift += 4;
            if (shift == 36) {
                if (wx == 0) {
                    if (magnitude > 0) {
                        over = true;
                    }
                    break;
                }
                shift = 0;
                wx = 0;
            }
        }

        decimalHigh.setW(high);
        decimalLow.setW(low);
        return over;
    }

    protected static boolean isNegative(
        final long operand
    ) {
        return ((operand & 01) == 01) && ((operand & 017) != 017);
    }

    protected static boolean isPositive(
        final long operand
    ) {
        return ((operand & 01) == 0) || ((operand & 017) == 017);
    }
}

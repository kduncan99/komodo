/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.algorithms;

import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Finds the first 30 numbers in the Fibonacci sequence.
 */
public class TestEratosthenes extends AlgorithmTest {

    private static final long[] EXPECTED_VALUES = {
        2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97,
        101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199,
        211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293,
        307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397,
        401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499,
        503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599,
        601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691,
        701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797,
        809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887,
        907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997
    };

    @Test
    public void testEratosthenes() throws MachineInterrupt {

        var codeSeg = allocateSegment(512);
        var dataSeg = allocateSegment(1024);
        var outputSeg = allocateSegment(1024);

        var code = getSegment(codeSeg);
        var data = getSegment(dataSeg);
        var output = getSegment(outputSeg);

        var codeLower = 0_1000;
        var codeUpper = 0_1777;
        var dataLower = 0_1000;
        var dataUpper = 0_2777;
        var outputLower = 0_1000;
        var outputUpper = 0_2777;

        // The program is as follows:
        // -- Do subroutines first (avoids forward references) --
        var pc = 0;

        // MARK .
        // . A prime number is in A0 - Insert it to the output array (indexed by X3).
        // . Then iterate over all the multiples of the prime number,
        // . marking them as non-prime.
        // . We cannot use A0 or X8, so we will avoid those.
        //      SA,W    A0,OUTPUT,*X3,B3
        //      LA      A3,A0               . Use A3 to iterate over multiples
        //      LSSL    A3,1                . Start with the 2*prime-number.
        // MARKLOOP .
        //      TG,U    A3,1000             . Make sure we don't go past 999.
        //      RTN
        //      SZ,W    DATA,X15,B2         . Set the multiple as not-prime (X15 is A3)
        //      AA      A3,A0
        //      J       MARKLOOP
        var mark = codeLower + pc;
        code[pc++] = sa(JFIELD_W, 0, 3, 1, 0, 3, outputLower);
        code[pc++] = laGRS(3, 0, 0, 0, GRS_A0);
        code[pc++] = lssl(3, 0, 0, 0, 1);

        var markLoop = codeLower + pc;
        code[pc++] = tgImm(3, 0, 0, 0, 1000);
        code[pc++] = rtn();
        code[pc++] = sz(JFIELD_W, 15, 0,  0, 2, dataLower);
        code[pc++] = aaGRS(3, 0, 0, 0, GRS_A0);
        code[pc++] = j(0, 0, 0, markLoop);

        // START . Start by assuming all are prime (marked by 1)
        //      LXI,U   X8,1
        //      LXM,U   X8,0
        //      LA,U    A8,999
        // CLEAR .
        //      SP1,W   DATA,*X8,B2
        //      JGD     A8,CLEAR
        //      SZ,W    DATA,B2     . 0 is not prime
        //      SZ,W    DATA+1,B2   . 1 is not prime
        var start = codeLower + pc;
        code[pc++] = lxiImm(8, 0, 0, 0, 1);
        code[pc++] = lxmImm(8, 0, 0, 0, 0);
        code[pc++] = laImm(8, 0, 0, 0, 999);

        var clear = codeLower + pc;
        code[pc++] = sp1(JFIELD_W, 8, 1, 0, 2, dataLower);
        code[pc++] = jgd(GRS_A8, 0, 0, 0, clear);
        code[pc++] = sz(JFIELD_W, 0, 0, 0, 2, dataLower);
        code[pc++] = sz(JFIELD_W, 0, 0, 0, 2, dataLower + 1);

        //      . Now start over at the top
        //      . Set X3 to auto-increment over output,
        //      . and A0 to contain the integer we're testing
        //      LXI,U   X3,1
        //      LXM,U   X3,0
        //      LA,U    A0,2        . start looking at n=2
        // LOOP .
        //      TZ      DATA,A0,B2  . Is the integer marked non-prime? If so, skip (A0 is X12)
        //      LOCL    MARK        . Note the prime and mark multiples as not-prime
        //      AA,U    A0,1        . Move to the next integer
        //      TLE,U   A0,999      . Is integer > 999? If so, skip
        //      J       LOOP
        //      $END
        code[pc++] = lxiImm(3, 0, 0, 0, 1);
        code[pc++] = lxmImm(3, 0, 0, 0, 0);
        code[pc++] = laImm(0, 0, 0, 0, 2);

        var loop = codeLower + pc;
        code[pc++] = tz(JFIELD_W, 12, 0, 0, 2, dataLower);
        code[pc++] = locl(0, 0, 0, mark);
        code[pc++] = aaImm(0, 0, 0, 0, 1);
        code[pc++] = tleImm(0, 0, 0, 0, 999);
        code[pc++] = j(0, 0, 0, loop);
        code[pc] = 0;

        setup();

        var codeBD = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                         .setLowerLimit(codeLower >> 9)
                                         .setUpperLimit(codeUpper);
        var codeBDI = 0_000100;
        registerBankDescriptorViaLevelAndBDI(BANK_DESCRIPTOR_LEVEL, codeBDI, codeBD);
        loadBaseRegister((short) 0, false, codeLower, codeUpper, 0_10000, code);

        var dataBD = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                         .setLowerLimit(dataLower >> 9)
                                         .setUpperLimit(dataUpper);
        var dataBDI = 0_000101;
        registerBankDescriptorViaLevelAndBDI(BANK_DESCRIPTOR_LEVEL, dataBDI, dataBD);
        loadBaseRegister((short) 2, false, dataLower, dataUpper, 0_20000, data);

        var outputBD = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                           .setLowerLimit(outputLower >> 9)
                                           .setUpperLimit(outputUpper);
        var outputBDI = 0_000102;
        registerBankDescriptorViaLevelAndBDI(BANK_DESCRIPTOR_LEVEL, outputBDI, outputBD);
        loadBaseRegister((short) 3, false, outputLower, outputUpper, 0_30000, output);

        _engine.getProgramAddressRegister()
               .setProgramCounter(start)
               .setBankLevel(BANK_DESCRIPTOR_LEVEL)
               .setBankDescriptorIndex(codeBDI);

        _ignoreJumpHistoryInterrupt = true;
        run();

        assertArrayEquals(EXPECTED_VALUES, Arrays.copyOf(output, EXPECTED_VALUES.length));
    }
}

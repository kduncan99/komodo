/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_Algorithms extends Test_InstructionProcessor {

    /**
     * Sieve of Eratosthenes - Primes from 2 to 1000
     * <p>
     * @throws MachineInterrupt
     * @throws MaxNodesException
     * @throws NodeNameConflictException
     * @throws UPIConflictException
     * @throws UPINotAssignedException
     */
    @Test
    public void sieve(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {

        long[] flags = new long[1001];      //  flags - index is the integer, the flag is true (non-zero) if the integer is prime
                                            //  Based on B1

        long[] primes = new long[1001];     //  result. [0] is the number of values found,
                                            //          [1] and beyond are the values we've found, one integer per word
                                            //  Based on B2

        long[] code = {
            //  . Mark zero and one as non-prime (zero).
            //  00000   SZ          flags       . mark zero and one aas non-prime
            (new InstructionWord(05, 0, 0, 0, 0, 0, 01, 0)).getW(),
            //  00001   SZ          flags+1
            (new InstructionWord(05, 0, 0, 0, 0, 0, 01, 01)).getW(),

            //  . Mark numbers from two as prime (one).
            //  00002   LA,U        A2,998
            (new InstructionWord(010, 016, 02, 0, 0, 0, 998)).getW(),
            //  00003   LXI,U       X2,1
            (new InstructionWord(046, 016, 02, 0, 0, 0, 01)).getW(),
            //  00004   LXM,U       X2,2
            (new InstructionWord(026, 016, 02, 0, 0, 0, 02)).getW(),
            //  initloop .
            //  00005   SP1         flags,*X2
            (new InstructionWord(05, 0, 02, 02, 01, 0, 01, 0)).getW(),
            //  00006   JGD         A2,initloop
            (new InstructionWord(070, 0, 016, 0, 0, 0, 00005)).getW(),  //  016 is GRS index for A2

            //  . Initialize primes table - X4 is iterator to the table
            //  00007   SZ          primes+0
            (new InstructionWord(05, 0, 0, 0, 0, 0, 02, 0)).getW(),
            //  00010   LXI,U       X4,1
            (new InstructionWord(046, 016, 04, 0, 0, 0, 01)).getW(),
            //  00011   LXM,U       X4,1
            (new InstructionWord(026, 016, 04, 0, 0, 0, 01)).getW(),

            //  . A5 is the table index limit (0 to 1000)
            //  00012   LA,U        A5,1000
            (new InstructionWord(010, 016, 05, 0, 0, 0, 1000)).getW(),

            //  . Initialize X2 to point to the first prime number.
            //  . We can cheat because we know it is two.
            //  . X2 will point at each subsequent base prime number
            //  . through each iteration of the outer loop
            //  . X2.inc is already set to 1, so, yay.
            //  00013   LXM,U       X2,2
            (new InstructionWord(026, 016, 02, 0, 0, 0, 02)).getW(),

            //  outerloop .

            //  . is the number in *X2.mod a prime?  If not, skip innerloop
            //  . remember: X2.mod is the candidate number,
            //  . *X2 is prime / not prime
            //  00014   TNZ         flags,X2,B1      . is X2.mod prime?
            (new InstructionWord(050, 0, 011, 02, 0, 0, 01, 0)).getW(),
            //  00015   J           outeriter   . No - skip inner loop
            (new InstructionWord(074, 015, 04, 0, 0, 0, 00032)).getW(),

            //  . X2.mod is prime - append it to the primes table
            //  0016    SX,H2       X2,primes,X4
            (new InstructionWord(06, 01, 02, 04, 0, 0, 02, 0)).getW(),
            //  0017    SZ,H1       primes,*X4
            (new InstructionWord(05, 02, 0, 04, 01, 0, 02, 0)).getW(),
            //  00020   ADD1        primes
            (new InstructionWord(05, 0, 015, 0, 0, 0, 2, 0)).getW(),

            //  . Mark all multiples of this prime as non-prime
            //  . (careful not to mark the prime itself as non-prime)
            //  . Use X3 as the incrementer/offset for the inner loop
            //  00021   LXI         X3,X2       . Set X3.mod to the next multiple
            (new InstructionWord(046, 0, 03, 0, 0, 0, 0, 02)).getW(),
            //  00022   LXM         X3,X2       . of X2.mod, and X3.inc to X2.mod.
            (new InstructionWord(026, 0, 03, 0, 0, 0, 0, 02)).getW(),
            //  00023   NOP         0,*X3
            (new InstructionWord(073, 014, 0, 03, 01, 0, 01, 0)).getW(),

            //  innerloop .
            //  00024   LXM         X12,X3      . Is X3 beyond the table? (X12 == A0)
            (new InstructionWord(026, 0, 014, 0, 0, 0, 0, 03)).getW(),
            //  00025   LXI,U       X12,0
            (new InstructionWord(046, 016, 014, 0, 0, 0, 0)).getW(),
            //  00026   TG          A0,A5 . is A5 > A0?
            (new InstructionWord(055, 0, 0, 0, 0, 0, 0, 021)).getW(),   //  A5 is GRS index 021
            //  00027   J           outeriter   . No, iterate
            (new InstructionWord(074, 015, 04, 0, 0, 0, 00032)).getW(),
            //  00030   SZ          0,*X3,B1    . Set a multiple as non-prime
            (new InstructionWord(05, 0, 0, 03, 01, 0, 01, 0)).getW(),
            //  0031    J           innerloop   . Iterate
            (new InstructionWord(074, 015, 04, 0, 0, 0, 00024)).getW(),

            //  outeriter .
            //  00032   NOP         0,*X2       . Go to next candidate
            (new InstructionWord(073, 014, 0, 02, 01, 0, 01, 0)).getW(),
            //  00033   LXM         X12,X2      . Is X2 beyond the table? (X12 == A0)
            (new InstructionWord(026, 0, 014, 0, 0, 0, 0, 02)).getW(),
            //  00034   LXI,U       X12,0
            (new InstructionWord(046, 016, 014, 0, 0, 0, 0)).getW(),
            //  00035   TLE         A0,A5 . is A5 <= A0?
            (new InstructionWord(054, 0, 0, 0, 0, 0, 0, 021)).getW(),   //  A5 is GRS index 021
            //  00036   J           outerloop   . No, iterate
            (new InstructionWord(074, 015, 04, 0, 0, 0, 00014)).getW(),

            //  . Stop
            //  00037   IAR         0
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),
        };

        long[][] sourceData = { code, flags, primes };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setExecutive24BitIndexingEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] expected = {
            168,    //  number of prime values following
            2,   3,   5,   7,   11,  13,  17,  19,  23,  29,
            31,  37,  41,  43,  47,  53,  59,  61,  67,  71,
            73,  79,  83,  89,  97,  101, 103, 107, 109, 113,
            127, 131, 137, 139, 149, 151, 157, 163, 167, 173,
            179, 181, 191, 193, 197, 199, 211, 223, 227, 229,
            233, 239, 241, 251, 257, 263, 269, 271, 277, 281,
            283, 293, 307, 311, 313, 317, 331, 337, 347, 349,
            353, 359, 367, 373, 379, 383, 389, 397, 401, 409,
            419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
            467, 479, 487, 491, 499, 503, 509, 521, 523, 541,
            547, 557, 563, 569, 571, 577, 587, 593, 599, 601,
            607, 613, 617, 619, 631, 641, 643, 647, 653, 659,
            661, 673, 677, 683, 691, 701, 709, 719, 727, 733,
            739, 743, 751, 757, 761, 769, 773, 787, 797, 809,
            811, 821, 823, 827, 829, 839, 853, 857, 859, 863,
            877, 881, 883, 887, 907, 911, 919, 929, 937, 941,
            947, 953, 967, 971, 977, 983, 991, 997
        };

        long[] result = getBank(ip, 2);
        assertArrayEquals(Arrays.copyOf(expected, result.length), result);
    }
}

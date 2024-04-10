/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_Algorithms extends BaseFunctions {

    //TODO fibonacci numbers less than 1000

    @Test
    public void factorial(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            ".",
            "$(0)",
            ". STACK FOR GENERAL USE",
            "STACKSIZE $EQU      128",
            "STACK     $RES      STACKSIZE",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(3)      $LIT",
            "          . Literals go in here so they are accessible before",
            "          . the DBank is based on B2.",
            "",
            "$(1),START",
            ".",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000021,0)",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+STACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (000020,0)",
            ".",
            "          . ESTABLISH GENERAL STACK ON B2/X2",
            "          LBU       B2,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     X2,0",
            "          LXM,U     X2,STACK+STACKSIZE",
            ".",
            "          BUY       2,X2,B2",
            "          LA,U      A5,10               . calculate 10!",
            "          SA        A5,0,X2,B2",
            "          LOCL      FACTORIAL",
            "          LA        A5,1,X2,B2",
            "          SELL      2,X2,B2",
            ".",
            "          HALT      0",
            ".",
            ". .............................................................................",
            ". Assumes generic stack exists on B2/X2, with a two-word stack frame allocated.",
            ". The first word of the stack frame is the parameter - i.e., for 5, we return",
            ". 5!.  The second word of the stack frame is where we place the returned value.",
            ". .............................................................................",
            ".",
            "FACTORIAL .",
            "          LA        A5,0,X2,B2",
            "          TP        A5",
            "          HALT      077                 . We don't like negative numbers",
            "          TNZ       A5",
            "          HALT      076                 . We also don't like zero",
            "          TNE,U     A5,1",
            "          J         FACTORIAL1",
            ".",
            "          . Operand greater than one, recurse with operand - 1.",
            "          BUY       2,X2,B2",
            "          ANA,U     A5,1",
            "          SA        A5,0,X2,B2",
            "          LOCL      FACTORIAL",
            ".",
            "          . Get result, multiply by original operand, and return it",
            "          LA        A5,1,X2,B2",
            "          SELL      2,X2,B2",
            "          MSI       A5,0,X2,B2",
            "          SA        A5,1,X2,B2",
            "          RTN       0",
            ".",
            "FACTORIAL1 . Operand is one - return one",
            "          SP1       1,X2,B2",
            "          RTN       0",
            ".",
            "DESREG    $RES      1",
            ".",
            "          $END START"
        };

        buildDualBank(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(3628800, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());

        clear();
    }

    /**
     * Sieve of Eratosthenes - Primes from 2 to 1000
     */
    @Test
    public void sieve(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "FLAGS     $RES 1001                     . Index is the integer, the flag is true",
            "                                        .   (non-zero) if the integer is prime.",
            "",
            "$(2)",
            "PRIMES    $RES 1001                     . Result - [0] is the number of values found,",
            "                                        .          [1] and beyond are the values we've found,",
            "                                        .              one integer per word.",
            "",
            "$(1)      .",
            "START",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          . AND BASE THE DATABANK ON B2",
            "          LD        DESREG",
            "          LBU       B2,DATA0BDI",
            "          LBU       B3,DATA2BDI",
            ".",
            "          . Mark zero and one as non-prime (zero).",
            "          SZ        flags,,B2",
            "          SZ        flags+1,,B2",
            "",
            "          . Mark numbers from two as prime (one).",
            "          LA,U      A2,998",
            "          LXI,U     X2,1",
            "          LXM,U     X2,2",
            "INITLOOP  SP1       flags,*X2,B2",
            "          JGD       A2,initloop",
            "",
            "          . Initialize primes table...",
            "          . X4 indexes the next free entry in the table",
            "          SZ        primes,,B3",
            "          LXI,U     X4,1",
            "          LXM,U     X4,1",
            "",
            "          . A5 is the table index limit (0 to 1000)",
            "          LA,U      A5,1000",
            "",
            "          . Initialize X2 to point to the first prime number.",
            "          . We can cheat because we know it is two.",
            "          . X2 will point at each subsequent base prime number",
            "          . through each iteration of the outer loop",
            "          . X2.inc is already set to 1, so, yay.",
            "          LXM,U     X2,2",
            "",
            "OUTERLOOP .",
            "          . is the number in *X2.mod a prime?  If not, skip innerloop",
            "          . remember: X2.mod is the candidate number,",
            "          . *X2 is prime / not prime,",
            "          TNZ       flags,X2,B2         . is X2.mod prime?",
            "          J         outeriter           . No - skip inner loop",
            "",
            "          . X2.mod is prime - append it to the primes table",
            "          SX,H2     X2,primes,X4,B3",
            "          SZ,H1     primes,*X4,B3",
            "          ADD1      primes,,B3",
            "",
            "          . Mark all multiples of this prime as non-prime",
            "          . (careful not to mark the prime itself as non-prime)",
            "          . Use X3 as the incrementer/offset for the inner loop",
            "          LXI       X3,X2               . Set X3.mod to the next multiple",
            "          LXM       X3,X2               . of X2.mod, and X3.inc to X2.mod.",
            "          NOP       0,*X3",
            "",
            "INNERLOOP .",
            "          LXM       X12,X3              . Is X3 beyond the table? (X12 == A0)",
            "          LXI,U     X12,0",
            "          TG        A0,A5               . is A5 > A0?",
            "          J         outeriter           . No, iterate",
            "          SZ        primes,*X3,B2       . Set a multiple as non-prime",
            "          J         innerloop           . Iterate",
            "",
            "OUTERITER .",
            "          NOP       0,*X2               . Go to next candidate",
            "          LXM       X12,X2              . Is X2 beyond the table? (X12 == A0)",
            "          LXI,U     X12,0",
            "          TLE       A0,A5               . is A5 <= A0?",
            "          J         outerloop           . No, iterate",
            "",
            "          . Stop",
            "          HALT      0",
            "",
            ". Data that must be in the code bank as it is required before data banks are based",
            "DESREG    +000020, 0",
            "DATA0BDI  +LBDIREF$+FLAGS, 0            . to be based on B2",
            "DATA2BDI  +LBDIREF$+PRIMES, 0           . to be based on B3",
            "",
            "          $END      START",
        };

        buildMultiBank(source, false, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

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

        long[] result = getBankByBaseRegister(3);
        assertArrayEquals(Arrays.copyOf(expected, result.length), result);

        clear();
    }
}

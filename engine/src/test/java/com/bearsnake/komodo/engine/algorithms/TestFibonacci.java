/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.algorithms;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Finds the first 30 numbers in the Fibonacci sequence.
 */
public class TestFibonacci extends FunctionUnitTest {

    private static final long[] EXPECTED_VALUES = {
        1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987,
        1597, 2584, 4181, 6765, 10946, 17711, 28657, 46368, 75025, 121393,
        196418, 317811, 514229, 832040
    };

    private long aa(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(014, j, a, x, h, i, b, d);
    }

    private long jgd(long grs, long x, long h, long i, long u) {
        return fjaxhiu(070, grs >> 4, grs, x, h, i, u);
    }

    private long la(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(010, j, a, x, h, i, b, d);
    }

    private long lxi(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(046, j, a, x, h, i, b, d);
    }

    private long lxm(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(026, j, a, x, h, i, b, d);
    }

    private long sa(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(01, j, a, x, h, i, b, d);
    }

    private long sp1(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(05, j, 02, x, h, i, b, d);
    };

    @Test
    public void testFibonacci() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        // The program is as follows:
        // START .
        //      SP1     data,B2     . Assume the 1st 2 numbers are already known
        //      SP1     data+1,B2
        //      LA,U    A9,27       . We need 28 more numbers
        //      LXI,U   X2,1
        //      LXM,U   X2,0        . Point X2 to the 1st already known number
        // LOOP .
        //      LA      A5,data,X2,B2
        //      AA      A5,data+1,X2,B2
        //      SA      A5,data+2,*X2,B2
        //      JGD     A9,LOOP
        //      $END

        var codeLower = 0_1000;
        var dataLower = 0_1000;

        code[0] = sp1(JFIELD_W, 0, 0, 0, 2, dataLower);
        code[1] = sp1(JFIELD_W, 0, 0, 0, 2, dataLower + 1);
        code[2] = la(JFIELD_U, 9, 0, 0, 0, 0, 27);
        code[3] = lxi(JFIELD_U, 2, 0, 0, 0, 0, 1);
        code[4] = lxm(JFIELD_U, 2, 0, 0, 0, 0, 0);
        var loop = 01005;
        code[5] = la(JFIELD_W, 5, 2, 0, 0, 2, dataLower);
        code[6] = aa(JFIELD_W, 5, 2, 0, 0, 2, dataLower + 1);
        code[7] = sa(JFIELD_W, 5, 2, 1, 0, 2, dataLower + 2);
        code[8] = jgd(GRS_A9, 0, 0, 0, loop);

        _engine = new Engine(this, this);

        loadBaseRegister((short) 0, false, codeLower, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, dataLower, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertArrayEquals(EXPECTED_VALUES, Arrays.copyOf(data, 30));
    }
}

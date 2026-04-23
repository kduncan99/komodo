/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

public abstract class FunctionUnitTest extends EngineUnitTest {

    protected long fjaxhibd(long f, long j, long a, long x, long h, long i, long b, long d) {
        return ((f & 077) << 30) | ((j & 017) << 26) | ((a & 017) << 22) | ((x & 017) << 18)
               | ((h & 01) << 17) | ((i & 01) << 16) | ((b & 017) << 12) | (d & 07777);
    }

    protected long fjaxhiu(long f, long j, long a, long x, long h, long i, long u) {
        return ((f & 077) << 30) | ((j & 017) << 26) | ((a & 017) << 22) | ((x & 017) << 18)
               | ((h & 01) << 17) | ((i & 01) << 16) | (u & 0177777);
    }

    protected long fjaxu(long f, long j, long a, long x, long u) {
        return ((f & 077) << 30) | ((j & 017) << 26) | ((a & 017) << 22) | ((x & 017) << 18) | (u & 0777777);
    }

    protected long data(long w) {
        return w;
    }

    protected long data(long h1, long h2) {
        return ((h1 & 0777777) << 18) | (h2 & 0777777);
    }

    protected long data(long t1, long t2, long t3) {
        return ((t1 & 07777) << 24) | ((t2 & 07777) << 12) | (t3 & 07777);
    }

    protected long data(long q1, long q2, long q3, long q4) {
        return ((q1 & 0777) << 27) | ((q2 & 0777) << 18) | ((q3 & 0777) << 9) | (q4 & 0777);
    }

    protected long data(long s1, long s2, long s3, long s4, long s5, long s6) {
        return ((s1 & 077) << 30) | ((s2 & 077) << 24) | ((s3 & 077) << 18) | ((s4 & 077) << 12) | ((s5 & 077) << 6) | (s6 & 077);
    }

    protected MachineInterrupt _interrupt = null;

    protected void run() throws MachineInterrupt {
        _engine.halt(HaltCode.NONE);
        _interrupt = null;
        while (!_engine.isHalted()) {
            _engine.cycle();
        }

        if (_interrupt != null) {
            if ((_interrupt.getInterruptClass() == MachineInterrupt.InterruptClass.InvalidInstruction)
                && (_engine.getCurrentInstruction().getW() == 0)) {
                // this is normal...ish. Anyway, it's how all the unit tests halt the engine.
            } else {
                System.out.println("Interrupt: " + _interrupt);
                System.out.printf("  SSF:  %02o\n", _interrupt.getShortStatusField());
                System.out.printf("  ISW0: %012o\n", _interrupt.getInterruptStatusWord0());
                System.out.printf("  ISW1: %012o\n", _interrupt.getInterruptStatusWord1());
                System.out.printf("PC: %o:%05o:%06o\n",
                                  _engine.getProgramAddressRegister().getBankLevel(),
                                  _engine.getProgramAddressRegister().getBankDescriptorIndex(),
                                  _engine.getProgramAddressRegister().getProgramCounter());
                System.out.printf("DR: %012o\n", _engine.getDesignatorRegister().getCompositeValue());
                for (int i = 0; i < 16; i++) {
                    var br = _engine.getBaseRegister(i);
                    if (!br.isVoid() || (i == 0)) {
                        if (br.isVoid()) {
                            System.out.printf("B%02d:VOID\n", i);
                        } else {
                            System.out.printf("B%-2d: addr:%s ll=%06o ul=%06o\n",
                                              i,
                                              br.getBaseAddress(),
                                              br.getLowerLimitNormalized(),
                                              br.getUpperLimitNormalized());
                        }
                    }
                }
                throw _interrupt;
            }
        }
    }

    @Override
    public void handleInterrupt(
        final MachineInterrupt interrupt
    ) {
        _interrupt = interrupt;
        _engine.halt(HaltCode.UNIT_TEST_STOP);
    }
}

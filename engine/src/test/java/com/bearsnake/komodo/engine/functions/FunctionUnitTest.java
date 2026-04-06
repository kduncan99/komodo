/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.EngineUnitTest;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

public abstract class FunctionUnitTest extends EngineUnitTest {

    protected Engine _engine;

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

    protected void run() throws MachineInterrupt {
        for (;;) {
            try {
                _engine.cycle();
            } catch (MachineInterrupt interrupt) {
                if (interrupt.getInterruptClass() == MachineInterrupt.InterruptClass.InvalidInstruction) {
                    var ci = _engine.getCurrentInstruction();
                    if (ci.getW() == 0) {
                        // this is our normal stop
                        break;
                    }
                }

                throw interrupt;
            } catch (EngineHaltedException ex) {
                IO.println("Engine Halted");
                break;
            }
        }
    }
}

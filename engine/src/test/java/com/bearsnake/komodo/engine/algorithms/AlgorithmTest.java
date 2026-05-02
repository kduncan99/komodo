/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.algorithms;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.HardwareCheckInterrupt;

import static com.bearsnake.komodo.engine.Constants.JFIELD_U;
import static com.bearsnake.komodo.engine.Constants.JFIELD_W;

public abstract class AlgorithmTest extends FunctionUnitTest {

    protected static final short BANK_DESCRIPTOR_LEVEL = 0;

    protected long aa(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(014, j, a, x, h, i, b, d);
    }

    protected long aaImm(long a, long x, long h, long i, long u) {
        return fjaxhiu(014, JFIELD_U, a, x, h, i, u);
    }

    protected long aaGRS(long a, long x, long h, long i, long grs) {
        return fjaxhiu(014, JFIELD_W, a, x, h, i, grs & 0177);
    }

    protected long anx(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(025, j, a, x, h, i, b, d);
    }

    protected long anxImm(long a, long x, long h, long i, long u) {
        return fjaxhiu(025, JFIELD_U, a, x, h, i, u);
    }

    protected long ax(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(024, j, a, x, h, i, b, d);
    }

    protected long axGRS(long a, long x, long h, long i, long grs) {
        return fjaxhiu(024, JFIELD_W, a, x, h, i, grs & 0177);
    }

    protected long j(long x, long h, long i, long u) {
        return fjaxhiu(074, 015, 04, x, h, i, u);
    }

    protected long jgd(long grs, long x, long h, long i, long u) {
        return fjaxhiu(070, grs >> 4, grs, x, h, i, u);
    }

    protected long la(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(010, j, a, x, h, i, b, d);
    }

    protected long laImm(long a, long x, long h, long i, long u) {
        return fjaxhiu(010, JFIELD_U, a, x, h, i, u);
    }

    protected long laGRS(long a, long x, long h, long i, long grs) {
        return fjaxhiu(010, JFIELD_W, a, x, h, i, grs & 0177);
    }

    protected long locl(long x, long h, long i, long u) {
        return fjaxhiu(07, 016, 00, x, h, i, u);
    }

    protected long lssl(long a, long x, long h, long i, long u) {
        return fjaxhiu(073, 012, a, x, h, i, u);
    }

    protected long lx(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(027, j, a, x, h, i, b, d);
    }

    protected long lxGRS(long a, long x, long h, long i, long grs) {
        return fjaxhibd(027, JFIELD_W, a, x, h, i, 0, grs & 0_177);
    }

    protected long lxi(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(046, j, a, x, h, i, b, d);
    }

    protected long lxiImm(long a, long x, long h, long i, long u) {
        return fjaxhiu(046, JFIELD_U, a, x, h, i, u);
    }

    protected long lxiGRS(long a, long x, long h, long grs) {
        return fjaxhibd(046, JFIELD_W, a, x, h, 0, 0, grs & 0_177);
    }

    protected long lxm(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(026, j, a, x, h, i, b, d);
    }

    protected long lxmImm(long a, long x, long h, long i, long u) {
        return fjaxhiu(026, JFIELD_U, a, x, h, i, u);
    }

    protected long lxmGRS(long a, long x, long h, long grs) {
        return fjaxhibd(026, JFIELD_W, a, x, h, 0, 0, grs & 0_177);
    }

    protected long nop(long x, long h, long i, long b, long d) {
        return fjaxhibd(073, 014, 0, x, h, i, b, d);
    }

    protected long rtn() {
        return fjaxhibd(073, 017, 03, 0, 0, 0, 0, 0);
    }

    protected long sa(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(01, j, a, x, h, i, b, d);
    }

    protected long sp1(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(05, j, 02, x, h, i, b, d);
    }

    protected long sx(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(06, j, a, x, h, i, b, d);
    }

    protected long sz(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(05, j, 00, x, h, i, b, d);
    }

    protected long tg(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(055, j, a, x, h, i, b, d);
    }

    protected long tgImm(long a, long x, long h, long i, long u) {
        return fjaxhiu(055, JFIELD_U, a, x, h, i, u);
    }

    protected long tle(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(054, j, a, x, h, i, b, d);
    }

    protected long tleImm(long a, long x, long h, long i, long u) {
        return fjaxhiu(054, JFIELD_U, a, x, h, i, u);
    }

    protected long tleGRS(long a, long x, long h, long i, long grs) {
        return fjaxhibd(054, JFIELD_W, a, x, h, i, 0, grs & 0_177);
    }

    protected long tz(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 06, x, h, i, b, d);
    }

    protected void setup() throws HardwareCheckInterrupt {
        _engine = new Engine(this, this);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        var destinationBDTSegIndex = createBankDescriptorTable(1024);
        loadBankDescriptorTableToBaseRegister(destinationBDTSegIndex, BANK_DESCRIPTOR_LEVEL);

        createReturnControlStack(0_1000, 0_1000);
    }
}

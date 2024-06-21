/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.Configuration;
import com.bearsnake.komodo.kexec.exec.BatchRun;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.Run;
import com.bearsnake.komodo.logger.Level;
import com.bearsnake.komodo.logger.LogManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestInterpreter {

    private Run _rce;

    @Before
    public void setup() throws Parser.SyntaxException {
        LogManager.setGlobalEnabled(true);
        LogManager.setGlobalLevel(Level.Trace);
        var ex = new Exec(new boolean[36]);
        ex.setConfiguration(new Configuration());
        var rci = RunCardInfo.parse(_rce, "@RUN TEST");
        _rce = new BatchRun("RUNID", rci);
    }

    @Test
    public void testEmpty() {
        var text = "@ . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertNull(ps._mnemonic);
        assertNull(ps._label);
        assertTrue(ps._optionsFields.isEmpty());
        assertTrue(ps._operandFields.isEmpty());

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testEmptyWithLabel() {
        var text = "@label: . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertNull(ps._mnemonic);
        assertEquals("LABEL", ps._label);
        assertTrue(ps._optionsFields.isEmpty());
        assertTrue(ps._operandFields.isEmpty());

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testEmptyWithInvalidLabel1() {
        var text = "@labe$l: . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertTrue(ps._facStatusResult.hasErrorMessages());
        assertEquals(0_400000_000000L, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testEmptyWithInvalidLabel2() {
        var text = "@veryLongLabel: . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertTrue(ps._facStatusResult.hasErrorMessages());
        assertEquals(0_400000_000000L, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testEmptyWithSimpleMnemonic() {
        var text = "@test . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertEquals("TEST", ps._mnemonic);
        assertNull(ps._label);
        assertTrue(ps._optionsFields.isEmpty());
        assertTrue(ps._operandFields.isEmpty());

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testEmptyWithInvalidMnemonic1() {
        var text = "@labe$l . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertTrue(ps._facStatusResult.hasErrorMessages());
        assertEquals(0_400000_000000L, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testEmptyWithInvalidMnemonic2() {
        var text = "@veryLongMnemonic . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertTrue(ps._facStatusResult.hasErrorMessages());
        assertEquals(0_400000_000000L, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testEmptyWithLabelAndMnemonic() {
        var text = "@label: test . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertEquals("TEST", ps._mnemonic);
        assertEquals("LABEL", ps._label);
        assertTrue(ps._optionsFields.isEmpty());
        assertTrue(ps._operandFields.isEmpty());

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testLog() {
        var text = "@label: log   This is a funny frog . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertEquals("LOG", ps._mnemonic);
        assertEquals("LABEL", ps._label);
        assertTrue(ps._optionsFields.isEmpty());
        assertEquals(1, ps._operandFields.size());
        assertEquals("This is a funny frog",
                     ps._operandFields.get(new SubfieldSpecifier(0, 0)));

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testOptions() {
        var text = "@foo,opt1///opt2/opt3";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertEquals("FOO", ps._mnemonic);
        assertNull(ps._label);
        assertEquals(5, ps._optionsFields.size());
        assertEquals("opt1", ps._optionsFields.get(0));
        assertEquals("", ps._optionsFields.get(1));
        assertEquals("", ps._optionsFields.get(2));
        assertEquals("opt2", ps._optionsFields.get(3));
        assertEquals("opt3", ps._optionsFields.get(4));
        assertEquals(0, ps._operandFields.size());

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testAsg() {
        var text = "@asg qual*file(3)/read/write, , a/  bbb/  cat . comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertEquals("ASG", ps._mnemonic);
        assertNull(ps._label);
        assertEquals(0, ps._optionsFields.size());
        assertEquals(3, ps._operandFields.size());
        assertEquals(1, ps._operandFields.get(0).size());
        assertEquals("qual*file(3)/read/write", ps._operandFields.get(0).get(0));
        assertEquals(1, ps._operandFields.get(1).size());
        assertEquals(3, ps._operandFields.get(2).size());
        assertEquals("a", ps._operandFields.get(2).get(0));
        assertEquals("bbb", ps._operandFields.get(2).get(1));
        assertEquals("cat", ps._operandFields.get(2).get(2));

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testCat() {
        var text = "@cat,pg qual*file(3)/read/write, , a/  bbb/  cat comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertEquals("CAT", ps._mnemonic);
        assertNull(ps._label);
        assertEquals(1, ps._optionsFields.size());
        assertEquals(3, ps._operandFields.size());
        assertEquals(1, ps._operandFields.get(0).size());
        assertEquals("qual*file(3)/read/write", ps._operandFields.get(0).get(0));
        assertEquals(1, ps._operandFields.get(1).size());
        assertEquals(3, ps._operandFields.get(2).size());
        assertEquals("a", ps._operandFields.get(2).get(0));
        assertEquals("bbb", ps._operandFields.get(2).get(1));
        assertEquals("cat", ps._operandFields.get(2).get(2));

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }

    @Test
    public void testNormal() {
        var text = "@normal,pg qual*file(3)/read/write, , a/  bbb/  cat comment";
        var ps = Interpreter.parseControlStatement(_rce, text);
        ps._facStatusResult.dump(System.out, "");

        assertEquals("NORMAL", ps._mnemonic);
        assertNull(ps._label);
        assertEquals(1, ps._optionsFields.size());
        assertEquals(3, ps._operandFields.size());
        assertEquals(3, ps._operandFields.get(0).size());
        assertEquals("qual*file(3)", ps._operandFields.get(0).get(0));
        assertEquals("read", ps._operandFields.get(0).get(1));
        assertEquals("write", ps._operandFields.get(0).get(2));
        assertEquals(1, ps._operandFields.get(1).size());
        assertEquals(3, ps._operandFields.get(2).size());
        assertEquals("a", ps._operandFields.get(2).get(0));
        assertEquals("bbb", ps._operandFields.get(2).get(1));
        assertEquals("cat", ps._operandFields.get(2).get(2));

        assertFalse(ps._facStatusResult.hasErrorMessages());
        assertFalse(ps._facStatusResult.hasInfoMessages());
        assertFalse(ps._facStatusResult.hasWarningMessages());
        assertEquals(0, ps._facStatusResult.getStatusWord());
    }
}

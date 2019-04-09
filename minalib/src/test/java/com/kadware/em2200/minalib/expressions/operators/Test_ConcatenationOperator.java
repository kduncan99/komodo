/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Stack;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kduncan
 */
public class Test_ConcatenationOperator {

    @Test
    public void simple_ASCII(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue("ABC"));
        valueStack.push(new StringValue("DEF"));

        Context context = new Context();
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertNull(vResult.getForm());
        assertNull(vResult.getRelocationInfo());
        assertEquals("ABCDEF", vResult.getValue());
        assertEquals(CharacterMode.ASCII, vResult.getCharacterMode());
    }

    @Test
    public void simple_Fdata(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue("ABC", CharacterMode.Fieldata));
        valueStack.push(new StringValue("DEF", CharacterMode.Fieldata));

        Context context = new Context();
        context.setCharacterMode(CharacterMode.Fieldata);
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertNull(vResult.getForm());
        assertNull(vResult.getRelocationInfo());
        assertEquals("ABCDEF", vResult.getValue());
        assertEquals(CharacterMode.Fieldata, vResult.getCharacterMode());
    }

    @Test
    public void simple_mixed(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue("ABC", CharacterMode.Fieldata));
        valueStack.push(new StringValue("DEF", CharacterMode.ASCII));

        Context context = new Context();
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertNull(vResult.getForm());
        assertNull(vResult.getRelocationInfo());
        assertEquals("ABCDEF", vResult.getValue());
        assertEquals(CharacterMode.ASCII, vResult.getCharacterMode());
    }

    @Test
    public void integer_ASCII(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue(0_101_102_103_104l));
        valueStack.push(new IntegerValue(0_105_106_107_110l));

        Context context = new Context();
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertNull(vResult.getForm());
        assertNull(vResult.getRelocationInfo());
        assertEquals("ABCDEFGH", vResult.getValue());
        assertEquals(CharacterMode.ASCII, vResult.getCharacterMode());
    }

    @Test
    public void integer_Fdata(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue(0_06_07_10_11_12_13l, false, Signed.None, Precision.None, null, null));
        valueStack.push(new IntegerValue(0_14_15_16_17_20_21l, false, Signed.None, Precision.Double, null, null));

        Context context = new Context();
        context.setCharacterMode(CharacterMode.Fieldata);
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertNull(vResult.getForm());
        assertNull(vResult.getRelocationInfo());
        assertEquals("ABCDEF@@@@@@GHIJKL", vResult.getValue());
        assertEquals(CharacterMode.Fieldata, vResult.getCharacterMode());
    }

    @Test
    public void integer_Fdata_reloc_truncation(
    ) throws ExpressionException {
        long[] doubleWord = {
            0_14_15_16_17_20_21l,
            0_22_23_24_25_26_27l,
        };

        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue(0_06_07_10_11_12_13l, false, Signed.None, Precision.None, null, null));
        valueStack.push(new IntegerValue(doubleWord, false, Signed.None, Precision.Single, null, null));

        Context context = new Context();
        context.setCharacterMode(CharacterMode.Fieldata);
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        Diagnostic[] diagArray = diags.getDiagnostics();
        assertEquals(1, diagArray.length);
        assertEquals(Diagnostic.Level.Truncation, diagArray[0].getLevel());

        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertNull(vResult.getForm());
        assertNull(vResult.getRelocationInfo());
        assertEquals("ABCDEFMNOPQR", vResult.getValue());
        assertEquals(CharacterMode.Fieldata, vResult.getCharacterMode());
    }

    @Test
    public void incompatibleType(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue("ABC", false, Signed.None, Precision.None, CharacterMode.ASCII));
        valueStack.push(new FloatingPointValue(1.0, false, Signed.None, Precision.None));

        Context context = new Context();
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        try {
            op.evaluate(context, valueStack, diags);
        } catch (ExpressionException ex) {
            //  drop through
        }

        Diagnostic[] diagArray = diags.getDiagnostics();
        assertEquals(1, diagArray.length);
        assertEquals(Diagnostic.Level.Value, diagArray[0].getLevel());
    }
}

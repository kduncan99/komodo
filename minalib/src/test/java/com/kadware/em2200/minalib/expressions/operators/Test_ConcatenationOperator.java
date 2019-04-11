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
        valueStack.push(new StringValue.Builder().setValue("ABC").build());
        valueStack.push(new StringValue.Builder().setValue("DEF").build());

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
        valueStack.push(new StringValue.Builder().setValue("ABC").setCharacterMode(CharacterMode.Fieldata).build());
        valueStack.push(new StringValue.Builder().setValue("DEF").setCharacterMode(CharacterMode.Fieldata).build());

        Context context = new Context();
        context._characterMode = CharacterMode.Fieldata;
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
        valueStack.push(new StringValue.Builder().setValue("ABC").setCharacterMode(CharacterMode.Fieldata).build());
        valueStack.push(new StringValue.Builder().setValue("DEF").setCharacterMode(CharacterMode.ASCII).build());

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
        valueStack.push(new IntegerValue.Builder().setValue(0_101_102_103_104l).build());
        valueStack.push(new IntegerValue.Builder().setValue(0_105_106_107_110l).build());

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
        valueStack.push(new IntegerValue.Builder().setValue(0_06_07_10_11_12_13l).build());
        valueStack.push(new IntegerValue.Builder().setValue(0_14_15_16_17_20_21l).setPrecision(Precision.Double).build());

        Context context = new Context();
        context._characterMode = CharacterMode.Fieldata;
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
        valueStack.push(new IntegerValue.Builder().setValue(0_06_07_10_11_12_13l).build());
        valueStack.push(new IntegerValue.Builder().setValue(doubleWord).setPrecision(Precision.Single).build());

        Context context = new Context();
        context._characterMode = CharacterMode.Fieldata;
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
        valueStack.push(new StringValue.Builder().setValue("ABC").setCharacterMode(CharacterMode.ASCII).build());
        valueStack.push(new FloatingPointValue.Builder().setValue(1.0).build());

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

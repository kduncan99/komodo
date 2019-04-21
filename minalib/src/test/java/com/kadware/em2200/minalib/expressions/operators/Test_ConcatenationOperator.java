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
        valueStack.push(new StringValue(false, "ABC", CharacterMode.ASCII));
        valueStack.push(new StringValue(false, "DEF", CharacterMode.ASCII));

        Context context = new Context();
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult.getFlagged());
        assertEquals("ABCDEF", vResult.getValue());
        assertEquals(CharacterMode.ASCII, vResult.getCharacterMode());
    }

    @Test
    public void simple_Fdata(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue(false, "ABC", CharacterMode.Fieldata));
        valueStack.push(new StringValue(false, "DEF", CharacterMode.Fieldata));

        Context context = new Context();
        context._characterMode = CharacterMode.Fieldata;
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult.getFlagged());
        assertEquals("ABCDEF", vResult.getValue());
        assertEquals(CharacterMode.Fieldata, vResult.getCharacterMode());
    }

    @Test
    public void simple_mixed(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue(false, "ABC", CharacterMode.Fieldata));
        valueStack.push(new StringValue(false, "DEF", CharacterMode.ASCII));

        Context context = new Context();
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult.getFlagged());
        assertEquals("ABCDEF", vResult.getValue());
        assertEquals(CharacterMode.ASCII, vResult.getCharacterMode());
    }

    @Test
    public void integer_ASCII(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue(false, 0_101_102_103_104L, null));
        valueStack.push(new IntegerValue(false, 0_105_106_107_110L, null));

        Context context = new Context();
        Diagnostics diags = new Diagnostics();

        Operator op = new ConcatenationOperator(new Locale(12, 18));
        op.evaluate(context, valueStack, diags);

        assertEquals(0, diags.getDiagnostics().length);
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult.getFlagged());
        assertEquals("ABCDEFGH", vResult.getValue());
        assertEquals(CharacterMode.ASCII, vResult.getCharacterMode());
    }

    @Test
    public void incompatibleType(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue(false, "ABC", CharacterMode.ASCII));
        valueStack.push(new FloatingPointValue(false, 1.0));

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

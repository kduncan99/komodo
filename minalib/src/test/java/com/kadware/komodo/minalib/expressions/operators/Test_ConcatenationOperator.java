/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;

import java.util.List;
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

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals("ABCDEF", vResult._value);
        assertEquals(CharacterMode.ASCII, vResult._characterMode);
    }

    @Test
    public void simple_Fdata(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue(false, "ABC", CharacterMode.Fieldata));
        valueStack.push(new StringValue(false, "DEF", CharacterMode.Fieldata));

        Context context = new Context(new Dictionary(), new String[0],  "TEST");
        context.setCharacterMode(CharacterMode.Fieldata);
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 10);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals("ABCDEF", vResult._value);
        assertEquals(CharacterMode.Fieldata, vResult._characterMode);
    }

    @Test
    public void simple_mixed(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue(false, "ABC", CharacterMode.Fieldata));
        valueStack.push(new StringValue(false, "DEF", CharacterMode.ASCII));

        Context context = new Context(new Dictionary(), new String[0],  "TEST");
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 22);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals("ABCDEF", vResult._value);
        assertEquals(CharacterMode.ASCII, vResult._characterMode);
    }

    @Test
    public void integer_ASCII(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue(0_101_102_103_104L));
        valueStack.push(new IntegerValue(0_105_106_107_110L));

        Context context = new Context(new Dictionary(), new String[0],  "TEST");
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 15);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue)valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals("ABCDEFGH", vResult._value);
        assertEquals(CharacterMode.ASCII, vResult._characterMode);
    }

    @Test
    public void incompatibleType(
    ) {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue(false, "ABC", CharacterMode.ASCII));
        valueStack.push(new FloatingPointValue(false, 1.0));

        Context context = new Context(new Dictionary(), new String[0],  "TEST");
        LineSpecifier ls = new LineSpecifier(0, 123);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        try {
            op.evaluate(context, valueStack);
        } catch (ExpressionException ex) {
            //  drop through
        }

        List<Diagnostic> diagList = context.getDiagnostics().getDiagnostics();
        assertEquals(1, diagList.size());
        assertEquals(Diagnostic.Level.Value, diagList.get(0).getLevel());
    }
}

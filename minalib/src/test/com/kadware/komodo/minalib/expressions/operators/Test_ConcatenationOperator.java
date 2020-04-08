/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.baselib.FloatingPointComponents;
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

    //TODO needs lots of tests regarding mixing precision and justification values
    @Test
    public void simple_ASCII(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue.Builder().setValue("ABC").setCharacterMode(CharacterMode.ASCII).build());
        valueStack.push(new StringValue.Builder().setValue("DEF").setCharacterMode(CharacterMode.ASCII).build());

        Context context = new Context(new Dictionary(), new String[0]);
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
        valueStack.push(new StringValue.Builder().setValue("ABC").setCharacterMode(CharacterMode.Fieldata).build());
        valueStack.push(new StringValue.Builder().setValue("DEF").setCharacterMode(CharacterMode.Fieldata).build());

        Context context = new Context(new Dictionary(), new String[0]);
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
        valueStack.push(new StringValue.Builder().setValue("ABC").setCharacterMode(CharacterMode.Fieldata).build());
        valueStack.push(new StringValue.Builder().setValue("DEF").setCharacterMode(CharacterMode.ASCII).build());

        Context context = new Context(new Dictionary(), new String[0]);
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
    public void incompatibleType(
    ) {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue.Builder().setValue("ABC").build());
        valueStack.push(new FloatingPointValue.Builder().setValue(new FloatingPointComponents(1.0)).build());

        Context context = new Context(new Dictionary(), new String[0]);
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

/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CharacterMode;
import com.kadware.komodo.kex.kasm.InstrumentedAssembler;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.util.List;
import java.util.Stack;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_ConcatenationOperator {

    //TODO needs lots of tests regarding mixing precision and justification values
    @Test
    public void simple_ASCII(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue.Builder().setValue("ABC").setCharacterMode(CharacterMode.ASCII).build());
        valueStack.push(new StringValue.Builder().setValue("DEF").setCharacterMode(CharacterMode.ASCII).build());

        Assembler asm = new Assembler.Builder().build();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
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

        Assembler asm = new Assembler.Builder().build();
        asm.setCharacterMode(CharacterMode.Fieldata);

        LineSpecifier ls = new LineSpecifier(0, 10);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
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

        Assembler asm = new Assembler.Builder().build();

        LineSpecifier ls = new LineSpecifier(0, 22);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
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

        Assembler asm = new InstrumentedAssembler();
        LineSpecifier ls = new LineSpecifier(0, 123);
        Operator op = new ConcatenationOperator(new Locale(ls, 18));
        try {
            op.evaluate(asm, valueStack);
        } catch (ExpressionException ex) {
            //  drop through
        }

        List<Diagnostic> diagList = asm.getDiagnostics().getDiagnostics();
        assertEquals(1, diagList.size());
        assertEquals(Diagnostic.Level.Value, diagList.get(0).getLevel());
    }
}

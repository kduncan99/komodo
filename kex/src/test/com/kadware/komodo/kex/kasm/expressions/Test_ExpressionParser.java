/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions;

import com.kadware.komodo.baselib.exceptions.CharacteristicOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristicUnderflowException;
import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.items.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_ExpressionParser {

    @Test
    public void parseFloatingLiteral(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("3.14159", locale);

        Expression exp = parser.parse(new Assembler.Builder().build());
        assertEquals(1, exp._items.size());
        ExpressionItem item = exp._items.get(0);
        assertTrue(item instanceof ValueItem);
        Value v = ((ValueItem)item)._value;
        assertTrue(v instanceof FloatingPointValue);
        assertEquals(3.14159, ((FloatingPointValue)v)._value.toDouble(), 0.00001);
    }

    @Test
    public void parseIntegerLiteral(
    ) throws ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("14458", locale);

        Expression exp = parser.parse(new Assembler.Builder().build());
        assertEquals(1, exp._items.size());
        ExpressionItem item = exp._items.get(0);
        assertTrue(item instanceof ValueItem);
        Value v = ((ValueItem)item)._value;
        assertTrue(v instanceof IntegerValue);
        assertEquals(14458L, ((IntegerValue)v)._value.get().longValue());
    }

    @Test
    public void parseLocationToken(
    ) throws ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("$", locale);

        String[] source = {
            "$(1) $RES 16"
        };
        Assembler asm = new Assembler.Builder().setSource(source).build();
        asm.assemble();
        Expression exp = parser.parse(asm);
        assertEquals(1, exp._items.size());

        ExpressionItem item0 = exp._items.get(0);
        assertTrue(item0 instanceof ValueItem);
        ValueItem vItem = (ValueItem) item0;
        assertTrue(vItem._value instanceof IntegerValue);
        IntegerValue iValue = (IntegerValue) vItem._value;
        assertEquals(020, iValue._value.get().intValue());
        assertEquals(1, iValue._references.length);
        assertTrue(iValue._references[0] instanceof UnresolvedReferenceToLocationCounter);
        UnresolvedReferenceToLocationCounter urlc = (UnresolvedReferenceToLocationCounter) iValue._references[0];
        assertEquals(1, urlc._locationCounterIndex);
    }

    @Test
    public void parseNegativeIntegerLiteral(
    ) throws ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("-14458", locale);

        Expression exp = parser.parse(new Assembler.Builder().build());
        assertEquals(2, exp._items.size());

        ExpressionItem item0 = exp._items.get(0);
        assertTrue(item0 instanceof OperatorItem);
//        Operator op = ((OperatorItem) item0)._operator; //  TODO check this value if we can, somehow

        ExpressionItem item1 = exp._items.get(1);
        assertTrue(item1 instanceof ValueItem);
        Value v1 = ((ValueItem)item1)._value;
        assertTrue(v1 instanceof IntegerValue);
        assertEquals(14458L, ((IntegerValue)v1)._value.get().longValue());
    }

    @Test
    public void parseStringLiteral(
    ) throws ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("'Hello'", locale);

        Expression exp = parser.parse(new Assembler.Builder().build());
        assertEquals(1, exp._items.size());
        ExpressionItem item = exp._items.get(0);
        assertTrue(item instanceof ValueItem);
        Value v = ((ValueItem)item)._value;
        assertTrue(v instanceof StringValue);
        assertEquals("Hello", ((StringValue)v)._value);
    }

    @Test
    public void simpleConcatenation(
    ) throws ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("'Hello ':'Stupid ':'Moron'", locale);

        Expression exp = parser.parse(new Assembler.Builder().build());
        assertEquals(5, exp._items.size());
    }

    @Test
    public void simpleMath(
    ) throws ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("1+3", locale);

        Expression exp = parser.parse(new Assembler.Builder().build());
        assertEquals(3, exp._items.size());
    }

    @Test
    public void parseLabel(
    ) {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("$Label", locale);

        String label = parser.parseLabel(new Assembler.Builder().build());
        assertEquals("$Label", label);
    }

    @Test
    public void parseBuiltInFunction(
    ) throws ExpressionException {
        LineSpecifier ls = new LineSpecifier(0, 10);
        Locale locale = new Locale(ls, 10);
        ExpressionParser parser = new ExpressionParser("$sl('Test')", locale);

        assertNotNull(parser.parseReference(new Assembler.Builder().build()));
    }
}

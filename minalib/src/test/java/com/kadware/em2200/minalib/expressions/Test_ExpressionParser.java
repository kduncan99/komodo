/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.builtInFunctions.*;
import com.kadware.em2200.minalib.expressions.operators.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_ExpressionParser {

    @Test
    public void parseIntegerLiteral(
    ) throws ExpressionException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("14458", locale);

        Dictionary system = new SystemDictionary();
        Context context = new Context(new Dictionary(system), "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        assertEquals(1, exp._items.size());
        IExpressionItem item = exp._items.get(0);
        assertTrue(item instanceof ValueItem);
        Value v = ((ValueItem)item)._value;
        assertTrue(v instanceof IntegerValue);
        assertEquals(14458L, ((IntegerValue)v)._value);
    }

    @Test
    public void parseNegativeIntegerLiteral(
    ) throws ExpressionException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("-14458", locale);

        Dictionary system = new SystemDictionary();
        Context context = new Context(new Dictionary(system), "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        assertEquals(2, exp._items.size());

        IExpressionItem item0 = exp._items.get(0);
        assertTrue(item0 instanceof OperatorItem);
        Operator op = ((OperatorItem) item0)._operator;

        IExpressionItem item1 = exp._items.get(1);
        assertTrue(item1 instanceof ValueItem);
        Value v1 = ((ValueItem)item1)._value;
        assertTrue(v1 instanceof IntegerValue);
        assertEquals(14458L, ((IntegerValue)v1)._value);
    }

    @Test
    public void parseStringLiteral(
    ) throws ExpressionException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("'Hello'", locale);

        Dictionary system = new SystemDictionary();
        Context context = new Context(new Dictionary(system), "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        assertEquals(1, exp._items.size());
        IExpressionItem item = exp._items.get(0);
        assertTrue(item instanceof ValueItem);
        Value v = ((ValueItem)item)._value;
        assertTrue(v instanceof StringValue);
        assertEquals("Hello", ((StringValue)v)._value);
    }

    @Test
    public void simpleConcatenation(
    ) throws ExpressionException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("'Hello ':'Stupid ':'Moron'", locale);

        Dictionary system = new SystemDictionary();
        Context context = new Context(new Dictionary(system), "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        assertEquals(5, exp._items.size());
    }

    @Test
    public void simpleMath(
    ) throws ExpressionException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("1+3", locale);

        Dictionary system = new SystemDictionary();
        Context context = new Context(new Dictionary(system), "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        assertEquals(3, exp._items.size());
    }

    @Test
    public void parseLabel(
    ) {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("$Label", locale);

        Diagnostics diagnostics = new Diagnostics();
        String label = parser.parseLabel(diagnostics);
        assertEquals("$Label", label);
    }

    @Test
    public void parseBuiltInFunction(
    ) throws ExpressionException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("$sl('Test')", locale);

        Dictionary system = new SystemDictionary();
        Context context = new Context(new Dictionary(system), "TEST");
        Diagnostics diagnostics = new Diagnostics();
        FunctionItem fi = parser.parseFunction(context, diagnostics);
        assertTrue(fi instanceof BuiltInFunctionItem);
        BuiltInFunctionItem bifItem = (BuiltInFunctionItem)fi;
        BuiltInFunction bif = bifItem._function;
        assertTrue(bif instanceof SLFunction);
    }
}

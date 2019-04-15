/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.builtInFunctions.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kduncan
 */
public class Test_ExpressionParser {

    @Test
    public void parseIntegerLiteral(
    ) throws ExpressionException,
             NotFoundException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("14458", locale);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        long[] expected = { 0, 14458 };

        List<ExpressionItem> items = exp.getItems();
        assertEquals(1, items.size());
        ExpressionItem item = items.get(0);
        assertTrue(item instanceof ValueItem);
        Value v = ((ValueItem)item).getValue();
        assertTrue(v instanceof IntegerValue);
        assertArrayEquals(expected, ((IntegerValue)v).getValue());
    }

    @Test
    public void parseStringLiteral(
    ) throws ExpressionException,
             NotFoundException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("'Hello'", locale);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        List<ExpressionItem> items = exp.getItems();
        assertEquals(1, items.size());
        ExpressionItem item = items.get(0);
        assertTrue(item instanceof ValueItem);
        Value v = ((ValueItem)item).getValue();
        assertTrue(v instanceof StringValue);
        assertEquals("Hello", ((StringValue)v).getValue());
    }

    @Test
    public void simpleConcatenation(
    ) throws ExpressionException,
             NotFoundException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("'Hello ':'Stupid ':'Moron'", locale);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        List<ExpressionItem> items = exp.getItems();
        assertEquals(5, items.size());
    }

    @Test
    public void simpleMath(
    ) throws ExpressionException,
             NotFoundException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("1+3", locale);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Expression exp = parser.parse(context, diagnostics);

        List<ExpressionItem> items = exp.getItems();
        assertEquals(3, items.size());
    }

    @Test
    public void parseLabel(
    ) throws ExpressionException,
             NotFoundException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("$Label", locale);

        Diagnostics diagnostics = new Diagnostics();
        String label = parser.parseLabel(diagnostics);
        assertEquals("$Label", label);
    }

    @Test
    public void parseBuiltInFunction(
    ) throws ExpressionException,
             NotFoundException {
        Locale locale = new Locale(10, 10);
        ExpressionParser parser = new ExpressionParser("$sl('Test')", locale);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        FunctionItem fi = parser.parseFunction(context, diagnostics);
        assertTrue(fi instanceof BuiltInFunctionItem);
        BuiltInFunctionItem bifItem = (BuiltInFunctionItem)fi;
        BuiltInFunction bif = bifItem.getBuiltInFunction();
        assertTrue(bif instanceof SLFunction);
    }
}

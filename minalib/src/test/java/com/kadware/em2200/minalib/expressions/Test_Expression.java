/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.expressions.operators.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Expression {

    @Test
    public void parseSimpleValue(
    ) throws ExpressionException {
        Value val = new IntegerValue(false, 42, null);
        List<IExpressionItem> items = new LinkedList<>();
        LineSpecifier ls = new LineSpecifier(0, 1);
        items.add(new ValueItem(new Locale(ls, 1), val));
        Expression exp = new Expression(items);

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context);

        assertTrue(diagnostics.isEmpty());
        assertEquals(val, result);
    }

    @Test
    public void parseSimpleMath(
    ) throws ExpressionException {
        Value addend1 = new IntegerValue(false, 42, null);
        Value addend2 = new IntegerValue(false, 112, null);
        Value expected = new IntegerValue(false, 154, null);

        List<IExpressionItem> items = new LinkedList<>();
        LineSpecifier ls10 = new LineSpecifier(0, 10);
        items.add(new ValueItem(new Locale(ls10, 1), addend1));
        items.add(new OperatorItem(new AdditionOperator(new Locale(ls10, 10))));
        items.add(new ValueItem(new Locale(ls10, 11), addend2));
        Expression exp = new Expression(items);

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context);

        assertTrue(diagnostics.isEmpty());
        assertEquals(expected, result);
    }

    @Test
    public void parseSimpleMathWithPrecedence(
    ) throws ExpressionException {
        //  expression is 5 + 7 * 12...  it should be evaluated at 5 + (7 * 12) == 89
        Value term1 = new IntegerValue(false, 5, null);
        Value term2 = new IntegerValue(false, 7, null);
        Value term3 = new IntegerValue(false, 12, null);
        Value expected = new IntegerValue(false, 89, null);

        List<IExpressionItem> items = new LinkedList<>();
        LineSpecifier ls = new LineSpecifier(0, 10);
        items.add(new ValueItem(new Locale(ls, 1), term1));
        items.add(new OperatorItem(new AdditionOperator(new Locale(ls, 10))));
        items.add(new ValueItem(new Locale(ls, 30), term2));
        items.add(new OperatorItem(new MultiplicationOperator(new Locale(ls, 12))));
        items.add(new ValueItem(new Locale(ls, 50), term3));
        Expression exp = new Expression(items);

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context);

        assertTrue(diagnostics.isEmpty());
        assertEquals(expected, result);
    }
}

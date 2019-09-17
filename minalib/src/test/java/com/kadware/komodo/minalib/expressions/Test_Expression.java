/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.LineSpecifier;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.dictionary.Dictionary;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.items.IExpressionItem;
import com.kadware.komodo.minalib.expressions.items.OperatorItem;
import com.kadware.komodo.minalib.expressions.items.ValueItem;
import java.util.LinkedList;
import java.util.List;
import com.kadware.komodo.minalib.expressions.operators.AdditionOperator;
import com.kadware.komodo.minalib.expressions.operators.MultiplicationOperator;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Expression {

    @Test
    public void evaluateSimpleValue(
    ) throws ExpressionException {
        Value val = new IntegerValue.Builder().setValue(42).build();
        List<IExpressionItem> items = new LinkedList<>();
        LineSpecifier ls = new LineSpecifier(0, 1);
        items.add(new ValueItem(new Locale(ls, 1), val));
        Expression exp = new Expression(items);

        Context context = new Context(new Dictionary(), new String[0]);
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context);

        assertTrue(diagnostics.isEmpty());
        assertEquals(val, result);
    }

    @Test
    public void evaluateSimpleMath(
    ) throws ExpressionException {
        Value addend1 = new IntegerValue.Builder().setValue(42).build();
        Value addend2 = new IntegerValue.Builder().setValue(112).build();
        Value expected = new IntegerValue.Builder().setValue(154).build();

        List<IExpressionItem> items = new LinkedList<>();
        LineSpecifier ls10 = new LineSpecifier(0, 10);
        items.add(new ValueItem(new Locale(ls10, 1), addend1));
        items.add(new OperatorItem(new AdditionOperator(new Locale(ls10, 10))));
        items.add(new ValueItem(new Locale(ls10, 11), addend2));
        Expression exp = new Expression(items);

        Context context = new Context(new Dictionary(), new String[0]);
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context);

        assertTrue(diagnostics.isEmpty());
        assertEquals(expected, result);
    }

    @Test
    public void evaluateSimpleMathWithPrecedence(
    ) throws ExpressionException {
        //  expression is 5 + 7 * 12...  it should be evaluated at 5 + (7 * 12) == 89
        Value term1 = new IntegerValue.Builder().setValue(5).build();
        Value term2 = new IntegerValue.Builder().setValue(7).build();
        Value term3 = new IntegerValue.Builder().setValue(12).build();
        Value expected = new IntegerValue.Builder().setValue(89).build();

        List<IExpressionItem> items = new LinkedList<>();
        LineSpecifier ls = new LineSpecifier(0, 10);
        items.add(new ValueItem(new Locale(ls, 1), term1));
        items.add(new OperatorItem(new AdditionOperator(new Locale(ls, 10))));
        items.add(new ValueItem(new Locale(ls, 30), term2));
        items.add(new OperatorItem(new MultiplicationOperator(new Locale(ls, 12))));
        items.add(new ValueItem(new Locale(ls, 50), term3));
        Expression exp = new Expression(items);

        Context context = new Context(new Dictionary(), new String[0]);
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context);

        assertTrue(diagnostics.isEmpty());
        assertEquals(expected, result);
    }
}

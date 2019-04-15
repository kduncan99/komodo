/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.baselib.exceptions.*;
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
    ) throws ExpressionException,
             NotFoundException {
        Value val = new IntegerValue.Builder().setValue(42).build();
        List<ExpressionItem> items = new LinkedList<>();
        items.add(new ValueItem(val));
        Expression exp = new Expression(items);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context, diagnostics);

        assertTrue(diagnostics.isEmpty());
        assertEquals(val, result);
    }

    @Test
    public void parseSimpleMath(
    ) throws ExpressionException,
             NotFoundException {
        Value addend1 = new IntegerValue.Builder().setValue(42).build();
        Value addend2 = new IntegerValue.Builder().setValue(112).build();
        Value expected = new IntegerValue.Builder().setValue(154).build();

        List<ExpressionItem> items = new LinkedList<>();
        items.add(new ValueItem(addend1));
        items.add(new OperatorItem(new AdditionOperator(new Locale(10, 10))));
        items.add(new ValueItem(addend2));
        Expression exp = new Expression(items);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context, diagnostics);

        assertTrue(diagnostics.isEmpty());
        assertEquals(expected, result);
    }

    @Test
    public void parseSimpleMathWithPrecedence(
    ) throws ExpressionException,
             NotFoundException {
        //  expression is 5 + 7 * 12...  it should be evaluated at 5 + (7 * 12) == 89
        Value term1 = new IntegerValue.Builder().setValue(5).build();
        Value term2 = new IntegerValue.Builder().setValue(7).build();
        Value term3 = new IntegerValue.Builder().setValue(12).build();
        Value expected = new IntegerValue.Builder().setValue(89).build();

        List<ExpressionItem> items = new LinkedList<>();
        items.add(new ValueItem(term1));
        items.add(new OperatorItem(new AdditionOperator(new Locale(10, 10))));
        items.add(new ValueItem(term2));
        items.add(new OperatorItem(new MultiplicationOperator(new Locale(10, 12))));
        items.add(new ValueItem(term3));
        Expression exp = new Expression(items);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Value result = exp.evaluate(context, diagnostics);

        assertTrue(diagnostics.isEmpty());
        assertEquals(expected, result);
    }

}

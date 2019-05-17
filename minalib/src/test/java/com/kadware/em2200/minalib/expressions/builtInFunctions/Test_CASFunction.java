/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.builtInFunctions;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;
import com.kadware.em2200.minalib.dictionary.*;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the addition operator
 */
public class Test_CASFunction {

    @Test
    public void test(
    ) throws ExpressionException {

        List<IExpressionItem> items = new LinkedList<>();
        IntegerValue iv = new IntegerValue(false, 060061062063L, null);
        LineSpecifier ls01 = new LineSpecifier(0, 1);
        items.add(new ValueItem(new Locale(ls01, 1), iv));

        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(items);

        LineSpecifier ls10 = new LineSpecifier(0, 10);
        BuiltInFunction bif = new CASFunction(new Locale(ls10, 16), expressions);

        Context context = new Context(new Dictionary(), new String[0],  "TEST");
        Value result = bif.evaluate(context);

        StringValue expected = new StringValue(false, "0123", CharacterMode.ASCII);
        assertEquals(expected, result);
    }
}

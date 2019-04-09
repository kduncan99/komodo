/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.builtInFunctions;

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
public class Test_SLFunction {

    @Test
    public void test(
    ) throws ExpressionException,
             NotFoundException {

        List<ExpressionItem> items = new LinkedList<>();
        StringValue sv = new StringValue("Hello Stupid");
        items.add(new ValueItem(sv));

        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(items);

        BuiltInFunction bif = new SLFunction(new Locale(10, 16), expressions);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Value result = bif.evaluate(context, diagnostics);

        IntegerValue expected = new IntegerValue(12);
        assertEquals(expected, result);
    }
}

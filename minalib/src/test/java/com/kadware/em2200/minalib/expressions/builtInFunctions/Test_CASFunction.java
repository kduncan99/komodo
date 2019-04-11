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
public class Test_CASFunction {

    @Test
    public void test(
    ) throws ExpressionException,
             NotFoundException {

        List<ExpressionItem> items = new LinkedList<>();
        IntegerValue iv = new IntegerValue.Builder().setValue(060061062063l).build();
        items.add(new ValueItem(iv));

        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(items);

        BuiltInFunction bif = new CASFunction(new Locale(10, 16), expressions);

        Context context = new Context();
        Diagnostics diagnostics = new Diagnostics();
        Value result = bif.evaluate(context, diagnostics);

        StringValue expected = new StringValue.Builder().setValue("0123").build();
        assertEquals(expected, result);
    }
}

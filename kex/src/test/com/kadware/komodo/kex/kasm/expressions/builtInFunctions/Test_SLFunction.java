/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CharacterMode;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.StringValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.items.IExpressionItem;
import com.kadware.komodo.kex.kasm.expressions.items.ValueItem;
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
    ) throws ExpressionException {

        List<IExpressionItem> items = new LinkedList<>();
        StringValue sv = new StringValue.Builder().setValue("Hello Stupid").setCharacterMode(CharacterMode.ASCII).build();
        LineSpecifier ls01 = new LineSpecifier(0, 1);
        items.add(new ValueItem(new Locale(ls01, 1), sv));

        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(items);

        LineSpecifier ls10 = new LineSpecifier(0, 10);
        BuiltInFunction bif = new SLFunction(new Locale(ls10, 16), expressions);

        Value result = bif.evaluate(new Assembler.Builder().build());
        IntegerValue expected = new IntegerValue.Builder().setValue(12).build();
        assertEquals(expected, result);
    }
}

/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.builtInFunctions;

import java.util.LinkedList;
import java.util.List;

import com.kadware.komodo.minalib.CharacterMode;
import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.LineSpecifier;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.Dictionary;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.StringValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.Expression;
import com.kadware.komodo.minalib.expressions.items.IExpressionItem;
import com.kadware.komodo.minalib.expressions.items.ValueItem;
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
        IntegerValue iv = new IntegerValue.Builder().setValue(0_060_061_062_063L).build();
        LineSpecifier ls01 = new LineSpecifier(0, 1);
        items.add(new ValueItem(new Locale(ls01, 1), iv));

        Expression[] expressions = new Expression[1];
        expressions[0] = new Expression(items);

        LineSpecifier ls10 = new LineSpecifier(0, 10);
        BuiltInFunction bif = new CASFunction(new Locale(ls10, 16), expressions);

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Value result = bif.evaluate(context);

        StringValue expected = new StringValue.Builder().setValue("0123").setCharacterMode(CharacterMode.ASCII).build();
        assertEquals(expected, result);
    }
}

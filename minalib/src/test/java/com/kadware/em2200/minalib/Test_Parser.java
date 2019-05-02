/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.diagnostics.*;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kduncan
 */
public class Test_Parser {

    @Test
    public void parser() {
        String[] source = {
            "          $ASCII",
            "          $BASIC",
            "$(0)      $LIT",
            "START*    .",
            "          LA        A0,(0104, 'HELLO, WORLD!')",
            "          ER        APRINT$",
            "          ER        EXIT$",
            "          + (25)",
            "          + (3+4)*5",
        };

        Diagnostics d = new Diagnostics();
        TextParser parser = new TextParser(source);
        parser.parse(d);
        assertTrue(d.isEmpty());

        ArrayList<TextLine> scset = parser._sourceCodeSet;
        assertEquals(9, scset.size());
        assertEquals(2, scset.get(0)._fields.size());
        assertEquals(2, scset.get(1)._fields.size());
        assertEquals(2, scset.get(2)._fields.size());
        assertEquals(1, scset.get(3)._fields.size());
        assertEquals(3, scset.get(4)._fields.size());
        assertEquals(3, scset.get(5)._fields.size());
        assertEquals(3, scset.get(6)._fields.size());
        assertEquals(2, scset.get(7)._fields.size());
        assertEquals(2, scset.get(8)._fields.size());

        assertNull(scset.get(0).getField(0));

        assertEquals(new Locale(3, 1), scset.get(2).getField(0)._locale);
        assertEquals("$(0)", scset.get(2).getField(0)._text);

        assertEquals(new Locale(5, 11), scset.get(4).getField(1)._locale);
        assertEquals("LA", scset.get(4).getField(1)._text);

        assertEquals(new Locale(7, 21), scset.get(6).getField(2)._locale);
        assertEquals("EXIT$", scset.get(6).getField(2)._text);

        assertEquals("+ (25)", scset.get(7).getField(1)._text);

        assertEquals("+ (3+4)*5", scset.get(8).getField(1)._text);
    }
}

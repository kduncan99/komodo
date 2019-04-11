/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.TextParser;
import com.kadware.em2200.minalib.TextLine;
import com.kadware.em2200.minalib.Locale;
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
            "          ER        EXIT$"
        };
        TextParser parser = new TextParser(source);
        parser.parse();
        assertTrue(parser.getDiagnostics().isEmpty());

        ArrayList<TextLine> scset = parser.getSourceCodeSet();
        assertEquals(7, scset.size());
        assertEquals(2, scset.get(0).getFieldCount());
        assertEquals(2, scset.get(1).getFieldCount());
        assertEquals(2, scset.get(2).getFieldCount());
        assertEquals(1, scset.get(3).getFieldCount());
        assertEquals(3, scset.get(4).getFieldCount());
        assertEquals(3, scset.get(5).getFieldCount());
        assertEquals(3, scset.get(6).getFieldCount());

        assertNull(scset.get(0).getField(0));

        assertEquals(new Locale(3, 1), scset.get(2).getField(0).getLocale());
        assertEquals("$(0)", scset.get(2).getField(0).getText());

        assertEquals(new Locale(5, 11), scset.get(4).getField(1).getLocale());
        assertEquals("LA", scset.get(4).getField(1).getText());

        assertEquals(new Locale(7, 21), scset.get(6).getField(2).getLocale());
        assertEquals("EXIT$", scset.get(6).getField(2).getText());
    }
}

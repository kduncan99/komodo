/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.TextField;
import com.kadware.em2200.minalib.TextSubfield;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.Locale;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author kduncan
 */
public class Test_TextField {

    @Test
    public void getLocaleLimit() {
        TextField fld = new TextField(new Locale(10, 20), "ABCDE");
        Locale expected = new Locale(10, 25);
        assertEquals(expected, fld.getLocaleLimit());
    }

    @Test
    public void parseSubfields_normal() {
        String code = "A0,0112,  *X5,";
        Locale loc = new Locale(10, 21);
        TextField fld = new TextField(loc, code);
        Diagnostics diag = fld.parseSubfields();
        assertEquals(0, diag.getDiagnostics().length);

        int sfCount = fld.getSubfieldCount();
        assertEquals(3, sfCount);

        TextSubfield sf0 = fld.getSubfield(0);
        Locale expLoc0 = new Locale(10, 21);
        assertEquals(expLoc0, sf0.getLocale());
        assertFalse(sf0.isFlagged());
        assertEquals("A0", sf0.getText());

        TextSubfield sf1 = fld.getSubfield(1);
        Locale expLoc1 = new Locale(10, 24);
        assertEquals(expLoc1, sf1.getLocale());
        assertFalse(sf1.isFlagged());
        assertEquals("0112", sf1.getText());

        TextSubfield sf2 = fld.getSubfield(2);
        Locale expLoc2 = new Locale(10, 31);
        assertEquals(expLoc2, sf2.getLocale());
        assertTrue(sf2.isFlagged());
        assertEquals("X5", sf2.getText());
    }

    @Test
    public void skipWhiteSpace() {
        String text = "ABC   DEF";
        assertEquals(6, TextField.skipWhiteSpace(text, 5));
    }

    @Test
    public void skipWhiteSpace_end() {
        String text = "ABC   ";
        assertEquals(6, TextField.skipWhiteSpace(text, 3));
    }

    @Test
    public void skipWhiteSpace_none() {
        String text = "ABC   DEF";
        assertEquals(8, TextField.skipWhiteSpace(text, 8));
    }
}

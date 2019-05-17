/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author kduncan
 */
public class Test_TextField {

    @Test
    public void getLocaleLimit() {
        LineSpecifier ls = new LineSpecifier(0, 10);
        TextField fld = new TextField(new Locale(ls, 20), "ABCDE");
        Locale expected = new Locale(ls, 25);
        assertEquals(expected, fld.getLocaleLimit());
    }

    @Test
    public void parseSubFields_normal1() {
        String code = "LABEL";
        LineSpecifier ls = new LineSpecifier(0, 1);
        Locale loc = new Locale(ls, 1);
        TextField fld = new TextField(loc, code);
        Diagnostics diag = fld.parseSubfields();
        assertTrue(diag.getDiagnostics().isEmpty());

        int sfCount = fld._subfields.size();
        assertEquals(1, sfCount);

        TextSubfield sf0 = fld.getSubfield(0);
        Locale expLoc0 = new Locale(ls, 1);
        assertEquals(expLoc0, sf0._locale);
        assertEquals("LABEL", sf0._text);
    }

    @Test
    public void parseSubFields_normal2() {
        LineSpecifier ls = new LineSpecifier(0, 1);

        String code = "Subfield1,  Subfield2";
        Locale loc = new Locale(ls, 8);
        TextField fld = new TextField(loc, code);
        Diagnostics diag = fld.parseSubfields();
        assertTrue(diag.getDiagnostics().isEmpty());

        int sfCount = fld._subfields.size();
        assertEquals(2, sfCount);

        TextSubfield sf0 = fld.getSubfield(0);
        Locale expLoc0 = new Locale(ls, 8);
        assertEquals(expLoc0, sf0._locale);
        assertEquals("Subfield1", sf0._text);

        TextSubfield sf1 = fld.getSubfield(1);
        Locale expLoc1 = new Locale(ls, 20);
        assertEquals(expLoc1, sf1._locale);
        assertEquals("Subfield2", sf1._text);
    }

    @Test
    public void parseSubfields_normal3() {
        LineSpecifier ls = new LineSpecifier(0, 10);

        String code = "A0,0112,  *X5,";
        Locale loc = new Locale(ls, 21);
        TextField fld = new TextField(loc, code);
        Diagnostics diag = fld.parseSubfields();
        assertTrue(diag.getDiagnostics().isEmpty());

        int sfCount = fld._subfields.size();
        assertEquals(4, sfCount);

        TextSubfield sf0 = fld.getSubfield(0);
        Locale expLoc0 = new Locale(ls, 21);
        assertEquals(expLoc0, sf0._locale);
        assertEquals("A0", sf0._text);

        TextSubfield sf1 = fld.getSubfield(1);
        Locale expLoc1 = new Locale(ls, 24);
        assertEquals(expLoc1, sf1._locale);
        assertEquals("0112", sf1._text);

        TextSubfield sf2 = fld.getSubfield(2);
        Locale expLoc2 = new Locale(ls, 31);
        assertEquals(expLoc2, sf2._locale);
        assertEquals("*X5", sf2._text);

        TextSubfield sf3 = fld.getSubfield(3);
        Locale expLoc3 = new Locale(ls, 35);
        assertEquals(expLoc3, sf3._locale);
        assertEquals("", sf3._text);
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

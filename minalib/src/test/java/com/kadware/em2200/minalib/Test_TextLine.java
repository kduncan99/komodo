/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.Locale;
import com.kadware.em2200.minalib.TextField;
import com.kadware.em2200.minalib.TextLine;
import com.kadware.em2200.minalib.diagnostics.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kduncan
 */
public class Test_TextLine {

    @Test
    public void parseFields_normal(
    ) {
        Diagnostics d = new Diagnostics();
        TextLine tline = new TextLine(10, "LABEL     LA,U      A0,015  . This is a comment.");
        tline.parseFields(d);
        assertTrue(d.isEmpty());

        assertEquals(3, tline._fields.size());

        TextField tf0 = tline.getField(0);
        Locale loc0 = tf0._locale;
        assertEquals(new Locale(10, 1), loc0);
        assertEquals("LABEL", tf0._text);

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1._locale;
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("LA,U", tf1._text);

        TextField tf2 = tline.getField(2);
        Locale loc2 = tf2._locale;
        assertEquals(new Locale(10, 21), loc2);
        assertEquals("A0,015", tf2._text);

        assertNull(tline.getField(3));
    }

    @Test
    public void parseFields_noComment(
    ) {
        Diagnostics d = new Diagnostics();
        TextLine tline = new TextLine(10, "LABEL     LA,U      A0,015");
        tline.parseFields(d);
        assertTrue(d.isEmpty());

        assertEquals(3, tline._fields.size());

        TextField tf0 = tline.getField(0);
        Locale loc0 = tf0._locale;
        assertEquals(new Locale(10, 1), loc0);
        assertEquals("LABEL", tf0._text);

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1._locale;
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("LA,U", tf1._text);

        TextField tf2 = tline.getField(2);
        Locale loc2 = tf2._locale;
        assertEquals(new Locale(10, 21), loc2);
        assertEquals("A0,015", tf2._text);

        assertNull(tline.getField(3));
    }

    @Test
    public void parseFields_noLabel(
    ) {
        Diagnostics d = new Diagnostics();
        TextLine tline = new TextLine(10, "          LA,U      A0,015  . This is a comment.");
        tline.parseFields(d);
        assertTrue(d.isEmpty());

        assertEquals(3, tline._fields.size());
        assertNull(tline.getField(0));

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1._locale;
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("LA,U", tf1._text);

        TextField tf2 = tline.getField(2);
        Locale loc2 = tf2._locale;
        assertEquals(new Locale(10, 21), loc2);
        assertEquals("A0,015", tf2._text);

        assertNull(tline.getField(3));
    }

    @Test
    public void parseFields_parenLevel1(
    ) {
        Diagnostics d = new Diagnostics();
        TextLine tline = new TextLine(10, "          LA,U      A0,('@ASG  '");
        tline.parseFields(d);
        List<Diagnostic> diags = d.getDiagnostics();
        assertEquals(1, diags.size());
        assertEquals(Diagnostic.Level.Error, diags.get(0).getLevel());
    }

    @Test
    public void parseFields_parenLevel2(
    ) {
        Diagnostics d = new Diagnostics();
        TextLine tline = new TextLine(10, "          LA,U      A0,'@ASG  ')");
        tline.parseFields(d);
        List<Diagnostic> diags = d.getDiagnostics();
        assertEquals(1, diags.size());
        assertEquals(Diagnostic.Level.Error, diags.get(0).getLevel());
    }

    @Test
    public void parseFields_unterminatedQuote(
    ) {
        Diagnostics d = new Diagnostics();
        TextLine tline = new TextLine(10, "TAG       $EQU      '@ASG  .");
        tline.parseFields(d);
        List<Diagnostic> diags = d.getDiagnostics();
        assertEquals(1, diags.size());
        assertEquals(Diagnostic.Level.Quote, diags.get(0).getLevel());
    }

    @Test
    public void parseFields_splitSign(
    ) {
        Diagnostics d = new Diagnostics();
        TextLine tline = new TextLine(10, "          + (100, 100)  . This should be ONE field");
        tline.parseFields(d);
        assertTrue(d.isEmpty());

        assertEquals(2, tline._fields.size());

        TextField tf0 = tline.getField(0);
        assertNull(tf0);

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1._locale;
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("+ (100, 100)", tf1._text);
    }

    @Test
    public void removeComments_empty(
    ) {
        assertEquals("", TextLine.removeComments(""));
    }

    @Test
    public void removeComments_blanks(
    ) {
        assertEquals("            ", TextLine.removeComments("            "));
    }

    @Test
    public void removeComments_left(
    ) {
        assertEquals("", TextLine.removeComments(". This is a comment"));
    }

    @Test
    public void removeComments_multi1(
    ) {
        assertEquals("", TextLine.removeComments(" . . "));
    }

    @Test
    public void removeComments_multi2(
    ) {
        assertEquals("", TextLine.removeComments(". . "));
    }

    @Test
    public void removeComments_multi3(
    ) {
        assertEquals("", TextLine.removeComments(". . "));
    }

    @Test
    public void removeComments_normal(
    ) {
        assertEquals("LABEL     LA,U      A5,07   ", TextLine.removeComments("LABEL     LA,U      A5,07    . This is a comment"));
    }

    @Test
    public void removeComments_period(
    ) {
        assertEquals("", TextLine.removeComments("."));
    }

    @Test
    public void removeComments_right(
    ) {
        assertEquals("L L,U A5,0", TextLine.removeComments("L L,U A5,0 ."));
    }
}

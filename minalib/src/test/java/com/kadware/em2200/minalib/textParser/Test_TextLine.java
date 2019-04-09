/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.textParser;

import com.kadware.em2200.minalib.Locale;
import com.kadware.em2200.minalib.diagnostics.*;
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
        TextLine tline = new TextLine(10, "LABEL     LA,U      A0,015  . This is a comment.");
        tline.parseFields();
        assertTrue(tline.getDiagnostics().isEmpty());

        assertEquals(3, tline.getFieldCount());

        TextField tf0 = tline.getField(0);
        Locale loc0 = tf0.getLocale();
        assertEquals(new Locale(10, 1), loc0);
        assertEquals("LABEL", tf0.getText());

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1.getLocale();
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("LA,U", tf1.getText());

        TextField tf2 = tline.getField(2);
        Locale loc2 = tf2.getLocale();
        assertEquals(new Locale(10, 21), loc2);
        assertEquals("A0,015", tf2.getText());

        assertNull(tline.getField(3));
    }

    @Test
    public void parseFields_noComment(
    ) {
        TextLine tline = new TextLine(10, "LABEL     LA,U      A0,015");
        tline.parseFields();
        assertTrue(tline.getDiagnostics().isEmpty());

        assertEquals(3, tline.getFieldCount());

        TextField tf0 = tline.getField(0);
        Locale loc0 = tf0.getLocale();
        assertEquals(new Locale(10, 1), loc0);
        assertEquals("LABEL", tf0.getText());

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1.getLocale();
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("LA,U", tf1.getText());

        TextField tf2 = tline.getField(2);
        Locale loc2 = tf2.getLocale();
        assertEquals(new Locale(10, 21), loc2);
        assertEquals("A0,015", tf2.getText());

        assertNull(tline.getField(3));
    }

    @Test
    public void parseFields_noLabel(
    ) {
        TextLine tline = new TextLine(10, "          LA,U      A0,015  . This is a comment.");
        tline.parseFields();
        assertTrue(tline.getDiagnostics().isEmpty());

        assertEquals(3, tline.getFieldCount());
        assertNull(tline.getField(0));

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1.getLocale();
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("LA,U", tf1.getText());

        TextField tf2 = tline.getField(2);
        Locale loc2 = tf2.getLocale();
        assertEquals(new Locale(10, 21), loc2);
        assertEquals("A0,015", tf2.getText());

        assertNull(tline.getField(3));
    }

    @Test
    public void parseFields_parenLevel1(
    ) {
        TextLine tline = new TextLine(10, "          LA,U      A0,('@ASG  '");
        tline.parseFields();
        Diagnostic[] diags = tline.getDiagnostics().getDiagnostics();
        assertEquals(1, diags.length);
        assertEquals(Diagnostic.Level.Error, diags[0].getLevel());
    }

    @Test
    public void parseFields_parenLevel2(
    ) {
        TextLine tline = new TextLine(10, "          LA,U      A0,'@ASG  ')");
        tline.parseFields();
        Diagnostic[] diags = tline.getDiagnostics().getDiagnostics();
        assertEquals(1, diags.length);
        assertEquals(Diagnostic.Level.Error, diags[0].getLevel());
    }

    @Test
    public void parseFields_unterminatedQuote(
    ) {
        TextLine tline = new TextLine(10, "TAG       $EQU      '@ASG  .");
        tline.parseFields();
        Diagnostic[] diags = tline.getDiagnostics().getDiagnostics();
        assertEquals(1, diags.length);
        assertEquals(Diagnostic.Level.Quote, diags[0].getLevel());
    }

    @Test
    public void parseFields_splitSign(
    ) {
        TextLine tline = new TextLine(10, "          + (100, 100)  . This should be ONE field");
        tline.parseFields();
        assertTrue(tline.getDiagnostics().isEmpty());

        assertEquals(2, tline.getFieldCount());

        TextField tf0 = tline.getField(0);
        assertNull(tf0);

        TextField tf1 = tline.getField(1);
        Locale loc1 = tf1.getLocale();
        assertEquals(new Locale(10, 11), loc1);
        assertEquals("+ (100, 100)", tf1.getText());
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

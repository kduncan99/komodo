/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.textParser;

import com.kadware.em2200.minalib.textParser.TextSubfield;
import com.kadware.em2200.minalib.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kduncan
 */
public class Test_TextSubfield {

    @Test
    public void getLocaleLimit() {
        TextSubfield sf = new TextSubfield(new Locale(10, 20), false, "ABCDE");
        Locale expected = new Locale(10, 25);
        assertEquals(expected, sf.getLocaleLimit());
    }
}

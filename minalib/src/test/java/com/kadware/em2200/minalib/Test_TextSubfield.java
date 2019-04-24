/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.TextSubfield;
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
        TextSubfield sf = new TextSubfield(new Locale(10, 20), "ABCDE");
        Locale expected = new Locale(10, 25);
        assertEquals(expected, sf.getLocaleLimit());
    }
}

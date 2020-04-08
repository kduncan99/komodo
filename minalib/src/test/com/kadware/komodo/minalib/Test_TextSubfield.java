/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kduncan
 */
public class Test_TextSubfield {

    @Test
    public void getLocaleLimit() {
        LineSpecifier ls = new LineSpecifier(0, 10);
        TextSubfield sf = new TextSubfield(new Locale(ls, 20), "ABCDE");
        Locale expected = new Locale(ls, 25);
        assertEquals(expected, sf.getLocaleLimit());
    }
}

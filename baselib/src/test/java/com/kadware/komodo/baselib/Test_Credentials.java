/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Test_Credentials {
    //  need some tests here

    @Test
    public void basic() {
        Credentials cred = new Credentials("sonny", "bono$9923");
        assertTrue(cred.validatePassword("bono$9923"));
        assertFalse(cred.validatePassword("cher$1992"));
    }
}


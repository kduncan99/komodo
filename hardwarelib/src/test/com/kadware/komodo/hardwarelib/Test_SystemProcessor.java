/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Credentials;
import com.kadware.komodo.baselib.Word36;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for Processor class
 */
public class Test_SystemProcessor {

    private static class TestSystemProcessor extends SystemProcessor {
        TestSystemProcessor() {
            super("SP0", 8080, 8443, new Credentials("test", "test"));
        }
    }

    private static final Word36 NONE = new Word36();
    private static final Word36 ALL = new Word36(0_777777_777777L);

    @Test
    public void jktest_composite(
    ) {
        SystemProcessor sp = new TestSystemProcessor();
        sp.setJumpKeys(NONE);

        Word36 jkw = new Word36(0123456_701234L);
        sp.setJumpKeys(jkw);
        Assert.assertEquals(jkw, sp.getJumpKeys());
    }

    @Test
    public void jktest_composite_set_component_get(
    ) {
        SystemProcessor sp = new TestSystemProcessor();
        sp.setJumpKeys(NONE);

        Word36 jkw = new Word36(0123456_700001L);
        sp.setJumpKeys(jkw);
        Assert.assertFalse(sp.getJumpKey(1));
        Assert.assertFalse(sp.getJumpKey(2));
        Assert.assertTrue(sp.getJumpKey(3));
        Assert.assertFalse(sp.getJumpKey(4));
        Assert.assertTrue(sp.getJumpKey(5));
        Assert.assertFalse(sp.getJumpKey(6));

        Assert.assertFalse(sp.getJumpKey(7));
        Assert.assertTrue(sp.getJumpKey(8));
        Assert.assertTrue(sp.getJumpKey(9));
        Assert.assertTrue(sp.getJumpKey(10));
        Assert.assertFalse(sp.getJumpKey(11));
        Assert.assertFalse(sp.getJumpKey(12));

        Assert.assertTrue(sp.getJumpKey(13));
        Assert.assertFalse(sp.getJumpKey(14));
        Assert.assertTrue(sp.getJumpKey(15));
        Assert.assertTrue(sp.getJumpKey(16));
        Assert.assertTrue(sp.getJumpKey(17));
        Assert.assertFalse(sp.getJumpKey(18));

        Assert.assertTrue(sp.getJumpKey(19));
        Assert.assertTrue(sp.getJumpKey(20));
        Assert.assertTrue(sp.getJumpKey(21));
        Assert.assertFalse(sp.getJumpKey(22));
        Assert.assertFalse(sp.getJumpKey(23));
        Assert.assertFalse(sp.getJumpKey(24));

        Assert.assertFalse(sp.getJumpKey(25));
        Assert.assertFalse(sp.getJumpKey(26));
        Assert.assertFalse(sp.getJumpKey(27));
        Assert.assertFalse(sp.getJumpKey(28));
        Assert.assertFalse(sp.getJumpKey(29));
        Assert.assertFalse(sp.getJumpKey(30));

        Assert.assertFalse(sp.getJumpKey(31));
        Assert.assertFalse(sp.getJumpKey(32));
        Assert.assertFalse(sp.getJumpKey(33));
        Assert.assertFalse(sp.getJumpKey(34));
        Assert.assertFalse(sp.getJumpKey(35));
        Assert.assertTrue(sp.getJumpKey(36));
    }

    @Test
    public void jktest_component(
    ) {
        SystemProcessor sp = new TestSystemProcessor();
        sp.setJumpKeys(NONE);

        sp.setJumpKey(3, true);
        sp.setJumpKey(4, true);
        sp.setJumpKey(13, true);
        for (int jkid = 1; jkid < 37; ++jkid) {
            Assert.assertEquals(jkid == 3 || jkid == 4 || jkid == 13, sp.getJumpKey(jkid));
        }
    }

    @Test
    public void jktest_component_set_composite_get(
    ) {
        SystemProcessor sp = new TestSystemProcessor();
        sp.setJumpKeys(NONE);

        sp.setJumpKey(3, true);
        sp.setJumpKey(4, true);
        sp.setJumpKey(13, true);
        Word36 jktest = new Word36(0_140040_000000L);
        Assert.assertEquals(jktest, sp.getJumpKeys());
    }
}

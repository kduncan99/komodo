/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestFileAllocation {

    @Test
    public void testBasic() {
        var region = new TrackRegion(500, 10);
        var hwTid = new HardwareTrackId(01001, 1000);
        var fa = new FileAllocation(region, hwTid);
        assertEquals(region, fa.getFileRegion());
        assertEquals(hwTid, fa.getHardwareTrackId());
    }
}

/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;

public class Test_Configurator {

    private static final String FILE_NAME = "../resources/configuration.json";

    @Test
    public void establishConfig(
    ) throws IOException {
        Configurator config = Configurator.read(FILE_NAME);
        config.write(FILE_NAME);
    }

    @Test
    public void getConfig(
    ) throws IOException {
        Configurator config = Configurator.read(FILE_NAME);
        Assert.assertEquals(1, config._version._major);
        Assert.assertEquals(0, config._version._minor);
        Assert.assertEquals(0, config._version._patch);
        Assert.assertEquals(0, config._version._build);
        Assert.assertEquals("admin", config._adminCredentials._userName);
    }
}

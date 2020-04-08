/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Credentials;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;

public class Test_Configurator {

    private static final String FILE_NAME = "../resources/configuration.json";

    @Test
    public void establishConfig(
    ) throws IOException {
        Configurator newConfig = new Configurator("Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved",
                                                  new Configurator.Version(1, 0, 0, 0),
                                                  "EM2200",
                                                  new Credentials("admin", "admin"));
        newConfig.write(FILE_NAME);
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

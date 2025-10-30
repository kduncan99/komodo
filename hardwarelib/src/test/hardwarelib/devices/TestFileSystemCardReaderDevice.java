/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.devices.FileSystemCardReaderDevice;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestFileSystemCardReaderDevice {

    private static final String DEVICE_NAME = "CR0";
    private static final String DEVICE_FILE = "foo";//TODO fix this

    @Test
    public void testReadyIsSetIfReadyIsNotSet() {
        var device = new FileSystemCardReaderDevice(DEVICE_NAME, DEVICE_FILE);
        device.probe();
        // TODO how to test?
    }

    @Test
    public void testReadyIsSetIfReaderIsNull() {
        var device = new FileSystemCardReaderDevice(DEVICE_NAME, DEVICE_FILE);
        device.probe();
        // TODO how to test?
    }
}

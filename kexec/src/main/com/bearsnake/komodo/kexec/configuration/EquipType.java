/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

import com.bearsnake.komodo.hardwarelib.channels.DiskChannel;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemDiskDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemPrinterDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemCardReaderDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemCardPunchDevice;
import com.bearsnake.komodo.hardwarelib.devices.FileSystemTapeDevice;
import com.bearsnake.komodo.hardwarelib.channels.SymbiontChannel;
import com.bearsnake.komodo.hardwarelib.channels.TapeChannel;

import java.util.Arrays;

public enum EquipType {

    CHANNEL_MODULE_DISK("CM-DISK", DiskChannel.class),
    CHANNEL_MODULE_SYMBIONT("CM-SYM", SymbiontChannel.class),
    CHANNEL_MODULE_TAPE("CM-TAPE", TapeChannel.class),
    FILE_SYSTEM_CARD_PUNCH("FS-PUNCH", FileSystemCardPunchDevice.class),
    FILE_SYSTEM_CARD_READER("FS-READER", FileSystemCardReaderDevice.class),
    FILE_SYSTEM_PRINTER("FS-PRINTER", FileSystemPrinterDevice.class),
    FILE_SYSTEM_DISK("FS-DISK", FileSystemDiskDevice.class),
    FILE_SYSTEM_TAPE("FS-TAPE", FileSystemTapeDevice.class);
    // TODO HOST_PRINTER (something configured on the host computer)
    // TODO NETWORK_CARD_READER
    // TODO NETWORK_PRINTER (traditional lpr interface)

    private final Class<?> _class;
    private final String _token;

    EquipType(
        final String token,
        final Class<?> clazz
    ) {
        _token = token;
        _class = clazz;
    }

    public Class<?> getClazz() {
        return _class;
    }

    public String getToken() {
        return _token;
    }

    public static EquipType getFromToken(final String token) {
        return Arrays.stream(EquipType.values())
                     .filter(et -> et._token.equalsIgnoreCase(token))
                     .findFirst()
                     .orElse(null);
    }
}

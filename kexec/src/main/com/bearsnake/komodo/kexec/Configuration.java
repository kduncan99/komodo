/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.hardwarelib.DiskChannel;
import com.bearsnake.komodo.hardwarelib.FileSystemDiskDevice;
import com.bearsnake.komodo.hardwarelib.FileSystemImageReaderDevice;
import com.bearsnake.komodo.hardwarelib.FileSystemImageWriterDevice;
import com.bearsnake.komodo.hardwarelib.FileSystemTapeDevice;
import com.bearsnake.komodo.hardwarelib.SymbiontChannel;
import com.bearsnake.komodo.hardwarelib.TapeChannel;
import com.bearsnake.komodo.kexec.exceptions.KExecException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Configuration {

    // discrete configuration values
    // The following cannot be changed - if we make them changeable, this has implications for the mnemonic table
    private final String _massStorageDefaultMnemonic    = "F";
    private final String _tapeDefaultMnemonic           = "T";
    private final String WordAddressableDefaultMnemonic = "D";

    // The following can be changed when the config file is loaded (once per execution)
    private String _accountAssignMnemonic          = "F";              // assign mnemonic for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    private long _accountInitialReserve            = 10;               // initial reserve for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    private String _dlocAssignMnemonic             = "F";              // assign mnemonic for SYS$*DLOC$ file
    private boolean _filesPrivateByAccount         = true;             // if false, files are private by project-id
    private String _genFAssignMnemonic             = "F";              // assign mnemonic for SYS$*GENF$ file
    private long _genFInitialReserve               = 128;              // initial reserve for SYS$*GENF$ file
    private String _libAssignMnemonic              = "F";              // assign mnemonic for SYS$*LIB$ file
    private long _libInitialReserve                = 128;              // initial reserve for SYS$*LIB$ file
    private long _libMaximumSize                   = 9999;             // max granules for SYS$*LIB$ file
    private boolean _logConsoleMessages            = true;
    private boolean _logIOs                        = true;
    private String _masterAccountId                = "SYSTEM";         // could be empty, in which case operator is prompted when ACCOUNT$R1 is created
    private long _maxCards                         = 256;
    private long _maxGranules                      = 256;              // max granules if not specified on @ASG or @CAT
    private long _maxPages                         = 256;
    private long _maxTime                          = 600;              // max seconds for run time
    public String OverheadAccountId                = "INSTALLATION";   // account ID for overhead runs such as SYS and ROLOUT/ROLBACK
    public String OverheadUserId                   = "INSTALLATION";   // User ID for overhead runs
    public String PrivilegedAccountId              = "123456";         // account ID which can override reading tape label blocks
    private boolean _releaseUnusedReserve          = true;
    private boolean _releaseUnusedRemovableReserve = false;
    private boolean _residueClear                  = false;            // zero out tracks when allocated
    private String _runAssignMnemonic              = "F";              // assign mnemonic for SYS$*RUN$ file
    private long _runInitialReserve                = 10;               // initial reserve for SYS$*RUN$ file
    private long _runMaximumSize                   = 256;              // max granules for SYS$*RUN$ file
    private String _sacrdAssignMnemonic            = "F";              // assign mnemonic for SYS$*SEC@ACR$ file
    private long _sacrdInitialReserve              = 10;               // initial reserve for SYS$*SEC@ACR$ file
    public double StandardRoloutAvailabilityGoal   = 1.50;
    public double StandardRoloutStartThreshold     = 3.00;
    public String SecurityOfficerUserId            = "";               // could be empty, in which case operator is prompted at boot time
    private int _symbiontBufferSize                = 224;              // Buffer size used for standard and alternate read/write buffers
    private String _systemTapeEquipment            = "T";              // assign mnemonic for exec tape requests
    public boolean TapeAccessRestrictedByAccount   = false;
    public boolean TerminateMaxCards               = false;
    public boolean TerminateMaxPages               = false;
    public boolean TerminateMaxTime                = false;
    public String TIPQualifier                     = "TIP$";
    public String TIPReadKey                       = "++++++";
    public String TIPWriteKey                      = "++++++";
    public String TPFAssignMnemonic                = "F";
    public long TPFMaxSize                         = 128;
    private String _userAssignMnemonic             = "F";              // assign mnemonic for SYS$*SEC@USERID$ file
    private long _userInitialReserve               = 10;               // initial reserve for SYS$*SEC@USERID$ file

    // Currently there are no configuration items which can be changed dynamically

    // -----------------------------------------------------------------------------------------------------------------
    public String getAccountAssignMnemonic() { return _accountAssignMnemonic; }
    public long getAccountInitialReserve() { return _accountInitialReserve; }
    public String getDLOCAssignMnemonic() { return _dlocAssignMnemonic; }
    public boolean getFilesPrivateByAccount() { return _filesPrivateByAccount; }
    public String getGENFAssignMnemonic() { return _genFAssignMnemonic; }
    public long getGENFInitialReserve() { return _genFInitialReserve; }
    public String getLibAssignMnemonic() { return _libAssignMnemonic; }
    public long getLibInitialReserve() { return _libInitialReserve; }
    public long getLibMaximumSize() { return _libMaximumSize; }
    public boolean getLogConsoleMessages() { return _logConsoleMessages; }
    public boolean getLogIos() { return _logIOs; }
    public String getMassStorageDefaultMnemonic() { return _massStorageDefaultMnemonic; }
    public String getMasterAccountId() { return _masterAccountId; }
    public long getMaxCards() { return _maxCards; }
    public long getMaxGranules() { return _maxGranules; }
    public long getMaxPages() { return _maxPages; }
    public long getMaxTime() { return _maxTime; }
    public boolean getReleaseUnusedReserve() { return _releaseUnusedReserve; }
    public boolean getReleaseUnusedRemovableReserve() { return _releaseUnusedRemovableReserve; }
    public boolean getResidueClear() { return _residueClear; }
    public String getRunAssignMnemonic() { return _runAssignMnemonic; }
    public long getRunInitialReserve() { return _runInitialReserve; }
    public long getRunMaximumSize() { return _runMaximumSize; }
    public String getSACRDAssignMnemonic() { return _sacrdAssignMnemonic; }
    public long getSACRDInitialReserve() { return _sacrdInitialReserve; }
    public int getSymbiontBufferSize() { return _symbiontBufferSize; }
    public String getSystemTapeEquipment() { return _systemTapeEquipment; }
    public String getUserAssignMnemonic() { return _userAssignMnemonic; }
    public long getUserInitialReserve() { return _userInitialReserve; }

    public void updateFromFile(final String filename) throws KExecException {
        // TODO
    }

    public enum EquipmentType {
        CHANNEL_MODULE_DISK("CM-DISK", DiskChannel.class),
        CHANNEL_MODULE_SYMBIONT("CM_SYM", SymbiontChannel.class),
        CHANNEL_MODULE_TAPE("CM-TAPE", TapeChannel.class),
        FILE_SYSTEM_CARD_PUNCH("FS-PUNCH", FileSystemImageWriterDevice.class),
        FILE_SYSTEM_CARD_READER("FS-READER", FileSystemImageReaderDevice.class),
        // TODO FILE_SYSTEM_PRINTER
        FILE_SYSTEM_DISK("FS-DISK", FileSystemDiskDevice.class),
        FILE_SYSTEM_TAPE("FS-TAPE", FileSystemTapeDevice.class);
        // TODO NETWORK_CARD_READER
        // TODO SYSTEM_PRINTER (something configured on the host computer - can we even do this in Java?)

        private final Class<?> _class;
        private final String _token;

        EquipmentType(
            final String token,
            final Class<?> clazz
        ) {
            _token = token;
            _class = clazz;
        }

        public Class<?> getClazz() { return _class; }
        public String getToken() { return _token; }

        public static EquipmentType getFromToken(final String token) {
            for (var et : EquipmentType.values()) {
                if (et._token.equalsIgnoreCase(token)) {
                    return et;
                }
            }
            return null;
        }
    }

    /**
     * Mnemonic stuff - only applies to disk and tape files
     */
    public enum MnemonicType {
        SECTOR_ADDRESSABLE_DISK,
        WORD_ADDRESSABLE_DISK,
        TAPE,
    }

    public static class MnemonicInfo {
        public final String _mnemonic;
        public final MnemonicType _type;
        public final List<EquipmentType> _equipmentTypes = new LinkedList<>();

        public MnemonicInfo(
            final String mnemonic,
            final MnemonicType type,
            final Collection<EquipmentType> equipmentTypes
        ) {
            _mnemonic = mnemonic;
            _type = type;
            _equipmentTypes.addAll(equipmentTypes);
        }
    }

    private static void putMnemonicInfo(
        final String mnemonic,
        final MnemonicType type,
        final EquipmentType[] equipmentTypes
    ) {
        var mi = new MnemonicInfo(mnemonic, type, List.of(equipmentTypes));
        MNEMONIC_TABLE.put(mi._mnemonic, mi);
    }

    private static final Map<String, MnemonicInfo> MNEMONIC_TABLE = new HashMap<>();
    static {
        // TODO point all the conventional mnemonics to appropriate equip types (i.e., F70, F70M, F81, etc)
        putMnemonicInfo("F", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipmentType[]{ EquipmentType.FILE_SYSTEM_DISK });
        putMnemonicInfo("FFS", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipmentType[]{ EquipmentType.FILE_SYSTEM_DISK });
        putMnemonicInfo("D", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipmentType[]{ EquipmentType.FILE_SYSTEM_DISK });
        putMnemonicInfo("DFS", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipmentType[]{ EquipmentType.FILE_SYSTEM_DISK });
        putMnemonicInfo("T", MnemonicType.TAPE, new EquipmentType[]{ EquipmentType.FILE_SYSTEM_TAPE });
        putMnemonicInfo("TFS", MnemonicType.TAPE, new EquipmentType[]{ EquipmentType.FILE_SYSTEM_TAPE });
    }

    public MnemonicType getMnemonicType(final String mnemonic) {
        var mi = MNEMONIC_TABLE.get(mnemonic);
        return mi == null ? null : mi._type;
    }
}

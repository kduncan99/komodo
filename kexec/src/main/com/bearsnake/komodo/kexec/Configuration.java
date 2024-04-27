/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.hardwarelib.Node;
import com.bearsnake.komodo.kexec.exceptions.KExecException;

import java.util.HashMap;

public class Configuration {

    public long AccountInitialReserve            = 10;               // initial reserve for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    public String AccountAssignMnemonic          = "F";              // assign mnemonic for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    public String DLOCAssignMnemonic             = "F";              // assign mnemonic for SYS$*DLOC$ file
    private int _execThreadPoolSize              = 25;               // number of threads in Exec thread pool
    public boolean FilesPrivateByAccount         = true;             // if false, files are private by project-id
    public long GenFInitialReserve               = 128;              // initial reserve for SYS$*GENF$ file
    public String GenFAssignMnemonic             = "F";              // assign mnemonic for SYS$*GENF$ file
    public long LibInitialReserve                = 128;              // initial reserve for SYS$*LIB$ file
    public String LibAssignMnemonic              = "F";              // assign mnemonic for SYS$*LIB$ file
    public long LibMaximumSize                   = 9999;             // max granules for SYS$*LIB$ file
    private boolean _logConsoleMessages          = true;
    private boolean _logIOs                      = true;
    private String _masterAccountId              = "SYSTEM";         // could be empty, in which case operator is prompted when ACCOUNT$R1 is created
    private String _massStorageDefaultMnemonic   = "F";              // Usually 'F'
    private long _maxCards                       = 256;
    public long MaxGranules                      = 256;              // max granules if not specified on @ASG or @CAT
    private long _maxPages                       = 256;
    public String OverheadAccountId              = "INSTALLATION";   // account ID for overhead runs such as SYS and ROLOUT/ROLBACK
    public String OverheadUserId                 = "INSTALLATION";   // User ID for overhead runs
    public String PrivilegedAccountId            = "123456";         // account ID which can override reading tape label blocks
    public boolean ReleaseUnusedReserve          = true;
    public boolean ReleaseUnusedRemovableReserve = false;
    public boolean ResidueClear                  = false;            // zero out tracks when allocated
    public long RunInitialReserve                = 10;               // initial reserve for SYS$*RUN$ file
    public String RunAssignMnemonic              = "F";              // assign mnemonic for SYS$*RUN$ file
    public long RunMaximumSize                   = 256;              // max granules for SYS$*RUN$ file
    public long SacrdInitialReserve              = 10;               // initial reserve for SYS$*SEC@ACR$ file
    public String SacrdAssignMnemonic            = "F";              // assign mnemonic for SYS$*SEC@ACR$ file
    public double StandardRoloutAvailabilityGoal = 1.50;
    public double StandardRoloutStartThreshold   = 3.00;
    public String SecurityOfficerUserId          = "";               // could be empty, in which case operator is prompted at boot time
    public long SymbiontBufferSize               = 224;              // Buffer size used for standard and alternate read/write buffers
    public String SystemTapeEquipment            = "T";              // assign mnemonic for exec tape requests
    public boolean TapeAccessRestrictedByAccount = false;
    public String TapeDefaultMnemonic            = "T";
    public boolean TerminateMaxCards             = false;
    public boolean TerminateMaxPages             = false;
    public boolean TerminateMaxTime              = false;
    public String TIPQualifier                   = "TIP$";
    public String TIPReadKey                     = "++++++";
    public String TIPWriteKey                    = "++++++";
    public String TPFAssignMnemonic              = "F";
    public long TPFMaxSize                       = 128;
    public long UserInitialReserve               = 10;               // initial reserve for SYS$*SEC@USERID$ file
    public String UserAssignMnemonic             = "F";              // assign mnemonic for SYS$*SEC@USERID$ file
    public String WordAddressableDefaultMnemonic = "D";

    public int getExecThreadPoolSize() { return _execThreadPoolSize; }
    public boolean getLogConsoleMessages() { return _logConsoleMessages; }
    public boolean getLogIos() { return _logIOs; }
    public String getMassStorageDefaultMnemonic() { return _massStorageDefaultMnemonic; }
    public String getMasterAccountId() { return _masterAccountId; }
    public long getMaxCards() { return _maxCards; }
    public long getMaxPages() { return _maxPages; }

    public void updateFromFile(final String filename) throws KExecException {
        // TODO
    }
}

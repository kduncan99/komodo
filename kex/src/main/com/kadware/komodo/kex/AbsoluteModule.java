/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a relocatable module in the external (kex) environment.
 * Represents an absolute module in the external (kex) environment.
 * Contains all of the code specifications necessary for describing the content
 * of an executable to be loaded into storage and executed.
 * This may be for execution under the control of an operating system (such as KEXEC),
 * or it may be an operating system in and of itself (again, such as KEXEC).
 */
public class AbsoluteModule {

    //  Inner classes ----------------------------------------------------------

    public static class BankLoadInfo {

        public int _bandDescriptorIndex;
        public boolean _dynamicFlag;                        //  bank option D
        private boolean _dataBankFlag;                      //  DBANK directive
        private boolean _writeProtectFlag;                  //  bank option R
        private boolean _guaranteedEntryPointFlag;          //  bank option G
        private boolean _sharedBankFlag;                    //  bank option S
        private boolean _testSetQueuingFlag;                //  bank option Q
        private boolean _commonDBankContingenciesFlag;      //  bank option H
        private boolean _allowContingencyProcessingFlag;    //  bank option A
        private boolean _start2kBoundaryFlag;               //  bank option T
        private int _storagePreference;                     //  default value 4, 5 -> extended mode
        private int _blockCount;                            //  Useful mainly for V option banks (void)
        private int _lowerLimit;
        private long[] _code;
    }

    //  Data -------------------------------------------------------------------

    private boolean _blockSize64Flag;       //  if true, blocks are 64 words = else 512 words
    private int _mainIBankBDI = 0;
    private int _mainDBankBDI = 0;
    private int _utilIBankBDI = 0;
    private int _utilDBankBDI = 0;
    private int _programStartAddress = 0;
    private long _processorOptions = 0;

    //  Bank load table keyed by BDI
    private final Map<Integer, BankLoadInfo> _bankLoadTable = new TreeMap<>();


    //  Constructor ------------------------------------------------------------

    //  Methods ----------------------------------------------------------------

}

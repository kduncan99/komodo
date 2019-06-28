/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.functions.activityControl.*;
import com.kadware.em2200.hardwarelib.functions.addressSpaceManagement.*;
import com.kadware.em2200.hardwarelib.functions.fixedPointBinary.*;
import com.kadware.em2200.hardwarelib.functions.generalLoad.*;
import com.kadware.em2200.hardwarelib.functions.generalStore.*;
import com.kadware.em2200.hardwarelib.functions.conditionalJump.*;
import com.kadware.em2200.hardwarelib.functions.interruptControl.*;
import com.kadware.em2200.hardwarelib.functions.logical.*;
import com.kadware.em2200.hardwarelib.functions.procedureControl.*;
import com.kadware.em2200.hardwarelib.functions.shift.*;
import com.kadware.em2200.hardwarelib.functions.special.*;
import com.kadware.em2200.hardwarelib.functions.stackManipulation.*;
import com.kadware.em2200.hardwarelib.functions.systemControl.*;
import com.kadware.em2200.hardwarelib.functions.test.*;
import com.kadware.em2200.hardwarelib.functions.unconditionalJump.*;


/**
 * Static class which contains all the lookup information for finding a handler for
 * a particular instruction word.
 */
public abstract class FunctionTable {

    //  Note: The following instructions are not intended to be implemented:
    //      BAO
    //      CMPXCHG

    /**
     * Basic Mode function handlers for f-field 005, indexed by a-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION005_HANDLERS = {
        new SZFunctionHandler(),    //  000
        new SNZFunctionHandler(),   //  001
        new SP1FunctionHandler(),   //  002
        new SN1FunctionHandler(),   //  003
        new SFSFunctionHandler(),   //  004
        new SFZFunctionHandler(),   //  005
        new SASFunctionHandler(),   //  006
        new SAZFunctionHandler(),   //  007
        new INCFunctionHandler(),   //  010
        new DECFunctionHandler(),   //  011
        new INC2FunctionHandler(),  //  012
        new DEC2FunctionHandler(),  //  013
        new ENZFunctionHandler(),   //  01r
        new ADD1FunctionHandler(),  //  015
        new SUB1FunctionHandler(),  //  016
        null,           //  017
    };

    /**
     * Basic Mode function handlers for f-field 007, indexed by j-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION007_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        new LAQWFunctionHandler(),  //  004
        new SAQWFunctionHandler(),  //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        new LDJFunctionHandler(),   //  012
        new LIJFunctionHandler(),   //  013
        new LPDFunctionHandler(),   //  014
        new SPDFunctionHandler(),   //  015
        null,           //  016
        new LBJFunctionHandler(),   //  017
    };

    /**
     * Basic Mode function handlers for f-field 071, indexed by j-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION071_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        new DAFunctionHandler(),    //  010
        new DANFunctionHandler(),   //  011
        new DSFunctionHandler(),    //  012
        new DLFunctionHandler(),    //  013
        new DLNFunctionHandler(),   //  014
        new DLMFunctionHandler(),   //  015
        new DJZFunctionHandler(),   //  016
        new DTEFunctionHandler(),   //  017
    };

    /**
     * Basic Mode function handlers for f-field 072, indexed by j-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION072_HANDLERS = {
        null,           //  000
        new SLJFunctionHandler(),   //  001
        new JPSFunctionHandler(),   //  002
        new JNSFunctionHandler(),   //  003
        new AHFunctionHandler(),    //  004
        new ANHFunctionHandler(),   //  005
        new ATFunctionHandler(),    //  006
        new ANTFunctionHandler(),   //  007
        null,           //  010
        new ERFunctionHandler(),    //  011
        null,           //  012
        null,           //  013
        null,           //  014
        new TRAFunctionHandler(),   //  015
        new SRSFunctionHandler(),   //  016
        new LRSFunctionHandler(),   //  017
    };

    /**
     * Basic Mode function handlers for f-field 073, j-field 015, indexed by a-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION073_015_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        new ACELFunctionHandler(),  //  003
        new DCELFunctionHandler(),  //  004
        new SPIDFunctionHandler(),  //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        new LDFunctionHandler(),    //  014
        new SDFunctionHandler(),    //  015
        new URFunctionHandler(),    //  016
        new SGNLFunctionHandler(),  //  017
    };

    /**
     * Basic Mode function handlers for f-field 073, j-field 017, indexed by a-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION073_017_HANDLERS = {
        new TSFunctionHandler(),    //  000
        new TSSFunctionHandler(),   //  001
        new TCSFunctionHandler(),   //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Basic Mode function handlers for f-field 073, indexed by j-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION073_HANDLERS = {
        new SSCFunctionHandler(),   //  000
        new DSCFunctionHandler(),   //  001
        new SSLFunctionHandler(),   //  002
        new DSLFunctionHandler(),   //  003
        new SSAFunctionHandler(),   //  004
        new DSAFunctionHandler(),   //  005
        new LSCFunctionHandler(),   //  006
        new DLSCFunctionHandler(),  //  007
        new LSSCFunctionHandler(),  //  010
        new LDSCFunctionHandler(),  //  011
        new LSSLFunctionHandler(),  //  012
        new LDSLFunctionHandler(),  //  013
        null,           //  014
        new SubSubFunctionHandler(BASIC_MODE_FUNCTION073_015_HANDLERS), //  015
        null,           //  016
        new SubSubFunctionHandler(BASIC_MODE_FUNCTION073_017_HANDLERS), //  017
    };

    /**
     * Basic Mode function handlers for f-field 074 j-field 004, indexed by a-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION074_004_HANDLERS = {
        new JFunctionHandler(),     //  000
        new JKFunctionHandler(),    //  001
        new JKFunctionHandler(),    //  002
        new JKFunctionHandler(),    //  003
        new JKFunctionHandler(),    //  004
        new JKFunctionHandler(),    //  005
        new JKFunctionHandler(),    //  006
        new JKFunctionHandler(),    //  007
        new JKFunctionHandler(),    //  010
        new JKFunctionHandler(),    //  011
        new JKFunctionHandler(),    //  012
        new JKFunctionHandler(),    //  013
        new JKFunctionHandler(),    //  014
        new JKFunctionHandler(),    //  015
        new JKFunctionHandler(),    //  016
        new JKFunctionHandler(),    //  017
    };

    /**
     * Basic Mode function handlers for f-field 074 j-field 014, indexed by a-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION074_014_HANDLERS = {
        new JOFunctionHandler(),    //  000
        null,           //  001
        null,           //  002
        new JDFFunctionHandler(),   //  003
        null,           //  004
        null,           //  005
        null,           //  006
        new PAIJFunctionHandler(),  //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Basic Mode function handlers for f-field 074 j-field 015, indexed by a-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION074_015_HANDLERS = {
        new JNOFunctionHandler(),   //  000
        null,           //  001
        null,           //  002
        new JNDFFunctionHandler(),  //  003
        null,           //  004
        new HLTJFunctionHandler(),  //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Basic Mode function handlers for f-field 074, indexed by j-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION074_HANDLERS = {
        new JZFunctionHandler(),    //  000
        new JNZFunctionHandler(),   //  001
        new JPFunctionHandler(),    //  002
        new JNFunctionHandler(),    //  003
        new SubSubFunctionHandler(BASIC_MODE_FUNCTION074_004_HANDLERS), //  004
        new HJFunctionHandler(),    //  005
        new NOPFunctionHandler(),   //  006
        new AAIJFunctionHandler(),  //  007
        new JNBFunctionHandler(),   //  010
        new JBFunctionHandler(),    //  011
        new JMGIFunctionHandler(),  //  012
        new LMJFunctionHandler(),   //  013
        new SubSubFunctionHandler(BASIC_MODE_FUNCTION074_014_HANDLERS), //  014
        new SubSubFunctionHandler(BASIC_MODE_FUNCTION074_015_HANDLERS), //  015
        new JCFunctionHandler(),    //  016
        new JNCFunctionHandler(),   //  017
    };

    /**
     * Basic Mode function handlers for f-field 075, indexed by j-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION075_HANDLERS = {
        new LBUFunctionHandler(),   //  000
        null,           //  001
        new SBUFunctionHandler(),   //  002
        new LBEFunctionHandler(),   //  003
        new SBEDFunctionHandler(),  //  004
        new LBEDFunctionHandler(),  //  005
        new SBUDFunctionHandler(),  //  006
        new LBUDFunctionHandler(),  //  007
        new TVAFunctionHandler(),   //  010
        null,           //  011
        null,           //  012
        new LXLMFunctionHandler(),  //  013
        new LBNFunctionHandler(),   //  014
        new CRFunctionHandler(),    //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Basic Mode function handlers for f-field 077 j-field 017, indexed by a-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION077_017_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        new HALTFunctionHandler(),
    };

    /**
     * Basic Mode function handlers for f-field 077, indexed by j-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTION077_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        new SubSubFunctionHandler(BASIC_MODE_FUNCTION077_017_HANDLERS),
    };

    /**
     * Basic mode function handler vector indexed by the instruction f-field
     */
    private static final FunctionHandler[] BASIC_MODE_FUNCTIONS = {
        null,           //  000
        new SAFunctionHandler(),    //  001
        new SNAFunctionHandler(),   //  002
        new SMAFunctionHandler(),   //  003
        new SRFunctionHandler(),    //  004
        new SubSubFunctionHandler(BASIC_MODE_FUNCTION005_HANDLERS), //  005
        new SXFunctionHandler(),    //  006
        new SubFunctionHandler(BASIC_MODE_FUNCTION007_HANDLERS), //  007
        new LAFunctionHandler(),    //  010
        new LNAFunctionHandler(),   //  011
        new LMAFunctionHandler(),   //  012
        new LNMAFunctionHandler(),  //  013
        new AAFunctionHandler(),    //  014
        new ANAFunctionHandler(),   //  015
        new AMAFunctionHandler(),   //  016
        new ANMAFunctionHandler(),  //  017
        new AUFunctionHandler(),    //  020
        new ANUFunctionHandler(),   //  021
        null,           //  022
        new LRFunctionHandler(),    //  023
        new AXFunctionHandler(),    //  024
        new ANXFunctionHandler(),   //  025
        new LXMFunctionHandler(),   //  026
        new LXFunctionHandler(),    //  027
        new MIFunctionHandler(),    //  030
        new MSIFunctionHandler(),   //  031
        new MFFunctionHandler(),    //  032
        null,           //  033
        new DIFunctionHandler(),    //  034
        new DSFFunctionHandler(),   //  035
        new DFFunctionHandler(),    //  036
        null,           //  037
        new ORFunctionHandler(),    //  040
        new XORFunctionHandler(),   //  041
        new ANDFunctionHandler(),   //  042
        new MLUFunctionHandler(),   //  043
        new TEPFunctionHandler(),   //  044
        new TOPFunctionHandler(),   //  045
        new LXIFunctionHandler(),   //  046
        new TLEMFunctionHandler(),  //  047
        new TZFunctionHandler(),    //  050
        new TNZFunctionHandler(),   //  051
        new TEFunctionHandler(),    //  052
        new TNEFunctionHandler(),   //  053
        new TLEFunctionHandler(),   //  054
        new TGFunctionHandler(),    //  055
        new TWFunctionHandler(),    //  056
        new TNWFunctionHandler(),   //  057
        new TPFunctionHandler(),    //  060
        new TNFunctionHandler(),    //  061
        null,           //  062
        null,           //  063
        null,           //  064
        null,           //  065
        null,           //  066
        null,           //  067
        new JGDFunctionHandler(),   //  070
        new SubFunctionHandler(BASIC_MODE_FUNCTION071_HANDLERS),    //  071
        new SubFunctionHandler(BASIC_MODE_FUNCTION072_HANDLERS),    //  072
        new SubFunctionHandler(BASIC_MODE_FUNCTION073_HANDLERS),    //  073
        new SubFunctionHandler(BASIC_MODE_FUNCTION074_HANDLERS),    //  074
        new SubFunctionHandler(BASIC_MODE_FUNCTION075_HANDLERS),    //  075
        null,           //  076
        new SubFunctionHandler(BASIC_MODE_FUNCTION077_HANDLERS),    //  077
    };

    /**
     * Extended Mode function handlers for f-field 005, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION005_HANDLERS = {
        new SZFunctionHandler(),    //  000
        new SNZFunctionHandler(),   //  001
        new SP1FunctionHandler(),   //  002
        new SN1FunctionHandler(),   //  003
        new SFSFunctionHandler(),   //  004
        new SFZFunctionHandler(),   //  005
        new SASFunctionHandler(),   //  006
        new SAZFunctionHandler(),   //  007
        new INCFunctionHandler(),   //  010
        new DECFunctionHandler(),   //  011
        new INC2FunctionHandler(),  //  012
        new DEC2FunctionHandler(),  //  013
        new ENZFunctionHandler(),   //  014
        new ADD1FunctionHandler(),  //  015
        new SUB1FunctionHandler(),  //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 007, j-field 016, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION007_016_HANDLERS = {
        new LOCLFunctionHandler(),  //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        new CALLFunctionHandler(),  //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 007, j-field 017, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION007_017_HANDLERS = {
        new GOTOFunctionHandler(),  //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 007, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION007_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        new LAQWFunctionHandler(),  //  004
        new SAQWFunctionHandler(),  //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION007_016_HANDLERS),  //  016
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION007_017_HANDLERS),  //  017
    };

    /**
     * Extended Mode function handlers for f-field 033, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION033_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        new TGMFunctionHandler(),   //  013
        new DTGMFunctionHandler(),  //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 037, j-field 004, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION037_004_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        new KCHGFunctionHandler(),  //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 037, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION037_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION037_004_HANDLERS),  //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 050, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION050_HANDLERS = {
        new TNOPFunctionHandler(),  //  000
        new TGZFunctionHandler(),   //  001
        new TPZFunctionHandler(),   //  002
        new TPFunctionHandler(),    //  003
        new TMZFunctionHandler(),   //  004
        new TMZGFunctionHandler(),  //  005
        new TZFunctionHandler(),    //  006
        new TNLZFunctionHandler(),  //  007
        new TLZFunctionHandler(),   //  010
        new TNZFunctionHandler(),   //  011
        new TPZLFunctionHandler(),  //  012
        new TNMZFunctionHandler(),  //  013
        new TNFunctionHandler(),    //  014
        new TNPZFunctionHandler(),  //  015
        new TNGZFunctionHandler(),  //  016
        new TSKPFunctionHandler(),  //  017
    };

    /**
     * Extended Mode function handlers for f-field 071, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION071_HANDLERS = {
        new MTEFunctionHandler(),   //  000
        new MTNEFunctionHandler(),  //  001
        new MTLEFunctionHandler(),  //  002
        new MTGFunctionHandler(),   //  003
        new MTWFunctionHandler(),   //  004
        new MTNWFunctionHandler(),  //  005
        new MATLFunctionHandler(),  //  006
        new MATGFunctionHandler(),  //  007
        new DAFunctionHandler(),    //  010
        new DANFunctionHandler(),   //  011
        new DSFunctionHandler(),    //  012
        new DLFunctionHandler(),    //  013
        new DLNFunctionHandler(),   //  014
        new DLMFunctionHandler(),   //  015
        new DJZFunctionHandler(),   //  016
        new DTEFunctionHandler(),   //  017
    };

    /**
     * Extended Mode function handlers for f-field 072, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION072_HANDLERS = {
        new TRARSFunctionHandler(), //  000
        null,           //  001
        new JPSFunctionHandler(),   //  002
        new JNSFunctionHandler(),   //  003
        new AHFunctionHandler(),    //  004
        new ANHFunctionHandler(),   //  005
        new ATFunctionHandler(),    //  006
        new ANTFunctionHandler(),   //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        new TRAFunctionHandler(),   //  015
        new SRSFunctionHandler(),   //  016
        new LRSFunctionHandler(),   //  017
    };

    /**
     * Extended Mode function handlers for f-field 073, j-field 014, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION073_014_HANDLERS = {
        new NOPFunctionHandler(),   //  000
        null,           //  001
        new BUYFunctionHandler(),   //  002
        new SELLFunctionHandler(),  //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 073, j-field 015, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION073_015_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        new ACELFunctionHandler(),  //  003
        new DCELFunctionHandler(),  //  004
        new SPIDFunctionHandler(),  //  005
        new DABTFunctionHandler(),  //  006
        null,           //  007
        null,           //  010
        null,           //  011
        new LAEFunctionHandler(),   //  012
        new SKQTFunctionHandler(),  //  013
        new LDFunctionHandler(),    //  014
        new SDFunctionHandler(),    //  015
        new URFunctionHandler(),    //  016
        new SGNLFunctionHandler(),  //  017
    };

    /**
     * Extended Mode function handlers for f-field 073, j-field 017, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION073_017_HANDLERS = {
        new TSFunctionHandler(),    //  000
        new TSSFunctionHandler(),   //  001
        new TCSFunctionHandler(),   //  002
        new RTNFunctionHandler(),   //  003
        new LUDFunctionHandler(),   //  004
        new SUDFunctionHandler(),   //  005
        new IARFunctionHandler(),   //  006
        null,           //  007
        new IPCFunctionHandler(),   //  010
        null,           //  011
        new SYSCFunctionHandler(),  //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 073, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION073_HANDLERS = {
        new SSCFunctionHandler(),   //  000
        new DSCFunctionHandler(),   //  001
        new SSLFunctionHandler(),   //  002
        new DSLFunctionHandler(),   //  003
        new SSAFunctionHandler(),   //  004
        new DSAFunctionHandler(),   //  005
        new LSCFunctionHandler(),   //  006
        new DLSCFunctionHandler(),  //  007
        new LSSCFunctionHandler(),  //  010
        new LDSCFunctionHandler(),  //  011
        new LSSLFunctionHandler(),  //  012
        new LDSLFunctionHandler(),  //  013
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION073_014_HANDLERS),  //  014
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION073_015_HANDLERS),  //  015
        null,           //  016
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION073_017_HANDLERS),  //  017
    };

    /**
     * Extended Mode function handlers for f-field 074 j-field 014, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION074_014_HANDLERS = {
        new JOFunctionHandler(),    //  000
        null,           //  001
        null,           //  002
        new JDFFunctionHandler(),   //  003
        new JCFunctionHandler(),    //  004
        new JNCFunctionHandler(),   //  005
        new AAIJFunctionHandler(),  //  006
        new PAIJFunctionHandler(),  //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 074 j-field 015, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION074_015_HANDLERS = {
        new JNOFunctionHandler(),   //  000
        null,           //  001
        null,           //  002
        new JNDFFunctionHandler(),  //  003
        new JFunctionHandler(),     //  004
        new HLTJFunctionHandler(),  //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 074, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION074_HANDLERS = {
        new JZFunctionHandler(),    //  000
        new JNZFunctionHandler(),   //  001
        new JPFunctionHandler(),    //  002
        new JNFunctionHandler(),    //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        new JNBFunctionHandler(),   //  010
        new JBFunctionHandler(),    //  011
        new JMGIFunctionHandler(),  //  012
        new LMJFunctionHandler(),   //  013
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION074_014_HANDLERS),  //  014
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION074_015_HANDLERS),  //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 075, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION075_HANDLERS = {
        new LBUFunctionHandler(),   //  000
        null,           //  001
        new SBUFunctionHandler(),   //  002
        new LBEFunctionHandler(),   //  003
        new SBEDFunctionHandler(),  //  004
        new LBEDFunctionHandler(),  //  005
        new SBUDFunctionHandler(),  //  006
        new LBUDFunctionHandler(),  //  007
        new TVAFunctionHandler(),   //  010
        null,           //  011
        null,           //  012
        new LXLMFunctionHandler(),  //  013
        new LBNFunctionHandler(),   //  014
        new CRFunctionHandler(),    //  015
        null,           //  016
        null,           //  017
    };

    /**
     * Extended Mode function handlers for f-field 077 j-field 017, indexed by a-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION077_017_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        new HALTFunctionHandler(),
    };

    /**
     * Extended Mode function handlers for f-field 077, indexed by j-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTION077_HANDLERS = {
        null,           //  000
        null,           //  001
        null,           //  002
        null,           //  003
        null,           //  004
        null,           //  005
        null,           //  006
        null,           //  007
        null,           //  010
        null,           //  011
        null,           //  012
        null,           //  013
        null,           //  014
        null,           //  015
        null,           //  016
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION077_017_HANDLERS),  //  017
    };

    /**
     * Extended mode function handler vector indexed by the instruction f-field
     */
    private static final FunctionHandler[] EXTENDED_MODE_FUNCTIONS = {
        null,           //  000
        new SAFunctionHandler(),    //  001
        new SNAFunctionHandler(),   //  002
        new SMAFunctionHandler(),   //  003
        new SRFunctionHandler(),    //  004
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION005_HANDLERS), //  005
        new SXFunctionHandler(),    //  006
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION007_HANDLERS), //  007
        new LAFunctionHandler(),    //  010
        new LNAFunctionHandler(),   //  011
        new LMAFunctionHandler(),   //  012
        new LNMAFunctionHandler(),  //  013
        new AAFunctionHandler(),    //  014
        new ANAFunctionHandler(),   //  015
        new AMAFunctionHandler(),   //  016
        new ANMAFunctionHandler(),  //  017
        new AUFunctionHandler(),    //  020
        new ANUFunctionHandler(),   //  021
        null,           //  022
        new LRFunctionHandler(),    //  023
        new AXFunctionHandler(),    //  024
        new ANXFunctionHandler(),   //  025
        new LXMFunctionHandler(),   //  026
        new LXFunctionHandler(),    //  027
        new MIFunctionHandler(),    //  030
        new MSIFunctionHandler(),   //  031
        new MFFunctionHandler(),    //  032
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION033_HANDLERS), //  033
        new DIFunctionHandler(),    //  034
        new DSFFunctionHandler(),   //  035
        new DFFunctionHandler(),    //  036
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION037_HANDLERS), //  037
        new ORFunctionHandler(),    //  040
        new XORFunctionHandler(),   //  041
        new ANDFunctionHandler(),   //  042
        new MLUFunctionHandler(),   //  043
        new TEPFunctionHandler(),   //  044
        new TOPFunctionHandler(),   //  045
        new LXIFunctionHandler(),   //  046
        new TLEMFunctionHandler(),  //  047
        new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION050_HANDLERS),  //  050 sub-indexed by a-field, *NOT* j-field
        new LXSIFunctionHandler(),  //  051
        new TEFunctionHandler(),    //  052
        new TNEFunctionHandler(),   //  053
        new TLEFunctionHandler(),   //  054
        new TGFunctionHandler(),    //  055
        new TWFunctionHandler(),    //  056
        new TNWFunctionHandler(),   //  057
        new LSBOFunctionHandler(),  //  060
        new LSBLFunctionHandler(),  //  061
        null,           //  062
        null,           //  063
        null,           //  064
        null,           //  065
        null,           //  066
        null,           //  067
        new JGDFunctionHandler(),   //  070
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION071_HANDLERS), //  071
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION072_HANDLERS), //  072
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION073_HANDLERS), //  073
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION074_HANDLERS), //  074
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION075_HANDLERS), //  075
        null,           //  076
        new SubFunctionHandler(EXTENDED_MODE_FUNCTION077_HANDLERS), //  077
    };

    /**
     * Retrieves the proper instruction/function handler given the instruction word
     * @param iw instruction word of interest
     * @param basicMode true if we are in basic mode, false if extended mode
     * @return InstructionHandler if found, else null
     */
    public static FunctionHandler lookup(
        final InstructionWord iw,
        final boolean basicMode
    ) {
        int fField = (int)iw.getF();
        FunctionHandler handler = basicMode ? BASIC_MODE_FUNCTIONS[fField] : EXTENDED_MODE_FUNCTIONS[fField];

        if (handler instanceof SubFunctionHandler) {
            handler = ((SubFunctionHandler)handler).getHandler((int)iw.getJ());
        }

        if (handler instanceof SubSubFunctionHandler) {
            handler = ((SubSubFunctionHandler)handler).getHandler((int)iw.getA());
        }

        return handler;
    }
}

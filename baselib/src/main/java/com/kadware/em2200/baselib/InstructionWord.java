/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

import com.kadware.em2200.baselib.exceptions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Extends the Word36 class to model an instruction word.
 */
public class InstructionWord extends Word36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Enumerators
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  For the instruction info table... what does the A field mean?
    public enum ASemantics {
        NONE,           //  A-field has no semantics
        A,              //  describes an A register
        B,              //  describes a B (base) register
        B_EXEC,         //  describes Exec's B (base) register B16-B31 (add 16 to the a-field value)
        R,              //  describes an R register
        X,              //  describes an X register
    }

    //  For the instruction info table...  a particular entry applies to either or both modes
    public enum Mode {
        BASIC,
        EXTENDED,
        EITHER,
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Special handlers for the few cases where automata cannot do the job
     */
    private static abstract class SpecialHandler {

        /**
         * Interprets the given instruction
         * <p>
         * @param instruction
         * <p>
         * @return
         */
        public abstract String interpret(
            final long instruction
        );
    }

    /**
     * BT instruction for extended mode has a special syntax...
     *      BT,j a,bd,*x,bs,*d
     * bd is base register for destination, bits 24-28 (MSB bit 0)
     * bs is base register for source, bits 20-23
     * d is displacement, bits 29-35
     */
    private static class BTHandler extends SpecialHandler {

        @Override
        public String interpret(
            final long instruction
        ) {
            StringBuilder builder = new StringBuilder();
            builder.append("BT");

            int j = (int)getJ(instruction);
            if ( j != 0 ) {
                builder.append(",");
                builder.append(J_FIELD_NAMES[j]);
            }

            builder.append(" ");
            while (builder.length() < 12 ) {
                builder.append(" ");
            }

            //TODO:BUG Fix this:
            //    When operating at PP < 2, the F0.i is used as an extension to
            //    F0.bs but not F0.bd.
            builder.append(String.format("X%d,", getA(instruction)));
            builder.append(String.format("B%d,", (instruction & 07600) >> 7));

            if (getH(instruction) > 0) {
                builder.append("*");
            }
            builder.append(String.format("X%d,", getX(instruction)));
            builder.append(String.format("B%d,", getS4(instruction) & 017));

            if (getI(instruction) > 0) {
                builder.append("*");
            }
            builder.append(String.format("0%o", instruction & 0177));
            return builder.toString();
        }
    }

    /**
     * ER instruction special handling:
     *      ER er_name
     */
    private static class ERHandler extends SpecialHandler {

        @Override
        public String interpret(
            final long instruction
        ) {
            StringBuilder builder = new StringBuilder();
            builder.append("ER          ");

            int erIndex = (int)getU(instruction);
            String erName = ER_NAMES.get(erIndex);
            if (erName == null) {
                builder.append(String.format("0%o", erIndex));
            } else {
                builder.append(erName);
            }

            long x = getX(instruction);
            if (x > 0) {
                builder.append(",");
                if (getH(instruction) > 0) {
                    builder.append("*");
                }
                builder.append(String.format("X%d", x));
            }

            return builder.toString();
        }
    }

    /**
     * JGD instruction special handling:
     *      JGD a,*u,*x for either mode.
     * Concatenation of j-field and a-field limited to 7 bits, specifics a GRS location displayed in the a-field.
     */
    private static class JGDHandler extends SpecialHandler {

        @Override
        public String interpret(
            final long instruction
        ) {
            long j = getJ(instruction);
            long a = getA(instruction);
            int grsIndex = (int)(( (j << 4) | a ) & 0x7F);

            StringBuilder builder = new StringBuilder();
            builder.append("JGD         ");
            builder.append(GeneralRegisterSet.NAMES[grsIndex]);
            builder.append(",");

            if (getI(instruction) > 0) {
                builder.append("*");
            }
            builder.append(String.format("0%o", getU(instruction)));

            long x = getX(instruction);
            if ( x > 0 ) {
                builder.append(",");
                if (getH(instruction) > 0) {
                    builder.append("*");
                }
                builder.append(String.format("X%d", x));
            }

            return builder.toString();
        }
    }

    /**
     * Describes a particular instruction, given various values for F, J, and A fields among other things.
     * This is predominantly for the process of interpreting an instruction for display purposes.
     */
    public static class InstructionInfo {
        /**
         * This object describes an instruction with the various F, J, and/or A fields, for this mode
         */
        public final Mode _mode;

        /**
         * This object describes an instruction with this value in the F field
         */
        public final int _fField;

        /**
         * This object describes an instruction with this value in the J field (but see _jFlag)
         */
        public final int _jField;

        /**
         * This object describes an instruction with this value in the A field (but see _aFlag and _aSemantics)
         */
        public final int _aField;

        /**
         *  true if match is required on j-field, and to prevent j-field interpretation
         */
        public final boolean _jFlag;

        /**
         * true if match is required on a-field, and to prevent a-field interpretation
         */
        public final boolean _aFlag;

        /**
         * true if this instruction interprets U<=0200 as a GRS address
         */
        public final boolean _grsFlag;

        /**
         * if _jFlag is false, this flag is true to allow j>=016; ignore if _jFlag is true
         */
        public final boolean _immediateFlag;

        /**
         * Indicates the meaning of the A field
         */
        public final ASemantics _aSemantics;

        /**
         * Instruction mnemonic
         */
        public final String _mnemonic;

        /**
         * Special case handler (null for most instances)
         */
        public final SpecialHandler _handler;

        /**
         * For Extended Mode jump instructions which use u field instead of b and d fields
         */
        public final boolean _useBMSemantics;

        /**
         * Constructor for most cases
         * <p>
         * @param mode
         * @param fField
         * @param jField
         * @param aField
         * @param jFlag
         * @param aFlag
         * @param grsFlag
         * @param immediateFlag
         * @param aSemantics
         * @param mnemonic
         * @param useBMSemantics
         */
        public InstructionInfo(
            final Mode mode,
            final int fField,
            final int jField,
            final int aField,
            final boolean jFlag,
            final boolean aFlag,
            final boolean grsFlag,
            final boolean immediateFlag,
            final ASemantics aSemantics,
            final String mnemonic,
            final boolean useBMSemantics
        ) {
            _mode = mode;
            _fField = fField;
            _jField = jField;
            _aField = aField;
            _jFlag = jFlag;
            _aFlag = aFlag;
            _grsFlag = grsFlag;
            _immediateFlag = immediateFlag;
            _aSemantics = aSemantics;
            _mnemonic = mnemonic;
            _useBMSemantics = useBMSemantics;
            _handler = null;
        }

        /**
         * Constructor for instructions with special handlers
         * <p>
         * @param mode
         * @param fField
         * @param jField
         * @param aField
         * @param jFlag
         * @param aFlag
         * @param grsFlag
         * @param immediateFlag
         * @param aSemantics
         * @param mnemonic
         * @param useBMSemantics
         * @param handler
         */
        public InstructionInfo(
            final Mode mode,
            final int fField,
            final int jField,
            final int aField,
            final boolean jFlag,
            final boolean aFlag,
            final boolean grsFlag,
            final boolean immediateFlag,
            final ASemantics aSemantics,
            final String mnemonic,
            final boolean useBMSemantics,
            final SpecialHandler handler
        ) {
            _mode = mode;
            _fField = fField;
            _jField = jField;
            _aField = aField;
            _jFlag = jFlag;
            _aFlag = aFlag;
            _grsFlag = grsFlag;
            _immediateFlag = immediateFlag;
            _aSemantics = aSemantics;
            _mnemonic = mnemonic;
            _useBMSemantics = useBMSemantics;
            _handler = handler;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constants
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static final long MASK_F         = 0770000000000l;
    public static final long MASK_J         = 0007400000000l;
    public static final long MASK_A         = 0000360000000l;
    public static final long MASK_X         = 0000017000000l;
    public static final long MASK_H         = 0000000400000l;
    public static final long MASK_I         = 0000000200000l;
    public static final long MASK_U         = 0000000177777l;
    public static final long MASK_HIU       = 0000000777777l;
    public static final long MASK_B         = 0000000170000l;   // extended-mode base
    public static final long MASK_IB        = 0000000370000l;   // extended-mode extended-base (5 bits, using I field)
    public static final long MASK_D         = 0000000007777l;   // extended-mode displacement

    public static final long MASK_NOT_F     = 0007777777777l;
    public static final long MASK_NOT_J     = 0770377777777l;
    public static final long MASK_NOT_A     = 0777417777777l;
    public static final long MASK_NOT_X     = 0777760777777l;
    public static final long MASK_NOT_H     = 0777777377777l;
    public static final long MASK_NOT_I     = 0777777577777l;
    public static final long MASK_NOT_U     = 0777777600000l;
    public static final long MASK_NOT_HIU   = 0777777000000l;
    public static final long MASK_NOT_B     = 0777777607777l;
    public static final long MASK_NOT_IB    = 0777777407777l;
    public static final long MASK_NOT_D     = 0777777770000l;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Internal static tables
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Mnemonics of the various Executive Requests, indexed by the ER number
     */
    private static final Map<Integer, String> ER_NAMES = new HashMap<>();
    static {
        ER_NAMES.put(01,    "IO$");
        ER_NAMES.put(02,    "IOI$");
        ER_NAMES.put(03,    "IOW$");
        ER_NAMES.put(04,    "EDJS$");
        ER_NAMES.put(06,    "WAIT$");
        ER_NAMES.put(07,    "WANY$");
        ER_NAMES.put(010,   "COM$");
        ER_NAMES.put(011,   "EXIT$");
        ER_NAMES.put(012,   "ABORT$");
        ER_NAMES.put(013,   "FORK$");
        ER_NAMES.put(014,   "TFORK$");
        ER_NAMES.put(015,   "READ$");
        ER_NAMES.put(016,   "PRINT$");
        ER_NAMES.put(017,   "CSF$");
        ER_NAMES.put(022,   "DATE$");
        ER_NAMES.put(023,   "TIME$");
        ER_NAMES.put(024,   "IOWI$");
        ER_NAMES.put(025,   "IOXI$");
        ER_NAMES.put(026,   "EABT$");
        ER_NAMES.put(027,   "II$");
        ER_NAMES.put(030,   "ABSAD$");
        ER_NAMES.put(032,   "FITEM$");
        ER_NAMES.put(033,   "INT$");
        ER_NAMES.put(034,   "IDENT$");
        ER_NAMES.put(035,   "CRTN$");
        ER_NAMES.put(037,   "WALL$");
        ER_NAMES.put(040,   "ERR$");
        ER_NAMES.put(041,   "MCT$");
        ER_NAMES.put(042,   "READA$");
        ER_NAMES.put(043,   "MCORE$");
        ER_NAMES.put(044,   "LCORE$");
        ER_NAMES.put(054,   "TDATE$");
        ER_NAMES.put(060,   "TWAIT$");
        ER_NAMES.put(061,   "RT$");
        ER_NAMES.put(062,   "NRT$");
        ER_NAMES.put(063,   "OPT$");
        ER_NAMES.put(064,   "PCT$");
        ER_NAMES.put(065,   "SETC$");
        ER_NAMES.put(066,   "COND$");
        ER_NAMES.put(067,   "UNLCK$");
        ER_NAMES.put(070,   "APRINT$");
        ER_NAMES.put(071,   "APRNTA$");
        ER_NAMES.put(072,   "APUNCH$");
        ER_NAMES.put(073,   "APNCHA$");
        ER_NAMES.put(074,   "APRTCN$");
        ER_NAMES.put(075,   "APCHCN$");
        ER_NAMES.put(076,   "APRTCA$");
        ER_NAMES.put(077,   "APCHCA$");
        ER_NAMES.put(0100,  "CEND$");
        ER_NAMES.put(0101,  "IALL$");
        ER_NAMES.put(0102,  "TREAD$");
        ER_NAMES.put(0103,  "SWAIT$");
        ER_NAMES.put(0104,  "PFI$");
        ER_NAMES.put(0105,  "PFS$");
        ER_NAMES.put(0106,  "PFD$");
        ER_NAMES.put(0107,  "PFUWL$");
        ER_NAMES.put(0110,  "PFWL$");
        ER_NAMES.put(0111,  "LOAD$");
        ER_NAMES.put(0112,  "RSI$");
        ER_NAMES.put(0113,  "TSQCL$");
        ER_NAMES.put(0114,  "FACIL$");
        ER_NAMES.put(0115,  "BDSPT$");
        ER_NAMES.put(0116,  "INFO$");
        ER_NAMES.put(0117,  "CQUE$");
        ER_NAMES.put(0120,  "TRMRG$");
        ER_NAMES.put(0121,  "TSQRG$");
        ER_NAMES.put(0122,  "CTSQ$");
        ER_NAMES.put(0123,  "CTS$");
        ER_NAMES.put(0124,  "CTSA$");
        ER_NAMES.put(0125,  "MSCON$");
        ER_NAMES.put(0126,  "SNAP$");
        ER_NAMES.put(0130,  "PUNCH$");
        ER_NAMES.put(0134,  "AWAIT$");
        ER_NAMES.put(0135,  "TSWAP$");
        ER_NAMES.put(0136,  "TINTL$");
        ER_NAMES.put(0137,  "PRTCN$");
        ER_NAMES.put(0140,  "ACSF$");
        ER_NAMES.put(0141,  "TOUT$");
        ER_NAMES.put(0142,  "TLBL$");
        ER_NAMES.put(0143,  "FACIT$");
        ER_NAMES.put(0144,  "PRNTA$");
        ER_NAMES.put(0145,  "PNCHA$");
        ER_NAMES.put(0146,  "NAME$");
        ER_NAMES.put(0147,  "ACT$");
        ER_NAMES.put(0150,  "DACT$");
        ER_NAMES.put(0153,  "CLIST$");
        ER_NAMES.put(0155,  "PRTCA$");
        ER_NAMES.put(0156,  "SETBP$");
        ER_NAMES.put(0157,  "PSR$");
        ER_NAMES.put(0160,  "BANK$");
        ER_NAMES.put(0161,  "ADED$");
        ER_NAMES.put(0163,  "ACCNT$");
        ER_NAMES.put(0164,  "PCHCN$");
        ER_NAMES.put(0165,  "PCHCA$");
        ER_NAMES.put(0166,  "AREAD$");
        ER_NAMES.put(0167,  "AREADA$");
        ER_NAMES.put(0170,  "ATREAD$");
        ER_NAMES.put(0175,  "CLCAL$");
        ER_NAMES.put(0176,  "SYSBAL$");
        ER_NAMES.put(0200,  "SYMB$");
        ER_NAMES.put(0202,  "ERRPR$");
        ER_NAMES.put(0207,  "LEVEL$");
        ER_NAMES.put(0210,  "LOG$");
        ER_NAMES.put(0212,  "CREG$");
        ER_NAMES.put(0213,  "SREG$");
        ER_NAMES.put(0214,  "SUVAL$");
        ER_NAMES.put(0215,  "SUMOD$");
        ER_NAMES.put(0216,  "STAB$");
        ER_NAMES.put(0222,  "SDEL$");
        ER_NAMES.put(0223,  "SPRNT$");
        ER_NAMES.put(0225,  "SABORT$");
        ER_NAMES.put(0227,  "DMSS$");
        ER_NAMES.put(0230,  "DMCM$");
        ER_NAMES.put(0231,  "DMES$");
        ER_NAMES.put(0232,  "DMRB$");
        ER_NAMES.put(0233,  "DMGC$");
        ER_NAMES.put(0234,  "ERCVS$");
        ER_NAMES.put(0235,  "MQF$");
        ER_NAMES.put(0236,  "SC$Q$");
        ER_NAMES.put(0237,  "DMABT$");
        ER_NAMES.put(0241,  "AUDIT$");
        ER_NAMES.put(0242,  "SYMINFO$");
        ER_NAMES.put(0243,  "SMOQUE$");
        ER_NAMES.put(0244,  "KEYIN$");
        ER_NAMES.put(0246,  "HMDBIT$");
        ER_NAMES.put(0247,  "CSI$");
        ER_NAMES.put(0250,  "CONFIG$");
        ER_NAMES.put(0251,  "TRTIM$");
        ER_NAMES.put(0252,  "ERTRAP$");
        ER_NAMES.put(0253,  "REGRTN$");
        ER_NAMES.put(0254,  "REGREP$");
        ER_NAMES.put(0255,  "TRAPRTN$");
        ER_NAMES.put(0263,  "TRON$");
        ER_NAMES.put(0264,  "DWTIME$");
        ER_NAMES.put(0266,  "MCODE$");
        ER_NAMES.put(0267,  "IOAID$");
        ER_NAMES.put(0270,  "AP$KEY");
        ER_NAMES.put(0271,  "AT$KEY");
        ER_NAMES.put(0272,  "SYSLOG$");
        ER_NAMES.put(0273,  "MODPS$");
        ER_NAMES.put(0274,  "TERMRUN$");
        ER_NAMES.put(0277,  "QECL$");
        ER_NAMES.put(0300,  "DQECL$");
        ER_NAMES.put(0303,  "SATTCP$");
        ER_NAMES.put(0304,  "SCDTL$");
        ER_NAMES.put(0305,  "SCDTA$");
        ER_NAMES.put(0307,  "TVSLBL$");
        ER_NAMES.put(0311,  "HOST$");
        ER_NAMES.put(0312,  "SCLDT$");
        ER_NAMES.put(0313,  "SCOMCNV$");
        ER_NAMES.put(0314,  "H2CON$");
        ER_NAMES.put(0317,  "UDSPP$");
        ER_NAMES.put(02004, "RT$INT");
        ER_NAMES.put(02005, "RT$OUT");
        ER_NAMES.put(02006, "CMS$REG");
        ER_NAMES.put(02011, "CA$ASG");
        ER_NAMES.put(02012, "CA$REL");
        ER_NAMES.put(02021, "CR$ELG");
        ER_NAMES.put(02030, "AC$NIT");
        ER_NAMES.put(02031, "VT$RD");
        ER_NAMES.put(02041, "VT$CHG");
        ER_NAMES.put(02042, "VT$PUR");
        ER_NAMES.put(02044, "TP$APL");
        ER_NAMES.put(02046, "TF$KEY");
        ER_NAMES.put(02047, "EX$CRD");
        ER_NAMES.put(02050, "DM$FAC");
        ER_NAMES.put(02051, "DM$IO");
        ER_NAMES.put(02052, "DM$IOW");
        ER_NAMES.put(02053, "DM$WT");
        ER_NAMES.put(02054, "BT$DIS");
        ER_NAMES.put(02055, "BT$ENA");
        ER_NAMES.put(02056, "FLAGBOX");
        ER_NAMES.put(02060, "RT$PSI");
        ER_NAMES.put(02061, "RT$PSD");
        ER_NAMES.put(02061, "TPLIB$");
        ER_NAMES.put(02065, "XFR$");
        ER_NAMES.put(02066, "CALL$");
        ER_NAMES.put(02067, "RTN$");
        ER_NAMES.put(02070, "TCORE$");
        ER_NAMES.put(02071, "XRS$");
        ER_NAMES.put(02074, "CO$MIT");
        ER_NAMES.put(02075, "RL$BAK");
        ER_NAMES.put(02101, "RT$PSS");
        ER_NAMES.put(02102, "RT$PID");
        ER_NAMES.put(02103, "SEXEM$");
        ER_NAMES.put(02104, "TIP$Q");
        ER_NAMES.put(02106, "QI$NIT");
        ER_NAMES.put(02107, "QI$CON");
        ER_NAMES.put(02110, "QI$DIS");
        ER_NAMES.put(02111, "TIP$TA");
        ER_NAMES.put(02112, "TIP$TC");
        ER_NAMES.put(02113, "TIP$ID");
        ER_NAMES.put(02114, "MCABT$");
        ER_NAMES.put(02115, "MSGN$");
        ER_NAMES.put(02117, "PERF$");
        ER_NAMES.put(02120, "TIP$XMIT");
        ER_NAMES.put(02130, "TIP$SM");
        ER_NAMES.put(02131, "TIP$TALK");
        ER_NAMES.put(02132, "SC$SR");
        ER_NAMES.put(02133, "TM$SET");
    }

    /**
     * Mnemonics representing the various values for the j-field when it represents a partial-word
     */
    private static final String J_FIELD_NAMES[] =
    {
        "W",        "H2",       "H1",       "XH2",
        "XH1/Q2",   "T3/Q4",    "T2/Q3",    "T1/Q1",
        "S6",       "S5",       "S4",       "S3",
        "S2",       "S1",       "U",        "XU"
    };

    /**
     * Table of InstructionInfo object describing how all the various instructions are to be interpreted for display purposes.
     */
    private static final InstructionInfo[] INSTRUCTION_INFOS = {
        //                  mode           f    j    a    jFlag  aFlag  grsFl  immFl  ASEM               MNEM     bmSem  Handler
        new InstructionInfo(Mode.EITHER,   001, 016, 000, true,  false, false, false, ASemantics.A,     "PRBA",  false),
        new InstructionInfo(Mode.EITHER,   001, 000, 000, false, false, true,  false, ASemantics.A,     "SA",    false),
        new InstructionInfo(Mode.EITHER,   002, 000, 000, false, false, true,  false, ASemantics.A,     "SNA",   false),
        new InstructionInfo(Mode.EITHER,   003, 016, 000, true,  false, false, false, ASemantics.A,     "PRBC",  false),
        new InstructionInfo(Mode.EITHER,   003, 000, 000, false, false, true,  false, ASemantics.A,     "SMA",   false),
        new InstructionInfo(Mode.EITHER,   004, 000, 000, false, false, true,  false, ASemantics.R,     "SR",    false),
        new InstructionInfo(Mode.EITHER,   005, 000, 000, false, true,  true,  false, ASemantics.NONE,  "SZ",    false),
        new InstructionInfo(Mode.EITHER,   005, 000, 001, false, true,  true,  false, ASemantics.NONE,  "SNZ",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 002, false, true,  true,  false, ASemantics.NONE,  "SP1",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 003, false, true,  true,  false, ASemantics.NONE,  "SN1",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 004, false, true,  true,  false, ASemantics.NONE,  "SFS",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 005, false, true,  true,  false, ASemantics.NONE,  "SFZ",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 006, false, true,  true,  false, ASemantics.NONE,  "SAS",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 007, false, true,  true,  false, ASemantics.NONE,  "SAZ",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 010, false, true,  true,  false, ASemantics.NONE,  "INC",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 011, false, true,  true,  false, ASemantics.NONE,  "DEC",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 012, false, true,  true,  false, ASemantics.NONE,  "INC2",  false),
        new InstructionInfo(Mode.EITHER,   005, 000, 013, false, true,  true,  false, ASemantics.NONE,  "DEC2",  false),
        new InstructionInfo(Mode.EITHER,   005, 000, 014, false, true,  true,  false, ASemantics.NONE,  "ENZ",   false),
        new InstructionInfo(Mode.EITHER,   005, 000, 015, false, true,  true,  false, ASemantics.NONE,  "ADD1",  false),
        new InstructionInfo(Mode.EITHER,   005, 000, 016, false, true,  true,  false, ASemantics.NONE,  "SUB1",  false),
        new InstructionInfo(Mode.EITHER,   006, 000, 000, false, false, true,  false, ASemantics.X,     "SX",    false),
        new InstructionInfo(Mode.EITHER,   007, 000, 000, true,  false, true,  false, ASemantics.A,     "ADE",   false),
        new InstructionInfo(Mode.EITHER,   007, 001, 000, true,  false, true,  false, ASemantics.A,     "DADE",  false),
        new InstructionInfo(Mode.EITHER,   007, 002, 000, true,  false, true,  false, ASemantics.A,     "SDE",   false),
        new InstructionInfo(Mode.EITHER,   007, 003, 000, true,  false, true,  false, ASemantics.A,     "DSDE",  false),
        new InstructionInfo(Mode.EITHER,   007, 004, 000, true,  false, true,  false, ASemantics.A,     "LAQW",  false),
        new InstructionInfo(Mode.EITHER,   007, 005, 000, true,  false, true,  false, ASemantics.A,     "SAQW",  false),
        new InstructionInfo(Mode.EITHER,   007, 006, 000, true,  false, true,  false, ASemantics.A,     "DEI",   false),
        new InstructionInfo(Mode.EITHER,   007, 007, 000, true,  false, true,  false, ASemantics.A,     "DDEI",  false),
        new InstructionInfo(Mode.EITHER,   007, 010, 000, true,  false, true,  false, ASemantics.A,     "IDE",   false),
        new InstructionInfo(Mode.EITHER,   007, 011, 000, true,  false, true,  false, ASemantics.A,     "DIDE",  false),
        new InstructionInfo(Mode.BASIC,    007, 012, 000, true,  false, false, false, ASemantics.X,     "LDJ",   false),
        new InstructionInfo(Mode.BASIC,    007, 013, 000, true,  false, false, false, ASemantics.X,     "LIJ",   false),
        new InstructionInfo(Mode.BASIC,    007, 014, 000, true,  false, false, false, ASemantics.NONE,  "LPD",   false),
        new InstructionInfo(Mode.BASIC,    007, 015, 000, true,  false, false, false, ASemantics.NONE,  "SPD",   false),
        new InstructionInfo(Mode.EXTENDED, 007, 016, 000, true,  true,  false, false, ASemantics.NONE,  "LOCL",  true),
        new InstructionInfo(Mode.EXTENDED, 007, 016, 013, true,  true,  false, false, ASemantics.NONE,  "CALL",  false),
        new InstructionInfo(Mode.BASIC,    007, 017, 000, true,  false, false, false, ASemantics.X,     "LBJ",   false),
        new InstructionInfo(Mode.EXTENDED, 007, 017, 000, true,  true,  false, false, ASemantics.NONE,  "GOTO",  false),
        new InstructionInfo(Mode.EITHER,   010, 000, 000, false, false, true,  true,  ASemantics.A,     "LA",    false),
        new InstructionInfo(Mode.EITHER,   011, 000, 000, false, false, true,  true,  ASemantics.A,     "LNA",   false),
        new InstructionInfo(Mode.EITHER,   012, 000, 000, false, false, true,  true,  ASemantics.A,     "LMA",   false),
        new InstructionInfo(Mode.EITHER,   013, 000, 000, false, false, true,  true,  ASemantics.A,     "LNMA",  false),
        new InstructionInfo(Mode.EITHER,   014, 000, 000, false, false, true,  true,  ASemantics.A,     "AA",    false),
        new InstructionInfo(Mode.EITHER,   015, 000, 000, false, false, true,  true,  ASemantics.A,     "ANA",   false),
        new InstructionInfo(Mode.EITHER,   016, 000, 000, false, false, true,  true,  ASemantics.A,     "AMA",   false),
        new InstructionInfo(Mode.EITHER,   017, 000, 000, false, false, true,  true,  ASemantics.A,     "ANMA",  false),
        new InstructionInfo(Mode.EITHER,   020, 000, 000, false, false, true,  true,  ASemantics.A,     "AU",    false),
        new InstructionInfo(Mode.EITHER,   021, 000, 000, false, false, true,  true,  ASemantics.A,     "ANU",   false),
        new InstructionInfo(Mode.BASIC,    022, 000, 000, false, false, true,  false, ASemantics.X,     "BT",    false),
        new InstructionInfo(Mode.EXTENDED, 022, 000, 000, false, false, false, false, ASemantics.NONE,  "BT",    false, new BTHandler()),
        new InstructionInfo(Mode.EITHER,   023, 000, 000, false, false, true,  true,  ASemantics.R,     "LR",    false),
        new InstructionInfo(Mode.EITHER,   024, 000, 000, false, false, true,  true,  ASemantics.X,     "AX",    false),
        new InstructionInfo(Mode.EITHER,   025, 000, 000, false, false, true,  true,  ASemantics.X,     "ANX",   false),
        new InstructionInfo(Mode.EITHER,   026, 000, 000, false, false, true,  true,  ASemantics.X,     "LXM",   false),
        new InstructionInfo(Mode.EITHER,   027, 000, 000, false, false, true,  true,  ASemantics.X,     "LX",    false),
        new InstructionInfo(Mode.EITHER,   030, 000, 000, false, false, true,  true,  ASemantics.A,     "MI",    false),
        new InstructionInfo(Mode.EITHER,   031, 000, 000, false, false, true,  true,  ASemantics.A,     "MSI",   false),
        new InstructionInfo(Mode.EITHER,   032, 000, 000, false, false, true,  true,  ASemantics.A,     "MF",    false),
        new InstructionInfo(Mode.EXTENDED, 033, 010, 000, true,  false, false, false, ASemantics.A,     "LS",    false),
        new InstructionInfo(Mode.EXTENDED, 033, 011, 000, true,  false, false, false, ASemantics.A,     "LSA",   false),
        new InstructionInfo(Mode.EXTENDED, 033, 012, 000, true,  false, false, false, ASemantics.A,     "SS",    false),
        new InstructionInfo(Mode.EXTENDED, 033, 013, 000, true,  false, true,  false, ASemantics.A,     "TGM",   false),
        new InstructionInfo(Mode.EXTENDED, 033, 014, 000, true,  false, true,  false, ASemantics.A,     "DTGM",  false),
        new InstructionInfo(Mode.EXTENDED, 033, 015, 000, true,  false, true,  false, ASemantics.A,     "DCB",   false),
        new InstructionInfo(Mode.EXTENDED, 033, 016, 000, true,  false, false, false, ASemantics.A,     "TES",   false),
        new InstructionInfo(Mode.EXTENDED, 033, 017, 000, true,  false, false, false, ASemantics.A,     "TNES",  false),
        new InstructionInfo(Mode.EITHER,   034, 000, 000, false, false, true,  true,  ASemantics.A,     "DI",    false),
        new InstructionInfo(Mode.EITHER,   035, 000, 000, false, false, true,  true,  ASemantics.A,     "DSF",   false),
        new InstructionInfo(Mode.EITHER,   036, 000, 000, false, false, true,  true,  ASemantics.A,     "DF",    false),
        new InstructionInfo(Mode.EXTENDED, 037, 000, 000, true,  false, false, false, ASemantics.A,     "LRD",   false),
        new InstructionInfo(Mode.EXTENDED, 037, 004, 000, true,  true,  false, false, ASemantics.NONE,  "SMD",   false),
        new InstructionInfo(Mode.EXTENDED, 037, 004, 001, true,  true,  false, false, ASemantics.NONE,  "SDMN",  false),
        new InstructionInfo(Mode.EXTENDED, 037, 004, 002, true,  true,  false, false, ASemantics.NONE,  "SDMF",  false),
        new InstructionInfo(Mode.EXTENDED, 037, 004, 003, true,  true,  false, false, ASemantics.NONE,  "SDMS",  false),
        new InstructionInfo(Mode.EITHER,   037, 007, 000, true,  false, false, false, ASemantics.A,     "LMC",   false),
        new InstructionInfo(Mode.BASIC,    037, 010, 000, true,  false, false, false, ASemantics.NONE,  "BIM",   false),
        new InstructionInfo(Mode.BASIC,    037, 011, 000, true,  false, false, false, ASemantics.NONE,  "BIC",   false),
        new InstructionInfo(Mode.BASIC,    037, 012, 000, true,  false, false, false, ASemantics.NONE,  "BIMT",  false),
        new InstructionInfo(Mode.BASIC,    037, 013, 000, true,  false, false, false, ASemantics.NONE,  "BICL",  false),
        new InstructionInfo(Mode.BASIC,    037, 014, 000, true,  false, false, false, ASemantics.NONE,  "BIML",  false),
        new InstructionInfo(Mode.BASIC,    037, 015, 000, true,  false, false, false, ASemantics.A,     "BDE",   false),
        new InstructionInfo(Mode.BASIC,    037, 016, 000, true,  false, false, false, ASemantics.A,     "DEB",   false),
        new InstructionInfo(Mode.BASIC,    037, 017, 000, true,  false, false, false, ASemantics.A,     "EDDE",  false),
        new InstructionInfo(Mode.EXTENDED, 037, 010, 000, true,  false, true,  false, ASemantics.A,     "ENQ",   false),
        new InstructionInfo(Mode.EXTENDED, 037, 011, 000, true,  false, true,  false, ASemantics.A,     "ENQF",  false),
        new InstructionInfo(Mode.EXTENDED, 037, 012, 000, true,  false, true,  false, ASemantics.A,     "DEQ",   false),
        new InstructionInfo(Mode.EXTENDED, 037, 013, 000, true,  false, true,  false, ASemantics.A,     "DEQW",  false),
        new InstructionInfo(Mode.EITHER,   040, 000, 000, false, false, true,  true,  ASemantics.A,     "OR",    false),
        new InstructionInfo(Mode.EITHER,   041, 000, 000, false, false, true,  true,  ASemantics.A,     "XOR",   false),
        new InstructionInfo(Mode.EITHER,   042, 000, 000, false, false, true,  true,  ASemantics.A,     "AND",   false),
        new InstructionInfo(Mode.EITHER,   043, 000, 000, false, false, true,  true,  ASemantics.A,     "MLU",   false),
        new InstructionInfo(Mode.EITHER,   044, 000, 000, false, false, true,  true,  ASemantics.A,     "TEP",   false),
        new InstructionInfo(Mode.EITHER,   045, 000, 000, false, false, true,  true,  ASemantics.A,     "TOP",   false),
        new InstructionInfo(Mode.EITHER,   046, 000, 000, false, false, true,  true,  ASemantics.X,     "LXI",   false),
        new InstructionInfo(Mode.EITHER,   047, 000, 000, false, false, true,  true,  ASemantics.A,     "TLEM",  false),
        new InstructionInfo(Mode.BASIC,    050, 000, 000, false, false, true,  true,  ASemantics.NONE,  "TZ",    false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 000, false, true,  true,  true,  ASemantics.NONE,  "TNOP",  false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 001, false, true,  true,  true,  ASemantics.NONE,  "TGZ",   false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 002, false, true,  true,  true,  ASemantics.NONE,  "TPZ",   false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 003, false, true,  true,  true,  ASemantics.NONE,  "TP",    false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 004, false, true,  true,  true,  ASemantics.NONE,  "TMZ",   false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 005, false, true,  true,  true,  ASemantics.NONE,  "TMZG",  false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 006, false, true,  true,  true,  ASemantics.NONE,  "TZ",    false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 007, false, true,  true,  true,  ASemantics.NONE,  "TNLZ",  false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 010, false, true,  true,  true,  ASemantics.NONE,  "TLZ",   false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 011, false, true,  true,  true,  ASemantics.NONE,  "TNZ",   false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 012, false, true,  true,  true,  ASemantics.NONE,  "TPZL",  false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 013, false, true,  true,  true,  ASemantics.NONE,  "TNMZ",  false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 014, false, true,  true,  true,  ASemantics.NONE,  "TN",    false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 015, false, true,  true,  true,  ASemantics.NONE,  "TNPZ",  false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 016, false, true,  true,  true,  ASemantics.NONE,  "TNGZ",  false),
        new InstructionInfo(Mode.EXTENDED, 050, 000, 017, false, true,  true,  true,  ASemantics.NONE,  "TSKP",  false),
        new InstructionInfo(Mode.BASIC,    051, 000, 000, false, false, true,  true,  ASemantics.NONE,  "TNX",   false),
        new InstructionInfo(Mode.EXTENDED, 051, 000, 000, false, false, true,  true,  ASemantics.X,     "LXSI",  false),
        new InstructionInfo(Mode.EITHER,   052, 000, 000, false, false, true,  true,  ASemantics.A,     "TE",    false),
        new InstructionInfo(Mode.EITHER,   053, 000, 000, false, false, true,  true,  ASemantics.A,     "TNE",   false),
        new InstructionInfo(Mode.EITHER,   054, 000, 000, false, false, true,  true,  ASemantics.A,     "TLE",   false),
        new InstructionInfo(Mode.EITHER,   055, 000, 000, false, false, true,  true,  ASemantics.A,     "TG",    false),
        new InstructionInfo(Mode.EITHER,   056, 000, 000, false, false, true,  true,  ASemantics.A,     "TW",    false),
        new InstructionInfo(Mode.EITHER,   057, 000, 000, false, false, true,  true,  ASemantics.A,     "TNW",   false),
        new InstructionInfo(Mode.BASIC,    060, 000, 000, false, false, true,  true,  ASemantics.NONE,  "TP",    false),
        new InstructionInfo(Mode.EXTENDED, 060, 000, 000, false, false, true,  true,  ASemantics.X,     "LSBO",  false),
        new InstructionInfo(Mode.BASIC,    061, 000, 000, false, false, true,  true,  ASemantics.NONE,  "TN",    false),
        new InstructionInfo(Mode.EXTENDED, 061, 000, 000, false, false, true,  true,  ASemantics.X,     "LSBL",  false),
        new InstructionInfo(Mode.EITHER,   062, 000, 000, false, false, true,  true,  ASemantics.A,     "SE",    false),
        new InstructionInfo(Mode.EITHER,   063, 000, 000, false, false, true,  true,  ASemantics.A,     "SNE",   false),
        new InstructionInfo(Mode.EITHER,   064, 000, 000, false, false, true,  true,  ASemantics.A,     "SLE",   false),
        new InstructionInfo(Mode.EITHER,   065, 000, 000, false, false, true,  true,  ASemantics.A,     "SG",    false),
        new InstructionInfo(Mode.EITHER,   066, 000, 000, false, false, true,  true,  ASemantics.A,     "SW",    false),
        new InstructionInfo(Mode.EITHER,   067, 000, 000, false, false, true,  true,  ASemantics.A,     "SNW",   false),
        new InstructionInfo(Mode.EITHER,   070, 000, 000, false, false, false, false, ASemantics.NONE,  "JGD",   false, new JGDHandler()),
        new InstructionInfo(Mode.BASIC,    071, 000, 000, true,  false, true,  false, ASemantics.A,     "MSE",   false),
        new InstructionInfo(Mode.BASIC,    071, 001, 000, true,  false, true,  false, ASemantics.A,     "MSNE",  false),
        new InstructionInfo(Mode.BASIC,    071, 002, 000, true,  false, true,  false, ASemantics.A,     "MSLE",  false),
        new InstructionInfo(Mode.BASIC,    071, 003, 000, true,  false, true,  false, ASemantics.A,     "MSG",   false),
        new InstructionInfo(Mode.BASIC,    071, 004, 000, true,  false, true,  false, ASemantics.A,     "MSW",   false),
        new InstructionInfo(Mode.BASIC,    071, 005, 000, true,  false, true,  false, ASemantics.A,     "MSNW",  false),
        new InstructionInfo(Mode.BASIC,    071, 006, 000, true,  false, true,  false, ASemantics.A,     "MASL",  false),
        new InstructionInfo(Mode.BASIC,    071, 007, 000, true,  false, true,  false, ASemantics.A,     "MASG",  false),
        new InstructionInfo(Mode.EXTENDED, 071, 000, 000, true,  false, true,  false, ASemantics.A,     "MTE",   false),
        new InstructionInfo(Mode.EXTENDED, 071, 001, 000, true,  false, true,  false, ASemantics.A,     "MTNE",  false),
        new InstructionInfo(Mode.EXTENDED, 071, 002, 000, true,  false, true,  false, ASemantics.A,     "MTLE",  false),
        new InstructionInfo(Mode.EXTENDED, 071, 003, 000, true,  false, true,  false, ASemantics.A,     "MTG",   false),
        new InstructionInfo(Mode.EXTENDED, 071, 004, 000, true,  false, true,  false, ASemantics.A,     "MTW",   false),
        new InstructionInfo(Mode.EXTENDED, 071, 005, 000, true,  false, true,  false, ASemantics.A,     "MTNW",  false),
        new InstructionInfo(Mode.EXTENDED, 071, 006, 000, true,  false, true,  false, ASemantics.A,     "MATL",  false),
        new InstructionInfo(Mode.EXTENDED, 071, 007, 000, true,  false, true,  false, ASemantics.A,     "MATG",  false),
        new InstructionInfo(Mode.EITHER,   071, 010, 000, true,  false, true,  false, ASemantics.A,     "DA",    false),
        new InstructionInfo(Mode.EITHER,   071, 011, 000, true,  false, true,  false, ASemantics.A,     "DAN",   false),
        new InstructionInfo(Mode.EITHER,   071, 012, 000, true,  false, true,  false, ASemantics.A,     "DS",    false),
        new InstructionInfo(Mode.EITHER,   071, 013, 000, true,  false, true,  false, ASemantics.A,     "DL",    false),
        new InstructionInfo(Mode.EITHER,   071, 014, 000, true,  false, true,  false, ASemantics.A,     "DLN",   false),
        new InstructionInfo(Mode.EITHER,   071, 015, 000, true,  false, true,  false, ASemantics.A,     "DLM",   false),
        new InstructionInfo(Mode.EITHER,   071, 016, 000, true,  false, false, false, ASemantics.A,     "DJZ",   true),
        new InstructionInfo(Mode.EITHER,   071, 017, 000, true,  false, true,  false, ASemantics.A,     "DTE",   false),
        new InstructionInfo(Mode.BASIC,    072, 001, 000, true,  false, false, false, ASemantics.NONE,  "SLJ",   false),
        new InstructionInfo(Mode.EITHER,   072, 002, 000, true,  false, false, false, ASemantics.A,     "JPS",   false),
        new InstructionInfo(Mode.EITHER,   072, 003, 000, true,  false, false, false, ASemantics.A,     "JNS",   false),
        new InstructionInfo(Mode.EITHER,   072, 004, 000, true,  false, true,  false, ASemantics.A,     "AH",    false),
        new InstructionInfo(Mode.EITHER,   072, 005, 000, true,  false, true,  false, ASemantics.A,     "ANH",   false),
        new InstructionInfo(Mode.EITHER,   072, 006, 000, true,  false, true,  false, ASemantics.A,     "AT",    false),
        new InstructionInfo(Mode.EITHER,   072, 007, 000, true,  false, true,  false, ASemantics.A,     "ANT",   false),
        new InstructionInfo(Mode.BASIC,    072, 010, 000, true,  false, false, false, ASemantics.NONE,  "EX",    false),
        new InstructionInfo(Mode.EXTENDED, 072, 010, 000, true,  false, false, false, ASemantics.A,     "BDE",   false),
        new InstructionInfo(Mode.BASIC,    072, 011, 000, true,  false, false, false, ASemantics.NONE,  "ER",    false, new ERHandler()),
        new InstructionInfo(Mode.EXTENDED, 072, 011, 000, true,  false, false, false, ASemantics.A,     "DEB",   false),
        new InstructionInfo(Mode.EITHER,   072, 012, 000, true,  false, true,  false, ASemantics.X,     "BN",    false),
        new InstructionInfo(Mode.EXTENDED, 072, 013, 000, true,  false, false, false, ASemantics.A,     "BAO",   false),
        new InstructionInfo(Mode.EITHER,   072, 014, 000, true,  false, true,  false, ASemantics.X,     "BBN",   false),
        new InstructionInfo(Mode.EITHER,   072, 015, 000, true,  false, false, false, ASemantics.X,     "TRA",   false),
        new InstructionInfo(Mode.EITHER,   072, 016, 000, true,  false, false, false, ASemantics.A,     "SRS",   false),
        new InstructionInfo(Mode.EITHER,   072, 017, 000, true,  false, false, false, ASemantics.A,     "LRS",   false),
        new InstructionInfo(Mode.EITHER,   073, 000, 000, true,  false, false, false, ASemantics.A,     "SSC",   true),
        new InstructionInfo(Mode.EITHER,   073, 001, 000, true,  false, false, false, ASemantics.A,     "DSC",   true),
        new InstructionInfo(Mode.EITHER,   073, 002, 000, true,  false, false, false, ASemantics.A,     "SSL",   true),
        new InstructionInfo(Mode.EITHER,   073, 003, 000, true,  false, false, false, ASemantics.A,     "DSL",   true),
        new InstructionInfo(Mode.EITHER,   073, 004, 000, true,  false, false, false, ASemantics.A,     "SSA",   true),
        new InstructionInfo(Mode.EITHER,   073, 005, 000, true,  false, false, false, ASemantics.A,     "DSA",   true),
        new InstructionInfo(Mode.EITHER,   073, 006, 000, true,  false, false, false, ASemantics.A,     "LSC",   true),
        new InstructionInfo(Mode.EITHER,   073, 007, 000, true,  false, false, false, ASemantics.A,     "DLSC",  true),
        new InstructionInfo(Mode.EITHER,   073, 010, 000, true,  false, false, false, ASemantics.A,     "LSSC",  true),
        new InstructionInfo(Mode.EITHER,   073, 011, 000, true,  false, false, false, ASemantics.A,     "LDSC",  true),
        new InstructionInfo(Mode.EITHER,   073, 012, 000, true,  false, false, false, ASemantics.A,     "LSSL",  true),
        new InstructionInfo(Mode.EITHER,   073, 013, 000, true,  false, false, false, ASemantics.A,     "LDSL",  true),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 000, true,  true,  true,  false, ASemantics.NONE,  "NOP",   false),
        new InstructionInfo(Mode.EITHER,   073, 014, 001, true,  true,  true,  false, ASemantics.NONE,  "LPM",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 002, true,  true,  false, false, ASemantics.NONE,  "BUY",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 003, true,  true,  false, false, ASemantics.NONE,  "SELL",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 004, true,  true,  false, false, ASemantics.NONE,  "UNLK",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 005, true,  true,  false, false, ASemantics.NONE,  "EX",    false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 006, true,  true,  false, false, ASemantics.NONE,  "EXR",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 007, true,  true,  false, false, ASemantics.NONE,  "BIMT",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 010, true,  true,  false, false, ASemantics.NONE,  "BIM",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 011, true,  true,  false, false, ASemantics.NONE,  "BIML",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 012, true,  true,  false, false, ASemantics.NONE,  "BIC",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 013, true,  true,  false, false, ASemantics.NONE,  "BICL",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 014, true,  true,  true,  false, ASemantics.NONE,  "LINC",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 015, true,  true,  true,  false, ASemantics.NONE,  "SINC",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 016, true,  true,  true,  false, ASemantics.NONE,  "LCC",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 014, 017, true,  true,  true,  false, ASemantics.NONE,  "SCC",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 015, 002, true,  true,  true,  false, ASemantics.NONE,  "LBRX",  false),
        new InstructionInfo(Mode.EITHER,   073, 015, 003, true,  true,  false, false, ASemantics.NONE,  "ACEL",  false),
        new InstructionInfo(Mode.EITHER,   073, 015, 004, true,  true,  false, false, ASemantics.NONE,  "DCEL",  false),
        new InstructionInfo(Mode.EITHER,   073, 015, 005, true,  true,  true,  false, ASemantics.NONE,  "SPID",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 015, 006, true,  true,  false, false, ASemantics.NONE,  "DABT",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 015, 007, true,  true,  false, false, ASemantics.NONE,  "SEND",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 015, 010, true,  true,  false, false, ASemantics.NONE,  "ACK",   false),
        new InstructionInfo(Mode.EITHER,   073, 015, 011, true,  true,  true,  false, ASemantics.NONE,  "SPM",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 015, 012, true,  true,  false, false, ASemantics.NONE,  "LAE",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 015, 013, true,  true,  true,  false, ASemantics.NONE,  "SKQT",  false),
        new InstructionInfo(Mode.EITHER,   073, 015, 014, true,  true,  true,  false, ASemantics.NONE,  "LD",    false),
        new InstructionInfo(Mode.EITHER,   073, 015, 015, true,  true,  true,  false, ASemantics.NONE,  "SD",    false),
        new InstructionInfo(Mode.EITHER,   073, 015, 016, true,  true,  false, false, ASemantics.NONE,  "UR",    false),
        new InstructionInfo(Mode.EITHER,   073, 015, 017, true,  true,  false, false, ASemantics.NONE,  "SGNL",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 016, 000, true,  false, false, false, ASemantics.A,     "EDDE",  false),
        new InstructionInfo(Mode.EITHER,   073, 017, 000, true,  true,  false, false, ASemantics.NONE,  "TS",    false),
        new InstructionInfo(Mode.EITHER,   073, 017, 001, true,  true,  false, false, ASemantics.NONE,  "TSS",   false),
        new InstructionInfo(Mode.EITHER,   073, 017, 002, true,  true,  false, false, ASemantics.NONE,  "TCS",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 003, true,  true,  false, false, ASemantics.NONE,  "RTN",   false),
        new InstructionInfo(Mode.EITHER,   073, 017, 004, true,  true,  true,  false, ASemantics.NONE,  "LUD",   false),
        new InstructionInfo(Mode.EITHER,   073, 017, 005, true,  true,  true,  false, ASemantics.NONE,  "SUD",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 006, true,  true,  false, false, ASemantics.NONE,  "IAR",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 007, true,  true,  false, false, ASemantics.NONE,  "ZEROP", false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 010, true,  true,  false, false, ASemantics.NONE,  "IPC",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 011, true,  true,  false, false, ASemantics.NONE,  "CJHE",  true),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 012, true,  true,  false, false, ASemantics.NONE,  "SYSC",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 013, true,  true,  false, false, ASemantics.NONE,  "LATP",  false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 014, true,  true,  false, false, ASemantics.NONE,  "INV",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 015, true,  true,  false, false, ASemantics.NONE,  "SJH",   false),
        new InstructionInfo(Mode.EXTENDED, 073, 017, 016, true,  true,  false, false, ASemantics.NONE,  "SSIP",  false),
        new InstructionInfo(Mode.EITHER,   074, 000, 000, true,  false, false, false, ASemantics.A,     "JZ",    true),
        new InstructionInfo(Mode.EITHER,   074, 001, 000, true,  false, false, false, ASemantics.A,     "JNZ",   true),
        new InstructionInfo(Mode.EITHER,   074, 002, 000, true,  false, false, false, ASemantics.A,     "JP",    true),
        new InstructionInfo(Mode.EITHER,   074, 003, 000, true,  false, false, false, ASemantics.A,     "JN",    true),
        new InstructionInfo(Mode.BASIC,    074, 004, 000, true,  true,  false, false, ASemantics.NONE,  "J",	     true),	// a-field == 0
        new InstructionInfo(Mode.BASIC,    074, 004, 000, true,  false, false, false, ASemantics.NONE,  "JK",    true),	// a-field > 0
        new InstructionInfo(Mode.BASIC,    074, 005, 000, true,  true,  false, false, ASemantics.NONE,  "HJ",    true),	// a-field == 0
        new InstructionInfo(Mode.BASIC,    074, 005, 000, true,  false, false, false, ASemantics.NONE,  "HKJ",   true),	// a-field > 0
        new InstructionInfo(Mode.BASIC,    074, 006, 000, true,  false, true,  false, ASemantics.A,     "NOP",   false),
        new InstructionInfo(Mode.BASIC,    074, 007, 000, true,  false, false, false, ASemantics.NONE,  "AAIJ",  true),
        new InstructionInfo(Mode.EITHER,   074, 010, 000, true,  false, false, false, ASemantics.A,     "JNB",   true),
        new InstructionInfo(Mode.EITHER,   074, 011, 000, true,  false, false, false, ASemantics.A,     "JB",    true),
        new InstructionInfo(Mode.EITHER,   074, 012, 000, true,  false, false, false, ASemantics.X,     "JMGI",  true),
        new InstructionInfo(Mode.EITHER,   074, 013, 000, true,  false, false, false, ASemantics.X,     "LMJ",   true),
        new InstructionInfo(Mode.EITHER,   074, 014, 000, true,  true,  false, false, ASemantics.NONE,  "JO",    true),
        new InstructionInfo(Mode.EITHER,   074, 014, 001, true,  true,  false, false, ASemantics.NONE,  "JFU",   true),
        new InstructionInfo(Mode.EITHER,   074, 014, 002, true,  true,  false, false, ASemantics.NONE,  "JFO",   true),
        new InstructionInfo(Mode.EITHER,   074, 014, 003, true,  true,  false, false, ASemantics.NONE,  "JDF",   true),
        new InstructionInfo(Mode.EXTENDED, 074, 014, 004, true,  true,  false, false, ASemantics.NONE,  "JC",    true),
        new InstructionInfo(Mode.EXTENDED, 074, 014, 005, true,  true,  false, false, ASemantics.NONE,  "JNC",   true),
        new InstructionInfo(Mode.EXTENDED, 074, 014, 006, true,  true,  false, false, ASemantics.NONE,  "AAIJ",  true),
        new InstructionInfo(Mode.EITHER,   074, 014, 007, true,  true,  false, false, ASemantics.NONE,  "PAIJ",  true),
        new InstructionInfo(Mode.EITHER,   074, 015, 000, true,  true,  false, false, ASemantics.NONE,  "JNO",   true),
        new InstructionInfo(Mode.EITHER,   074, 015, 001, true,  true,  false, false, ASemantics.NONE,  "JNFU",  true),
        new InstructionInfo(Mode.EITHER,   074, 015, 002, true,  true,  false, false, ASemantics.NONE,  "JNFO",  true),
        new InstructionInfo(Mode.EITHER,   074, 015, 003, true,  true,  false, false, ASemantics.NONE,  "JNDF",  true),
        new InstructionInfo(Mode.EXTENDED, 074, 015, 004, true,  true,  false, false, ASemantics.NONE,  "J",	    true),
        new InstructionInfo(Mode.EITHER,   074, 015, 005, true,  true,  false, false, ASemantics.NONE,  "HLTJ",  true),
        new InstructionInfo(Mode.BASIC,    074, 016, 000, true,  false, false, false, ASemantics.NONE,  "JC",    true),
        new InstructionInfo(Mode.BASIC,    074, 017, 000, true,  false, false, false, ASemantics.NONE,  "JNC",   true),
        new InstructionInfo(Mode.EITHER,   075, 000, 000, true,  false, true,  false, ASemantics.B,     "LBU",   false),
        new InstructionInfo(Mode.EITHER,   075, 002, 000, true,  false, true,  false, ASemantics.B,     "SBU",   false),
        new InstructionInfo(Mode.EITHER,   075, 003, 000, true,  false, true,  false, ASemantics.B_EXEC,"LBE",   false),
        new InstructionInfo(Mode.EITHER,   075, 004, 000, true,  false, false, false, ASemantics.B_EXEC,"SBED",  false),
        new InstructionInfo(Mode.EITHER,   075, 005, 000, true,  false, false, false, ASemantics.B_EXEC,"LBED",  false),
        new InstructionInfo(Mode.EITHER,   075, 006, 000, true,  false, false, false, ASemantics.B,     "SBUD",  false),
        new InstructionInfo(Mode.EITHER,   075, 007, 000, true,  false, false, false, ASemantics.B,     "LBUD",  false),
        new InstructionInfo(Mode.EITHER,   075, 010, 000, true,  false, false, false, ASemantics.X,     "TVA",   false),
        new InstructionInfo(Mode.EXTENDED, 075, 012, 000, true,  false, false, false, ASemantics.A,     "RDC",   false),
        new InstructionInfo(Mode.EITHER,   075, 013, 000, true,  false, true,  false, ASemantics.X,     "LXLM",  false),
        new InstructionInfo(Mode.EITHER,   075, 014, 000, true,  false, true,  false, ASemantics.X,     "LBN",   false),
        new InstructionInfo(Mode.EITHER,   075, 015, 000, true,  false, false, false, ASemantics.A,     "CR",    false),
        new InstructionInfo(Mode.EITHER,   076, 000, 000, true,  false, true,  false, ASemantics.A,     "FA",    false),
        new InstructionInfo(Mode.EITHER,   076, 001, 000, true,  false, true,  false, ASemantics.A,     "FAN",   false),
        new InstructionInfo(Mode.EITHER,   076, 002, 000, true,  false, true,  false, ASemantics.A,     "FM",    false),
        new InstructionInfo(Mode.EITHER,   076, 003, 000, true,  false, true,  false, ASemantics.A,     "FD",    false),
        new InstructionInfo(Mode.EITHER,   076, 004, 000, true,  false, true,  false, ASemantics.A,     "LUF",   false),
        new InstructionInfo(Mode.EITHER,   076, 005, 000, true,  false, true,  false, ASemantics.A,     "LCF",   false),
        new InstructionInfo(Mode.EITHER,   076, 006, 000, true,  false, true,  false, ASemantics.A,     "MCDU",  false),
        new InstructionInfo(Mode.EITHER,   076, 007, 000, true,  false, true,  false, ASemantics.A,     "CDU",   false),
        new InstructionInfo(Mode.EITHER,   076, 010, 000, true,  false, true,  false, ASemantics.A,     "DFA",   false),
        new InstructionInfo(Mode.EITHER,   076, 011, 000, true,  false, true,  false, ASemantics.A,     "DFAN",  false),
        new InstructionInfo(Mode.EITHER,   076, 012, 000, true,  false, true,  false, ASemantics.A,     "DFM",   false),
        new InstructionInfo(Mode.EITHER,   076, 013, 000, true,  false, true,  false, ASemantics.A,     "DFD",   false),
        new InstructionInfo(Mode.EITHER,   076, 014, 000, true,  false, true,  false, ASemantics.A,     "DFU",   false),
        new InstructionInfo(Mode.EITHER,   076, 015, 000, true,  false, true,  false, ASemantics.A,     "DLCF",  false),
        new InstructionInfo(Mode.EITHER,   076, 016, 000, true,  false, true,  false, ASemantics.A,     "FEL",   false),
        new InstructionInfo(Mode.EITHER,   076, 017, 000, true,  false, true,  false, ASemantics.A,     "FCL",   false),
    };

    //  j-field values
    public static final int W = 0;
    public static final int H2 = 1;
    public static final int H1 = 2;
    public static final int XH2 = 3;
    public static final int XH1 = 4;
    public static final int Q2 = 4;
    public static final int T3 = 5;
    public static final int Q4 = 5;
    public static final int T2 = 6;
    public static final int Q3 = 6;
    public static final int T1 = 7;
    public static final int Q1 = 7;
    public static final int S6 = 8;
    public static final int S5 = 9;
    public static final int S4 = 10;
    public static final int S3 = 11;
    public static final int S2 = 12;
    public static final int S1 = 13;
    public static final int U = 14;
    public static final int XU = 15;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors and such
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * General constructor
     */
    public InstructionWord(
    ) {
    }

    /**
     * Constructor which takes a ones-complement value
     * <p>
     * @param value
     */
    public InstructionWord(
        final long value
    ) {
        super(value);
    }

    /**
     * Constructor which takes component FJAXHIU fields
     * <p>
     * @param f
     * @param j
     * @param a
     * @param x
     * @param h
     * @param i
     * @param u
     */
    public InstructionWord(
        final long f,
        final long j,
        final long a,
        final long x,
        final long h,
        final long i,
        final long u
    ) {
        super(((f & 077) << 30)
                | ((j & 017) << 26)
                | ((a & 017) << 22)
                | ((x & 017) << 18)
                | ((h & 01) << 17)
                | ((i & 01) << 16)
                | (u & 0177777));
    }

    /**
     * Constructor which takes component FJAXU fields, where U include the H and I bits
     * <p>
     * @param f
     * @param j
     * @param a
     * @param x
     * @param u
     */
    public InstructionWord(
        final long f,
        final long j,
        final long a,
        final long x,
        final long u
    ) {
        super(((f & 077) << 30)
                | ((j & 017) << 26)
                | ((a & 017) << 22)
                | ((x & 017) << 18)
                | (u & 0777777));
    }

    /**
     * Constructor which takes component FJAXHIBD fields
     * <p>
     * @param f
     * @param j
     * @param a
     * @param x
     * @param h
     * @param i
     * @param b
     * @param d
     */
    public InstructionWord(
        final long f,
        final long j,
        final long a,
        final long x,
        final long h,
        final long i,
        final long b,
        final long d
    ) {
        super(((f & 077) << 30)
                | ((j & 017) << 26)
                | ((a & 017) << 22)
                | ((x & 017) << 18)
                | ((h & 01) << 17)
                | ((i & 01) << 16)
                | ((b & 017) << 12)
                | (d & 07777));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Non-static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Convenience method
     */
    public void clear(
    ) {
        setW(0);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getF(
    ) {
        return getF(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getJ(
    ) {
        return getJ(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getA(
    ) {
        return getA(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getX(
    ) {
        return getX(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getH(
    ) {
        return getH(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getI(
    ) {
        return getI(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getU(
    ) {
        return getU(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getHIU(
    ) {
        return getHIU(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getB(
    ) {
        return getB(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getIB(
    ) {
        return getIB(_value);
    }

    /**
     * Partial-word extractor
     * <p>
     * @return
     */
    public long getD(
    ) {
        return getD(_value);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setF(
        final long newValue
    ) {
        _value = setF(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setJ(
        final long newValue
    ) {
        _value = setJ(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setA(
        final long newValue
    ) {
        _value = setA(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setX(
        final long newValue
    ) {
        _value = setX(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setH(
        final long newValue
    ) {
        _value = setH(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setI(
        final long newValue
    ) {
        _value = setI(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setU(
        final long newValue
    ) {
        _value = setU(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setHIU(
        final long newValue
    ) {
        _value = setHIU(_value, newValue);
    }

    public void setXHIU(
        final long newValue
    ) {
        _value = setXHIU(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setB(
        final long newValue
    ) {
        _value = setB(_value, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param newValue value to be injected into a partial-word of this object's value
     */
    public void setD(
        final long newValue
    ) {
        _value = setD(_value, newValue);
    }

    //  Interpretive methods -------------------------------------------------------------------------------------------------------

    public String getMnemonic(
        final boolean extendedMode
    ) {
        return getMnemonic(_value, extendedMode);
    }

    /**
     * Interprets this instruction word into a displayable string
     * <p>
     * @param extendedMode assume extended mode - false implies basic mode
     * @param execModeRegistersFlag true to display exec registers instead of user registers for a and x fields
     * <p>
     * @return
     */
    public String interpret(
        final boolean extendedMode,
        final boolean execModeRegistersFlag
    ) {
        return interpret(_value, extendedMode, execModeRegistersFlag);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getF(
        final long value
    ) {
        return (value & MASK_F) >> 30;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getJ(
        final long value
    ) {
        return (value & MASK_J) >> 26;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getA(
        final long value
    ) {
        return (value & MASK_A) >> 22;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getX(
        final long value
    ) {
        return (value & MASK_X) >> 18;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getH(
        final long value
    ) {
        return (value & MASK_H) >> 17;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getI(
        final long value
    ) {
        return (value & MASK_I) >> 16;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getU(
        final long value
    ) {
        return (value & MASK_U);
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getHIU(
        final long value
    ) {
        return (value & MASK_HIU);
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getB(
        final long value
    ) {
        return (value & MASK_B) >> 12;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getIB(
        final long value
    ) {
        return (value & MASK_IB) >> 12;
    }

    /**
     * Partial-word extractor
     * <p>
     * @param value
     * <p>
     * @return
     */
    public static long getD(
        final long value
    ) {
        return (value & MASK_D);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setF(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_F) | ((newValue & 077) << 30);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setJ(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_J) | ((newValue & 017) << 26);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setA(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_A) | ((newValue & 017) << 22);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setX(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_X) | ((newValue & 017) << 18);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setH(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_H) | ((newValue & 01) << 17);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setI(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_I) | ((newValue & 01) << 16);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setU(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_U) | (newValue & 0177777);
    }

    /**
     * Replace the X, H, I, and U fields of an initial value with those of the new value, returning the result
     * <p>
     * @param initialValue
     * @param newValue
     * <p>
     * @return
     */
    public static long setXHIU(
        final long initialValue,
        final long newValue
    ) {
        long mask = MASK_X | MASK_H | MASK_I | MASK_U;
        long notMask = MASK_NOT_X & MASK_NOT_H & MASK_NOT_I & MASK_NOT_U;
        return (initialValue & notMask) | (newValue & mask);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setHIU(
        final long initialValue,
        final long newValue
    ) {
        return setH2(initialValue, newValue);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setB(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_B) | ((newValue & 017) << 12);
    }

    /**
     * Partial-word injector
     * <p>
     * @param initialValue 36-bit value into which the new partial-word value is to be injected
     * @param newValue the value to be injected into the partial-word
     * <p>
     * @return the result of the injection operation
     */
    public static long setD(
        final long initialValue,
        final long newValue
    ) {
        return (initialValue & MASK_NOT_D) | (newValue & 07777);
    }


    //  Interpretive methods -------------------------------------------------------------------------------------------------------

    public static String getMnemonic(
        final long value,
        final boolean extendedMode
    ) {
        long f = getF(value);
        long j = getJ(value);
        long a = getA(value);

        for (InstructionInfo iInfo : INSTRUCTION_INFOS) {
            if (iInfo._fField == f) {
                if ( (iInfo._mode == Mode.EITHER)
                    || ( (iInfo._mode == Mode.EXTENDED ) && extendedMode )
                    || ( (iInfo._mode == Mode.BASIC ) && !extendedMode ) ) {
                    if (iInfo._jFlag && (iInfo._jField != j)) {
                        continue;
                    }

                    if (iInfo._aFlag && (iInfo._aField != a)) {
                        continue;
                    }

                    return iInfo._mnemonic;
                }
            }
        }

        return "";
    }

    /**
     * Interprets a normal instruction word to a string of displayable text
     * By 'normal', we mean the f-field defines an operation which is modified by the j, a, x, h, i, and u (or b and d) fields.
     * <p>
     * @param instruction 36-bit word being interpreted
     * @param mnemonic instruction mnemonic
     * @param extendedMode true to consider the b and d fields, false to consider the u field
     * @param aSemantics indicates the nature of the a field
     * @param jFieldFlag true to interpret the j-field, else false
     * @param grsFlag true to convert u-fields less than 0200 to GRS register designations
     *                  (if appropriate; not for j=U or XU, and not for EM b > 0)
     * @param forceBMSemantics true to force EM instruction to use u-field instead of b and d fields
     * @param execModeRegistersFlag true to display Exec registers instead of User registers for a and x fields
     * <p>
     * @return
     */
    private static String interpretNormal(
        final long instruction,
        final String mnemonic,
        final boolean extendedMode,
        final ASemantics aSemantics,
        final boolean jFieldFlag,
        final boolean grsFlag,
        final boolean forceBMSemantics,
        final boolean execModeRegistersFlag
    ) {
        StringBuilder builder = new StringBuilder();

        // setup
        long j = getJ(instruction);
        long a = getA(instruction);
        long x = getX(instruction);
        long h = getH(instruction);
        long i = getI(instruction);
        long u = getU(instruction);
        long hiu = getHIU(instruction);
        long b = getB(instruction);
        long d = getD(instruction);

        //  are we going to convert the u (or d) field to a GRS register?
        //  If this is normal grs conversion, but ,u or ,xu, no.
        //  If we are in extended mode and not using B0, no.
        //  If we are indexing at all, no.
        //  In each of these cases, it is exceedingly unlikely that the coder
        //  meant to reference the GRS.  Possible, but unlikely.
        // ???? Apparently there is a bug here, at least in some cases... we're not always seeing the GRS name when we should
        boolean grsConvert = grsFlag;
        if ((grsConvert && (j >= 016)) || (extendedMode && (b > 0)) || (x > 0)) {
            grsConvert = false;
        }

        builder.append(mnemonic);

        // develop j field string, with leading ',' if field is not blank
        if (jFieldFlag && (j > 0)) {
            builder.append(",");
            builder.append(J_FIELD_NAMES[(int)j]);
        }

        builder.append(" ");
        while (builder.length() < 12) {
            builder.append(" ");
        }

        // a field (generally a register reference)
        switch (aSemantics) {
            case A:
                if (execModeRegistersFlag) {
                    builder.append("E");
                }
                builder.append(String.format("A%d,", a));
                break;

            case B:
                builder.append(String.format("B%d,", a));
                break;

            case B_EXEC:
                builder.append(String.format("B%d,", a + 16));
                break;

            case R:
                if (execModeRegistersFlag) {
                    builder.append("E");
                }
                builder.append(String.format("R%d,", a));
                break;

            case X:
                if (execModeRegistersFlag) {
                    builder.append("E");
                }
                builder.append(String.format("X%d,", a));
                break;

            case NONE:
                break;
        }

        // u or d field - various possibilities here
        boolean immediate = jFieldFlag && (j >= 016);
        if (grsConvert && (u < 0200)) {
            // Use the GRS register name for the u field.
            builder.append(GeneralRegisterSet.NAMES[(int)u]);
        } else if (extendedMode && !forceBMSemantics && !immediate) {
            // Extended mode, BM not forced (i.e., not a jump or similar), and not immediate...
            // Decode the d field
            builder.append(String.format("0%o", d));
        } else if (immediate) {
            if (x == 0) {
                builder.append(String.format("0%o", hiu));
            } else {
                builder.append(String.format("0%o", u));
            }
        } else {
            if (i != 0) {
                builder.append("*");
            }
            builder.append(String.format("0%o", u));
        }

        // x field
        if (x > 0) {
            builder.append(",");
            if (h > 0) {
                builder.append("*");
            }
            if (execModeRegistersFlag) {
                builder.append("E");
            }
            builder.append(String.format("X%d", x));
        }

        //  Doing EM, BM not forced, and no GRS conversion, and no ,U or ,XU, we show B register
        if (extendedMode && !forceBMSemantics && !grsConvert && !immediate) {
            //  ...if there wasn't an x, we need an extra comma
            if (x == 0) {
                builder.append(",");
            }

            int effective_b = (int)b;
            if (i > 0) {
                effective_b += 16;
            }

            builder.append(String.format(",B%d", effective_b));
        }

        return builder.toString();
    }

    /**
     * Interprets an instruction word into a displayable string
     * <p>
     * @param instruction 36-bit instruction to be interpreted
     * @param extendedMode assume extended mode - false implies basic mode
     * @param execModeRegistersFlag true to display exec registers instead of user registers for a and x fields
     * <p>
     * @return
     */
    public static String interpret(
        final long instruction,
        final boolean extendedMode,
        final boolean execModeRegistersFlag
    ) {
        long f = getF(instruction);
        long j = getJ(instruction);
        long a = getA(instruction);

        for (InstructionInfo iInfo : INSTRUCTION_INFOS) {
            if (iInfo._fField == f) {
                if ( (iInfo._mode == Mode.EITHER)
                        || ((iInfo._mode == Mode.EXTENDED) && extendedMode)
                        || ((iInfo._mode == Mode.BASIC) && !extendedMode) ) {
                    if (iInfo._jFlag && (iInfo._jField != j)) {
                        continue;
                    }

                    if (iInfo._aFlag && (iInfo._aField != a)) {
                        continue;
                    }

                    //  If there's a handler, use that.  Othewise, use the normal interpreter
                    if (iInfo._handler != null) {
                        return iInfo._handler.interpret(instruction);
                    } else {
                        boolean jField = !iInfo._jFlag;
                        return interpretNormal( instruction,
                                                iInfo._mnemonic,
                                                extendedMode,
                                                iInfo._aSemantics,
                                                jField,
                                                iInfo._grsFlag,
                                                iInfo._useBMSemantics,
                                                execModeRegistersFlag );
                    }
                }
            }
        }

        // Couldn't find a way to interpret the instruction - just use octal.
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%02o %02o %02o %02o %1o %1o ",
                                     getF(instruction),
                                     getJ(instruction),
                                     getA(instruction),
                                     getX(instruction),
                                     getH(instruction),
                                     getI(instruction)));

        if (extendedMode) {
            builder.append(String.format("%02o %04o", getB(instruction), getD(instruction)));
        } else {
            builder.append(String.format("%06o", getU(instruction)));
        }

        return builder.toString();
    }

    //  Other stuff ----------------------------------------------------------------------------------------------------------------

    /**
     * Retrieves an InstructionInfo object for the given combination of mnemonic and mode
     * <p>
     * @param mnemonic
     * @param mode
     * <p>
     * @return InstructionInfo object if found, else null
     * <p>
     * @throws NotFoundException if the mnemonic/mode combination does not exist
     */
    public static InstructionInfo getInstructionInfo(
        final String mnemonic,
        final Mode mode
    ) throws NotFoundException {
        for (InstructionInfo info : INSTRUCTION_INFOS ) {
            if ((info._mode == mode) || (info._mode == Mode.EITHER)) {
                if (mnemonic.equalsIgnoreCase(info._mnemonic)) {
                    return info;
                }
            }
        }

        throw new NotFoundException(mnemonic);
    }

    /**
     * Translates a j-field spec (such as "H1") to it's j-field integer value
     * <p>
     * @param fieldSpec
     * <p>
     * @return
     * <p>
     * @throws NotFoundException if the fieldSpec is not a valid spec
     */
    public static int getJFieldValue(
        final String fieldSpec
    ) throws NotFoundException {
        for (int jx = 0; jx < J_FIELD_NAMES.length; ++jx) {
            String[] split = J_FIELD_NAMES[jx].split("/");
            for (String splitSpec : split) {
                if (splitSpec.equalsIgnoreCase(fieldSpec)) {
                    return jx;
                }
            }
        }

        throw new NotFoundException(fieldSpec);
    }
}

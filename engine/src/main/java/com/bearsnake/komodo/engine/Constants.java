package com.bearsnake.komodo.engine;

public class Constants {

    public static int BREAKPOINT_FETCH = 1;
    public static int BREAKPOINT_READ = 2;
    public static int BREAKPOINT_WRITE = 4;

    //  Indices of the defined registers indicating where they are found in the GRS.
    //  X0-X15, A0-A15, and R0-R15 are user registers.
    //  EX0-EX15, EA0-EA15, and ER0-ER15 are EXEC registers.
    //  Note the overlap in both user and EXEC registers, of X12-X15 and A0-A3.
    public static int GRS_X0 = 0_000;
    public static int GRS_X1 = 0_001;
    public static int GRS_X2 = 0_002;
    public static int GRS_X3 = 0_003;
    public static int GRS_X4 = 0_004;
    public static int GRS_X5 = 0_005;
    public static int GRS_X6 = 0_006;
    public static int GRS_X7 = 0_007;
    public static int GRS_X8 = 0_010;
    public static int GRS_X9 = 0_011;
    public static int GRS_X10 = 0_012;
    public static int GRS_X11 = 0_013;
    public static int GRS_X12 = 0_014;
    public static int GRS_X13 = 0_015;
    public static int GRS_X14 = 0_016;
    public static int GRS_X15 = 0_017;
    public static int GRS_A0 = 0_014;
    public static int GRS_A1 = 0_015;
    public static int GRS_A2 = 0_016;
    public static int GRS_A3 = 0_017;
    public static int GRS_A4 = 0_020;
    public static int GRS_A5 = 0_021;
    public static int GRS_A6 = 0_022;
    public static int GRS_A7 = 0_023;
    public static int GRS_A8 = 0_024;
    public static int GRS_A9 = 0_025;
    public static int GRS_A10 = 0_026;
    public static int GRS_A11 = 0_027;
    public static int GRS_A12 = 0_030;
    public static int GRS_A13 = 0_031;
    public static int GRS_A14 = 0_032;
    public static int GRS_A15 = 0_033;
    public static int GRS_R0 = 0_100;
    public static int GRS_R1 = 0_101;
    public static int GRS_R2 = 0_102;
    public static int GRS_R3 = 0_103;
    public static int GRS_R4 = 0_104;
    public static int GRS_R5 = 0_105;
    public static int GRS_R6 = 0_106;
    public static int GRS_R7 = 0_107;
    public static int GRS_R8 = 0_110;
    public static int GRS_R9 = 0_111;
    public static int GRS_R10 = 0_112;
    public static int GRS_R11 = 0_113;
    public static int GRS_R12 = 0_114;
    public static int GRS_R13 = 0_115;
    public static int GRS_R14 = 0_116;
    public static int GRS_R15 = 0_117;
    public static int GRS_ER0 = 0_120;
    public static int GRS_ER1 = 0_121;
    public static int GRS_ER2 = 0_122;
    public static int GRS_ER3 = 0_123;
    public static int GRS_ER4 = 0_124;
    public static int GRS_ER5 = 0_125;
    public static int GRS_ER6 = 0_126;
    public static int GRS_ER7 = 0_127;
    public static int GRS_ER8 = 0_130;
    public static int GRS_ER9 = 0_131;
    public static int GRS_ER10 = 0_132;
    public static int GRS_ER11 = 0_133;
    public static int GRS_ER12 = 0_134;
    public static int GRS_ER13 = 0_135;
    public static int GRS_ER14 = 0_136;
    public static int GRS_ER15 = 0_137;
    public static int GRS_EX0 = 0_140;
    public static int GRS_EX1 = 0_141;
    public static int GRS_EX2 = 0_142;
    public static int GRS_EX3 = 0_143;
    public static int GRS_EX4 = 0_144;
    public static int GRS_EX5 = 0_145;
    public static int GRS_EX6 = 0_146;
    public static int GRS_EX7 = 0_147;
    public static int GRS_EX8 = 0_150;
    public static int GRS_EX9 = 0_151;
    public static int GRS_EX10 = 0_152;
    public static int GRS_EX11 = 0_153;
    public static int GRS_EX12 = 0_154;
    public static int GRS_EX13 = 0_155;
    public static int GRS_EX14 = 0_156;
    public static int GRS_EX15 = 0_157;
    public static int GRS_EA0 = 0_154;
    public static int GRS_EA1 = 0_155;
    public static int GRS_EA2 = 0_156;
    public static int GRS_EA3 = 0_157;
    public static int GRS_EA4 = 0_160;
    public static int GRS_EA5 = 0_161;
    public static int GRS_EA6 = 0_162;
    public static int GRS_EA7 = 0_163;
    public static int GRS_EA8 = 0_164;
    public static int GRS_EA9 = 0_165;
    public static int GRS_EA10 = 0_166;
    public static int GRS_EA11 = 0_167;
    public static int GRS_EA12 = 0_170;
    public static int GRS_EA13 = 0_171;
    public static int GRS_EA14 = 0_172;
    public static int GRS_EA15 = 0_173;

    //  User-displayable names of the various registers, in register index order
    public static final String[] GRS_REGISTER_NAMES = {
        "X0",    "X1",    "X2",    "X3",    "X4",    "X5",    "X6",    "X7",
        "X8",    "X9",    "X10",   "X11",   "A0",    "A1",    "A2",    "A3",
        "A4",    "A5",    "A6",    "A7",    "A8",    "A9",    "A10",   "A11",
        "A12",   "A13",   "A14",   "A15",   "UR0",   "UR1",   "UR2",   "UR3",
        "040",   "041",   "042",   "043",   "044",   "045",   "046",   "047",
        "050",   "051",   "052",   "053",   "054",   "055",   "056",   "057",
        "060",   "061",   "062",   "063",   "064",   "065",   "066",   "067",
        "070",   "071",   "072",   "073",   "074",   "075",   "076",   "077",
        "R0",    "R1",    "R2",    "R3",    "R4",    "R5",    "R6",    "R7",
        "R8",    "R9",    "R10",   "R11",   "R12",   "R13",   "R14",   "R15",
        "ER0",   "ER1",   "ER2",   "ER3",   "ER4",   "ER5",   "ER6",   "ER7",
        "ER8",   "ER9",   "ER10",  "ER11",  "ER12",  "ER13",  "ER14",  "ER15",
        "EX0",   "EX1",   "EX2",   "EX3",   "EX4",   "EX5",   "EX6",   "EX7",
        "EX8",   "EX9",   "EX10",  "EX11",  "EA0",   "EA1",   "EA2",   "EA3",
        "EA4",   "EA5",   "EA6",   "EA7",   "EA8",   "EA9",   "EA10",  "EA11",
        "EA12",  "EA13",  "EA14",  "EA15",  "0174",  "0175",  "0176",  "0177",
        };
}

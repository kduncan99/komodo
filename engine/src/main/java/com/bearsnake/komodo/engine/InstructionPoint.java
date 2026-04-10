package com.bearsnake.komodo.engine;

public enum InstructionPoint {
    BETWEEN_INSTRUCTIONS(0),
    RESOLVING_ADDRESS(1),
    MID_INSTRUCTION(2);

    private final int code;

    InstructionPoint(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static InstructionPoint fromCode(int code) {
        for (InstructionPoint ip : values()) {
            if (ip.getCode() == code) {
                return ip;
            }
        }
        throw new IllegalArgumentException("Invalid instruction point code: " + code);
    }
}

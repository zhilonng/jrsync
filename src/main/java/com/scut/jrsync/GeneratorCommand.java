package com.scut.jrsync;

public enum GeneratorCommand {
    STEP_DOWN(1),   //step_down = 1
    FILE(2),      // file = 2
    STEP_UP(3);   //step_up = 3

    private final byte code;

    GeneratorCommand(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

}

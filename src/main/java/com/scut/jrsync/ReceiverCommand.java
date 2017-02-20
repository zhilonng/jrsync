/*

 */
package com.scut.jrsync;

public enum ReceiverCommand {
    FILE_START(1), //file_start = 1
    RAW_DATA(2),   // raw_data = 2
    COPY_BLOCK(3),  // copy_block = 3
    FILE_END(4),    // file_end = 4
    ENUMERATOR_DONE(5);  // enumerator_done = 5

    private final byte code;

    ReceiverCommand(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

}

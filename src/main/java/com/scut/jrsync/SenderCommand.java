/*

 */
package com.scut.jrsync;

public enum SenderCommand {
    FILE_START(1),   // file_start = 1
    HASH(2),      // hash = 2
    FILE_END(3),   // file_end =3
    ENUMERATOR_DONE(4),  //enumerator_done =4
    EVERYTHING_OK(5);    // everything_ok =5

    private final byte code;

    SenderCommand(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

}

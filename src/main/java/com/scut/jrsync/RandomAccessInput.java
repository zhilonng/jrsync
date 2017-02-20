/*


 */
package com.scut.jrsync;

import java.io.IOException;
import java.io.OutputStream;
//读文件接口
public interface RandomAccessInput {

	//拷贝
    public abstract void copyTo(OutputStream target, long offset, short length) throws IOException;

    public abstract void close() throws IOException; 

}

package com.scut.jrsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 */
public interface FilePath {

    public abstract String getName();  //文件名	

    public abstract FilePath getParent();  //上级路径

    public abstract Iterable<? extends FilePath> getChildrenSorted() throws IOException;

    public abstract FilePath getChild(String string);  //子路径

    public abstract boolean hasChild(String name);  //是否有子路径

    public abstract boolean isDirectory();   //是否目录

    public abstract long getSize();   //文件大小

    public abstract long getLastChange();  //得到最后一次变化

    public abstract void setLastChange(long lastChange) throws IOException;  //记录最后一次发生变化

    public abstract FilePath createSubdirectory(String name) throws IOException;  //创建子目录

    public abstract void delete() throws IOException;  //删除

    public abstract void renameTo(String substring) throws IOException;  //重命名

    public abstract InputStream openInputStream() throws IOException;   //输入

    public abstract OutputStream openOutputStream() throws IOException;  //输出

    public abstract RandomAccessInput openRandomAccessInput() throws IOException;  //打开访问

}

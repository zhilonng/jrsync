package com.scut.jrsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 */
public interface FilePath {

    public abstract String getName();  //�ļ���	

    public abstract FilePath getParent();  //�ϼ�·��

    public abstract Iterable<? extends FilePath> getChildrenSorted() throws IOException;

    public abstract FilePath getChild(String string);  //��·��

    public abstract boolean hasChild(String name);  //�Ƿ�����·��

    public abstract boolean isDirectory();   //�Ƿ�Ŀ¼

    public abstract long getSize();   //�ļ���С

    public abstract long getLastChange();  //�õ����һ�α仯

    public abstract void setLastChange(long lastChange) throws IOException;  //��¼���һ�η����仯

    public abstract FilePath createSubdirectory(String name) throws IOException;  //������Ŀ¼

    public abstract void delete() throws IOException;  //ɾ��

    public abstract void renameTo(String substring) throws IOException;  //������

    public abstract InputStream openInputStream() throws IOException;   //����

    public abstract OutputStream openOutputStream() throws IOException;  //���

    public abstract RandomAccessInput openRandomAccessInput() throws IOException;  //�򿪷���

}

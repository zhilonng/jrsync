package com.scut.jrsync;

import java.io.IOException;

interface ExplicitMoveIterator<T> {
    /**
     */
    public abstract T get();  //获得当前元素

    /**
     */
    public abstract void move() throws IOException;  //转移到下个元素

    /**
     */
    public abstract boolean hasCurrent();  //当前有数据流返回true
}
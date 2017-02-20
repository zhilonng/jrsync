package com.scut.jrsync;

/**
 * 
 */
public class FastConcurrentList<T> {

    private int length;
    private volatile Object[] content;

    public FastConcurrentList() {
        this.length = 0;
        this.content = new Object[128];
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        return (T) this.content[index];
    }

    public int add(T p) {
        final int index = this.length++;
        if (index >= this.content.length) {
            final Object[] newContent = new Object[this.content.length * 2];
            System.arraycopy(this.content, 0, newContent, 0, index);
            this.content = newContent;
        }
        this.content[index] = p;
        return index;
    }

}

package com.scut.jrsync;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 */ 
public class Enumerator implements Runnable {

    private final FilePath localDir;
    private final GeneratorCommandWriter writer;
    private final FastConcurrentList<FilePath> filePaths;
    private final ExceptionBuffer exc;

    public Enumerator(FilePath localDir, OutputStream target,
            FastConcurrentList<FilePath> filePathBuffer, ExceptionBuffer exc) {
        this.localDir = localDir;
        this.writer = new GeneratorCommandWriter(new DataOutputStream(target));
        this.filePaths = filePathBuffer;
        this.exc = exc;
    }

    @Override
    public void run() {
        try {
            final int count = this.sendDirRecursive(this.localDir);
            System.out.println("Enumerated " + count + " files.");   //扫描有总的文件数
        } catch (final IOException e) {
            this.exc.addThrowable(e);
        } finally {
            this.writer.close();
        }
    }

    private int sendDirRecursive(FilePath dir) throws IOException {
        int count = 0;
        this.writer.writeStepDown(dir.getName());
        for (final FilePath child : dir.getChildrenSorted()) {
            if (child.isDirectory()) {
                count += this.sendDirRecursive(child);
            } else {
                this.filePaths.add(child);
                this.writer.writeFile(child.getName(), child.getSize(), child.getLastChange());
                count++;
            }
        }
        this.writer.writeStepUp();
        return count;
    }

}

/*

 */
package com.scut.jrsync;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;

/**
 */
public class Receiver implements Runnable {

    private static final String TMP_SUFFIX = ".jstmp";

    private final DataInputStream input;
    private final FastConcurrentList<TargetFileInfo> filePaths;
    private final BlockingQueue<Integer> toResend;  //文件块

    private boolean enumeratorDone;

    private int openResends;

    public Receiver(InputStream source, FastConcurrentList<TargetFileInfo> b, BlockingQueue<Integer> toResend) {
        this.input = new DataInputStream(source);
        this.filePaths = b;
        this.toResend = toResend;
    }

    @Override
    public void run() {
        try {
            int index = -2;
            FilePath tmpFile = null;
            MD5OutputStream tmpFileStream = null;
            RandomAccessInput templateFile = null;

            while (!Thread.interrupted()) {
                final int command = this.input.read();
                if (command < 0) {
                    break;
                }
               //file_start = 1
                if (command == ReceiverCommand.FILE_START.getCode()) {
                	//生成一个新的文件的开头=>临时文件
                    index = this.input.readInt();
                    tmpFile = this.createTempFileFor(index);   //创建临时文件
                    tmpFileStream = new MD5OutputStream(tmpFile.openOutputStream());
                    //raw_data = 2
                } else if (command == ReceiverCommand.RAW_DATA.getCode()) {
                	//写入临时文件=>
                    final int length = this.input.readInt();
                    StreamHelper.copy(this.input, tmpFileStream, length);
                    //copy_block = 3
                } else if (command == ReceiverCommand.COPY_BLOCK.getCode()) {
                    final long offset = this.input.readLong();
                    final short length = this.input.readShort();
                    if (templateFile == null) {
                        templateFile = this.filePaths.get(index).getFilePath().openRandomAccessInput();
                    }
                    templateFile.copyTo(tmpFileStream, offset, length);
                } else if (command == ReceiverCommand.FILE_END.getCode()) {  // file_end = 4
                    if (templateFile != null) {
                        templateFile.close();
                        templateFile = null;
                    }

                    final byte[] expectedDigest = new byte[MD5.DIGEST_LENGTH];
                    this.input.readFully(expectedDigest);

                    if (Arrays.equals(expectedDigest, tmpFileStream.getDigest())) {
                        tmpFileStream.close();
                        this.renameToRealName(index, tmpFile);
                        if (this.enumeratorDone) {
                            this.openResends--;
                            assert this.openResends >= 0;
                            if (this.openResends == 0) {
                                break;
                            }
                        }
                    } else {
                        this.toResend.add(index);
                        if (!this.enumeratorDone) {
                            this.openResends++;
                        }
                    }
                } else if (command == ReceiverCommand.ENUMERATOR_DONE.getCode()) {  // enumerator_done = 5
                    this.enumeratorDone = true;
                    if (this.openResends == 0) {
                        break;
                    }
                } else {
                    throw new IOException("unknown command " + command);
                }
            }

            this.toResend.add(-1);
        } catch (final Exception e) {
            Logger.LOGGER.log(Level.SEVERE, "exception in receiver", e);
            try {
                this.input.close();
            } catch (final IOException e1) {
                Logger.LOGGER.log(Level.WARNING, "exception while closing", e1);
            }
        }
    }

    private FilePath createTempFileFor(int index) {
        final FilePath orig = this.filePaths.get(index).getFilePath();
        return orig.getParent().getChild(orig.getName() + TMP_SUFFIX);
    }

    private void renameToRealName(int index, FilePath tmpFile) throws IOException {  //将临时文件命名成真正文件名
        tmpFile.setLastChange(this.filePaths.get(index).getSourceChangeTime());
        tmpFile.renameTo(this.filePaths.get(index).getFilePath().getName());
    }

}

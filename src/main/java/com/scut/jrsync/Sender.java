/*

 */
package com.scut.jrsync;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ���ҳ������ļ��鲢��diff�ļ����receive ���ղ�����
 */
public class Sender implements Runnable {

    private static final int RAW_DATA_BUFFER_LIMIT = 8096;

    private final DataInputStream source;
    private final FastConcurrentList<FilePath> filePaths;
    private final ReceiverCommandWriter writer;  //������
    private final ExceptionBuffer exc;

    private int count;   //��¼��Ҫͬ���ļ�����Ŀ

    public Sender(InputStream source, FastConcurrentList<FilePath> filePaths, OutputStream target, ExceptionBuffer exc) {
        this.source = new DataInputStream(source);
        this.filePaths = filePaths;
        this.writer = new ReceiverCommandWriter(new DataOutputStream(target));
        this.exc = exc;
    }

    private static class BlockInfo {  //Block��С��Ϣ

        private final byte[] strongHash;  //ǿ��ϣ
        private final int blockNumber;    //��������Ŀ

        public BlockInfo(byte[] strongHash, int blockNumber) {
            this.strongHash = strongHash;
            this.blockNumber = blockNumber;
        }

        public boolean matches(byte[] currentMD4) {  //�Ƿ�ƥ��
            return Arrays.equals(this.strongHash, currentMD4);
        }

        public void writeCopyCommand(ReceiverCommandWriter writer, int blockSize) throws IOException {
            writer.writeCopyBlock(this.blockNumber * ((long) blockSize), (short) blockSize);
        }

    }

    @Override
    public void run() {
        try {
            boolean okReceived = false;
            int index = -1;
            int strongHashSize = -1;
            int blockSize = -1;
            final Map<Integer, List<BlockInfo>> hashes = new HashMap<Integer, List<BlockInfo>>();
            int blockNumber = -1;

            while (!Thread.interrupted()) {
                final int command = this.source.read();
                if (command < 0) {
                    if (!okReceived) {
                        throw new IOException("Error while copying! Check daemon log for details.");
                    }
                    break;
                }
                if (command == SenderCommand.FILE_START.getCode()) {  // file_start = 1
                    index = this.source.readInt();
                    strongHashSize = this.source.readByte();
                    blockSize = this.source.readShort();   //blocksize��С
                    blockNumber = 0;
                    hashes.clear();
                } else if (command == SenderCommand.HASH.getCode()) {  //hash = 2
                    final Integer rollingHash = this.source.readInt();
                    List<BlockInfo> blocksForHash = hashes.get(rollingHash);
                    if (blocksForHash == null) {
                        blocksForHash = new ArrayList<BlockInfo>(1);
                        hashes.put(rollingHash, blocksForHash);
                    }
                    final byte[] strongHash = new byte[strongHashSize];
                    this.source.readFully(strongHash);
                    blocksForHash.add(new BlockInfo(strongHash, blockNumber));
                    blockNumber++;  //����Ŀ
                } else if (command == SenderCommand.FILE_END.getCode()) {   // file_end =3
                    if (hashes.isEmpty()) {
                        this.copyFileFully(index);  //ȫ������
                    } else {
                        this.copyFileUsingDiff(index, hashes, blockSize, strongHashSize); //���Ʋ��첿��
                    }
                    this.count++;  //��Ҫͬ�����ļ�+1
                } else if (command == SenderCommand.ENUMERATOR_DONE.getCode()) {//enumerator_done =4
                    this.writer.writeEnumeratorDone();
                } else if (command == SenderCommand.EVERYTHING_OK.getCode()) {  //everything_ok =5
                    okReceived = true;
                } else {
                    throw new IOException("unknown command " + command);
                }
            }

            System.out.println("Had to send " + this.count + " files");
        } catch (final IOException e) {
            this.exc.addThrowable(e);
        } finally {
            this.writer.close();
        }
    }

    private void copyFileUsingDiff(int index, Map<Integer, List<BlockInfo>> hashes, int blockSize,
            int strongHashSize) throws IOException {
        final FilePath file = this.filePaths.get(index); //�����������ļ�
        final InputStream fileStream = file.openInputStream();  //�򿪶��ļ���
        try {
            final MD5InputStream md4stream = new MD5InputStream(fileStream);  //MD4������
            final BufferedInputStream bufferedStream = new BufferedInputStream(md4stream);
            this.writer.writeFileStart(index);  //������д��

            final Checksum32 rollingChecksum = new Checksum32();  //������
            final ByteArrayOutputStream rawDataBuffer = new ByteArrayOutputStream();
            final byte[] block = new byte[blockSize];
            StreamHelper.readFully(bufferedStream, block);
            rollingChecksum.check(block, 0, block.length);  //����������

            outerLoop: while (true) {
                final int currentChecksum = rollingChecksum.getValue();  //�õ���������ֵ
                final List<BlockInfo> blocksWithChecksum = hashes.get(currentChecksum); //������������
                if (blocksWithChecksum != null) {
                    rollingChecksum.copyBlock(block);   //������
                    final byte[] currentMD4 = MD5.determineFor(block, strongHashSize);  //�õ���ǰ��MD4ֵ
                    for (final BlockInfo b : blocksWithChecksum) {
                        if (b.matches(currentMD4)) {  //���ǿУ��ƥ��
                            if (rawDataBuffer.size() > 0) {
                                this.flushRawData(rawDataBuffer);
                            }
                            b.writeCopyCommand(this.writer, blockSize);
                            final int readCount = StreamHelper.readFully(bufferedStream, block);
                            if (readCount < block.length) {
                                //EOF
                                rawDataBuffer.write(block, 0, readCount);
                                break outerLoop;
                            } else {
                                rollingChecksum.check(block, 0, block.length);
                                continue outerLoop;
                            }
                        }
                    }
                }

                //���ִ�е������ô�Ƿ��ҵ�ƥ��
                final int nextByte = bufferedStream.read();  //��ȡ�¸��ֽ�
                if (nextByte < 0) {
                    //EOF
                    rollingChecksum.copyBlock(block);
                    rawDataBuffer.write(block);
                    break outerLoop;
                }
                rawDataBuffer.write(rollingChecksum.roll((byte) nextByte)); //�������¸��ֽڼ��������
                if (rawDataBuffer.size() > RAW_DATA_BUFFER_LIMIT) {
                    this.flushRawData(rawDataBuffer);
                }
            }

            if (rawDataBuffer.size() > 0) {
                this.flushRawData(rawDataBuffer);
            }

            this.writer.writeFileEnd(md4stream.getDigest());
        } finally {
            fileStream.close();
        }
    }
    
    // �ѻ�������flush
    private void flushRawData(ByteArrayOutputStream rawDataBuffer) throws IOException {
        final byte[] data = rawDataBuffer.toByteArray();
        this.writer.writeRawData(data.length, new ByteArrayInputStream(data));  //д��receive����
        rawDataBuffer.reset();  //��������
    }

    private void copyFileFully(int index) throws IOException {
        final FilePath file = this.filePaths.get(index);
        final InputStream fileStream = file.openInputStream();
        try {
            final MD5InputStream md4stream = new MD5InputStream(fileStream);
            this.writer.writeFileStart(index);  //��ʼд��receive
            long remainingBytes = file.getSize();
            while (remainingBytes > 0) {
                final long inThisChunk = Math.min(remainingBytes, Integer.MAX_VALUE);
                this.writer.writeRawData((int) inThisChunk, md4stream);
                remainingBytes -= inThisChunk;
            }
            this.writer.writeFileEnd(md4stream.getDigest());  //����д��receive
        } finally {
            fileStream.close();
        }
    }

}

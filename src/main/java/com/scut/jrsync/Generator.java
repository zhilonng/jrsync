package com.scut.jrsync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;


/**
 *  
 */
public class Generator implements Runnable {

    /**
     */
    private static final String MAX_FILENAME = "\uFFFF\uFFFF\uFFFF";  //最大的文件名

    private final DataInputStream input;
    private final FilePath localParentDir;
    private final SenderCommandWriter writer;  //传送到sender
    private final FastConcurrentList<TargetFileInfo> sourceFilePaths;

    private final BlockingQueue<Integer> toResend;
    private int strongHashSize = 4;

    private static class GeneratorCommandData {

        private final GeneratorCommand type;
        private final String name;
        private final long size;
        private final long lastChange;

        public GeneratorCommandData(GeneratorCommand type, String name, long size, long lastChange) {
            this.type = type;
            this.name = name;
            this.size = size;
            this.lastChange = lastChange;
        }

        public GeneratorCommand getType() {
            return this.type;
        }

        public String getName() {
            return this.name;
        }

        public long getSize() {
            return this.size;
        }

        public long getLastChange() {
            return this.lastChange;
        }

    }

    private static class GeneratorCommandIterator implements ExplicitMoveIterator<GeneratorCommandData> {

        private final DataInputStream input;
        private GeneratorCommandData current;

        public GeneratorCommandIterator(DataInputStream input) throws IOException {
            this.input = input;
            this.move();
        }

        @Override
        public GeneratorCommandData get() {
            return this.current;
        }

        @Override
        public void move() throws IOException {
            final int command = this.input.read();
            if (command < 0) {
                this.current = null;
                return;
            }
            if (command == GeneratorCommand.FILE.getCode()) { //// file = 2
                final String name = this.input.readUTF();
                final long size = this.input.readLong();
                final long changedAt = this.input.readLong();
                this.current = new GeneratorCommandData(GeneratorCommand.FILE, name, size, changedAt);
            } else if (command == GeneratorCommand.STEP_DOWN.getCode()) {  //step_down = 1
                final String name = this.input.readUTF();
                this.current = new GeneratorCommandData(GeneratorCommand.STEP_DOWN, name, -1, -1);
            } else if (command == GeneratorCommand.STEP_UP.getCode()) {   //step_up = 3
                this.current = new GeneratorCommandData(GeneratorCommand.STEP_UP, null, -1, -1);
            } else {
                throw new IOException("unknown command " + command);
            }
        }

        @Override
        public boolean hasCurrent() {
            return this.current != null;
        }

    }

    public Generator(InputStream source, FilePath remoteParentDir, BlockingQueue<Integer> toResend,
            OutputStream target, FastConcurrentList<TargetFileInfo> filePaths) {
        this.input = new DataInputStream(source);
        this.localParentDir = remoteParentDir;
        this.toResend = toResend;
        this.writer = new SenderCommandWriter(new DataOutputStream(target));  //sender
        this.sourceFilePaths = filePaths;
    }

    @Override
    public void run() {
        try {

            final ExplicitMoveIterator<GeneratorCommandData> commandIter = new GeneratorCommandIterator(this.input);
            final GeneratorCommandData baseDirCommand = commandIter.get();
            commandIter.move();
            sanityCheck(baseDirCommand.getType() == GeneratorCommand.STEP_DOWN);

            FilePath baseDir;
            if (this.localParentDir.hasChild(baseDirCommand.getName())) {
                baseDir = this.localParentDir.getChild(baseDirCommand.getName());
                if (!baseDir.isDirectory()) {
                    baseDir.delete();
                    baseDir = this.localParentDir.createSubdirectory(baseDirCommand.getName());
                }
            } else {
                baseDir = this.localParentDir.createSubdirectory(baseDirCommand.getName());
            }

            this.mergeRecursive(baseDir, commandIter);

            sanityCheck(!commandIter.hasCurrent());

            this.writer.writeEnumeratorDone();
            this.strongHashSize++;

            int lastIndex = -1;
            while (!Thread.interrupted()) {
                final int index = this.toResend.take();
                if (index < 0) {
                    break;
                }
                if (index <= lastIndex) {
                    //  新一轮
                    this.strongHashSize++;
                }
                lastIndex = index;
                this.writeCopyCommandForMissingFile(index);
            }

            this.writer.writeEverythingOk();
        } catch (final InterruptedException e) {
        } catch (final IOException e) {
            Logger.LOGGER.log(Level.SEVERE, "exception in generator", e);
        } finally {
            this.writer.close();
        }
    }

    private void mergeRecursive(FilePath localDir,
            ExplicitMoveIterator<GeneratorCommandData> commandIter) throws IOException {

        final ExplicitMoveIterator<FilePath> childrenIter =
                new ExplicitMoveAdapter<FilePath>(localDir.getChildrenSorted());
        while (!Thread.currentThread().isInterrupted()) {
            final GeneratorCommandData currentCommand = commandIter.get();
            final String remoteName = currentCommand.getName();
            final String localName = childrenIter.hasCurrent() ? childrenIter.get().getName() : MAX_FILENAME;
            switch (currentCommand.getType()) {
            case STEP_DOWN:
                if (remoteName.compareTo(localName) < 0) {
                    final FilePath subdir = localDir.createSubdirectory(remoteName);
                    commandIter.move();
                    this.createAllRecursive(subdir, commandIter);
                } else if (remoteName.compareTo(localName) > 0) {
                    childrenIter.get().delete();
                    childrenIter.move();
                } else {
                    if (childrenIter.get().isDirectory()) {
                        commandIter.move();
                        this.mergeRecursive(childrenIter.get(), commandIter);
                    } else {
                        childrenIter.get().delete();
                        final FilePath subdir = localDir.createSubdirectory(remoteName);
                        commandIter.move();
                        this.createAllRecursive(subdir, commandIter);
                    }
                    childrenIter.move();
                }
                break;
            case FILE:
                if (remoteName.compareTo(localName) < 0) {
                    final int index = this.sourceFilePaths.add(
                            new TargetFileInfo(localDir.getChild(remoteName), currentCommand.getLastChange()));
                    this.writeCopyCommandForMissingFile(index);
                    commandIter.move();
                } else if (remoteName.compareTo(localName) > 0) {
                    childrenIter.get().delete();
                    childrenIter.move();
                } else {
                    final int index = this.sourceFilePaths.add(
                            new TargetFileInfo(localDir.getChild(remoteName), currentCommand.getLastChange()));
                    if (childrenIter.get().isDirectory()) {
                        throw new RuntimeException();
//                        childrenIter.get().delete();
//                        final FilePath subdir = localDir.createSubdirectory(remoteName);
//                        commandIter.move();
//                        this.createAllRecursive(subdir, commandIter);
                    } else {
                        if (commandIter.get().getSize() != childrenIter.get().getSize()
                            || commandIter.get().getLastChange() != childrenIter.get().getLastChange()) {
                            this.writeCopyCommandForExistingFile(index);
                        }
                    }
                    commandIter.move();
                    childrenIter.move();
                }
                break;
            case STEP_UP:
                commandIter.move();
                while (childrenIter.hasCurrent()) {
                    childrenIter.get().delete();
                    childrenIter.move();
                }
                return;
            default:
                throw new IOException("unknown type " + currentCommand.getType());
            }
        }
    }

    private void writeCopyCommandForExistingFile(int index) throws IOException {
        final int blockSize = 2044 + this.strongHashSize;
        this.writer.writeFileStart(index, this.strongHashSize, blockSize);
        final InputStream in = this.sourceFilePaths.get(index).getFilePath().openInputStream();
        try {
            this.sendHashes(in, blockSize);
        } finally {
            in.close();
        }
        this.writer.writeFileEnd();
    }

    private void sendHashes(InputStream in, int blockSize) throws IOException {
        final byte[] block = new byte[blockSize];
        while (StreamHelper.readFully(in, block) == block.length) {
        	//为简单起见未发送
            final int rollingHash = Checksum32.determineFor(block);
            final byte[] strongHash = MD5.determineFor(block, this.strongHashSize);
            this.writer.writeHashes(rollingHash, strongHash);
        }
    }

    private void writeCopyCommandForMissingFile(int index) throws IOException {
        this.writer.writeFileStart(index, this.strongHashSize, 0);
        this.writer.writeFileEnd();
    }

    private void createAllRecursive(FilePath dir, ExplicitMoveIterator<GeneratorCommandData> commandIter)
        throws IOException {

        while (true) {
            final GeneratorCommandData cur = commandIter.get();
            switch (cur.getType()) {
            case STEP_DOWN:
                final FilePath subdir = dir.createSubdirectory(cur.getName());
                commandIter.move();
                this.createAllRecursive(subdir, commandIter);
                break;
            case FILE:
                final int index = this.sourceFilePaths.add(
                        new TargetFileInfo(dir.getChild(cur.getName()), cur.getLastChange()));
                this.writeCopyCommandForMissingFile(index);
                commandIter.move();
                break;
            case STEP_UP:
                commandIter.move();
                return;
            default:
                throw new IOException("should not happen");
            }
        }
    }

    private static void sanityCheck(boolean b) throws IOException {
        if (!b) {
            throw new IOException("protocol failure");
        }
    }

}

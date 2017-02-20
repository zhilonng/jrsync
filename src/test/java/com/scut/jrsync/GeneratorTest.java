/*
 */
package com.scut.jrsync;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import com.scut.jrsync.FastConcurrentList;
import com.scut.jrsync.FilePath;
import com.scut.jrsync.Generator;
import com.scut.jrsync.TargetFileInfo;

public class GeneratorTest {

    private static String callGenerator(GeneratorCommandBuilder input, StubFilePath remoteParentDir) {
        final BlockingQueue<Integer> toResend = new LinkedBlockingQueue<Integer>();
        toResend.add(-1);
        return callGenerator(input, remoteParentDir, toResend);
    }

    private static String callGenerator(GeneratorCommandBuilder input, StubFilePath remoteParentDir,
            BlockingQueue<Integer> toResend) {
        final ByteArrayInputStream source = new ByteArrayInputStream(input.toByteArray());
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final Generator e = new Generator(source, remoteParentDir, toResend, buffer, new FastConcurrentList<TargetFileInfo>());
        e.run();
        return TestHelper.toHexString(buffer.toByteArray());
    }

    private static void checkChildren(FilePath dir, String... expectedNames) throws Exception {
        assertEquals(Arrays.asList(expectedNames), TestHelper.getChildrenNames(dir));
    }


    @Test
    public void testDirectoryCreation() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        assertEquals(expected, actual);
    }

    @Test
    public void testSimpleDirectoryStructureCreation() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepDown("a")
                .stepUp()
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"), "a");
        checkChildren(remoteParentDir.getChild("xyz").getChild("a"));
        assertEquals(expected, actual);
    }

    @Test
    public void testMultiDirectoryStructureCreation() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepDown("a")
                .stepUp()
                .stepDown("b")
                .stepUp()
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"), "a", "b");
        checkChildren(remoteParentDir.getChild("xyz").getChild("a"));
        checkChildren(remoteParentDir.getChild("xyz").getChild("b"));
        assertEquals(expected, actual);
    }

    @Test
    public void testComplexDirectoryStructureCreation() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepDown("a")
                .stepDown("b")
                .stepUp()
                .stepUp()
                .stepDown("c")
                .stepUp()
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"), "a", "c");
        checkChildren(remoteParentDir.getChild("xyz").getChild("a"), "b");
        checkChildren(remoteParentDir.getChild("xyz").getChild("a").getChild("b"));
        checkChildren(remoteParentDir.getChild("xyz").getChild("c"));
        assertEquals(expected, actual);
    }

    @Test
    public void testStuffInRemoteParentDirIsLeftUntouched() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("stuff")
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "stuff", "xyz");
        checkChildren(remoteParentDir.getChild("stuff"));
        checkChildren(remoteParentDir.getChild("xyz"));
        assertEquals(expected, actual);
    }

    @Test
    public void testDirectoryDeletion1() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .startDir("willBeDeleted")
                .endDir()
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"));
        assertEquals(expected, actual);
    }

    @Test
    public void testDirectoryDeletion2() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepDown("willBeCreated")
                .stepUp()
                .stepDown("willStay")
                .stepUp()
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .startDir("willBeDeleted")
                .endDir()
                .startDir("willStay")
                .endDir()
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"), "willBeCreated", "willStay");
        checkChildren(remoteParentDir.getChild("xyz").getChild("willBeCreated"));
        checkChildren(remoteParentDir.getChild("xyz").getChild("willStay"));
        assertEquals(expected, actual);
    }

    @Test
    public void testFileDeletion() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .file("datei", 123, 456)
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"));
        assertEquals(expected, actual);
    }

    @Test
    public void testFileWithSameAttributesIsNotCopied() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .file("datei", 123, 456)
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .file("datei", 123, 456)
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"), "datei");
        assertEquals(expected, actual);
    }

    @Test
    public void testFileCreation() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .file("datei", 123, 456)
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"));
        assertEquals(expected, actual);
    }

    @Test
    public void testMultipleFileCreation() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .file("datei", 123, 456)
                .file("datei2", 123, 456)
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .startFile(1, 4, 0)
                .endFile()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"));
        assertEquals(expected, actual);
    }

    @Test
    public void testFileCreationInNewDirectory() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .stepDown("neu")
                .file("datei", 123, 456)
                .stepUp()
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .build();

        final String expected = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"), "neu");
        checkChildren(remoteParentDir.getChild("xyz").getChild("neu"));
        assertEquals(expected, actual);
    }

    @Test
    public void testResend() throws Exception {
        final LinkedBlockingQueue<Integer> toResend = new LinkedBlockingQueue<Integer>();
        toResend.add(0);
        toResend.add(-1);

        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .file("datei", 123, 456)
                .file("datei2", 123, 456)
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .startFile(1, 4, 0)
                .endFile()
                .enumeratorDone()
                .startFile(0, 5, 0)
                .endFile()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir, toResend);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"));
        assertEquals(expected, actual);
    }

    @Test
    public void testResendMultipleTimes() throws Exception {
        final LinkedBlockingQueue<Integer> toResend = new LinkedBlockingQueue<Integer>();
        toResend.add(0);
        toResend.add(1);
        toResend.add(0);
        toResend.add(0);
        toResend.add(-1);

        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .file("datei", 123, 456)
                .file("datei2", 123, 456)
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .startFile(1, 4, 0)
                .endFile()
                .enumeratorDone()
                .startFile(0, 5, 0)
                .endFile()
                .startFile(1, 5, 0)
                .endFile()
                .startFile(0, 6, 0)
                .endFile()
                .startFile(0, 7, 0)
                .endFile()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir, toResend);
        checkChildren(remoteParentDir, "xyz");
        checkChildren(remoteParentDir.getChild("xyz"));
        assertEquals(expected, actual);
    }

    @Test
    public void testFileHashing() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .file("datei", 123, 456)
                .stepUp();

        final String block1 = TestHelper.multiplyString("a", 2048);
        final String block2 = TestHelper.multiplyString("b", 2048);
        final String block3 = TestHelper.multiplyString("c", 2047);

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .file("datei", block1 + block2 + block3)
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .startFile(0, 4, 2048)
                .hash(TestHelper.rollingChecksum(block1), TestHelper.shortMD4(block1, 4))
                .hash(TestHelper.rollingChecksum(block2), TestHelper.shortMD4(block2, 4))
                .endFile()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        assertEquals(expected, actual);
    }

    @Test
    public void testNoHashForShortFile() throws Exception {
        final GeneratorCommandBuilder input = GeneratorCommandBuilder.start()
                .stepDown("xyz")
                .file("datei", 123, 456)
                .stepUp();

        final StubFilePath remoteParentDir = StubFilePathBuilder.start("tmp")
                .startDir("xyz")
                .file("datei", "kurzer Inhalt")
                .endDir()
                .build();

        final String expected = SenderCommandBuilder.start()
                .startFile(0, 4, 2048)
                .endFile()
                .enumeratorDone()
                .everythingOk()
                .toHexString();

        final String actual = callGenerator(input, remoteParentDir);
        assertEquals(expected, actual);
    }
}

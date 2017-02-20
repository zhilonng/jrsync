/*
 */
package com.scut.jrsync;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.scut.jrsync.ExceptionBuffer;
import com.scut.jrsync.FastConcurrentList;
import com.scut.jrsync.FilePath;
import com.scut.jrsync.Sender;

public class SenderTest {

    private static String callSender(SenderCommandBuilder input, FilePath... files) throws Exception {
        final FastConcurrentList<FilePath> b = new FastConcurrentList<FilePath>();
        for (final FilePath p : files) {
            b.add(p);
        }
        final ByteArrayInputStream source = new ByteArrayInputStream(input.toByteArray());
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final ExceptionBuffer exc = new ExceptionBuffer();
        final Sender e = new Sender(source, b, buffer, exc);
        e.run();
        exc.doHandling();
        return TestHelper.toHexString(buffer.toByteArray());
    }

    @Test
    public void testSendWithoutHashes() throws Exception {
        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String data = "dies sind daten in der datei";
        final StubFilePath file = new StubFilePath(null, "datei", data);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .rawData(data)
                .endFile(TestHelper.md4(data))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file);
        assertEquals(expected, actual);
    }

    @Test
    public void testSendEmptyFile() throws Exception {
        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String data = "";
        final StubFilePath file = new StubFilePath(null, "datei", data);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .endFile(TestHelper.md4(data))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file);
        assertEquals(expected, actual);
    }

    @Test
    public void testMultipleFilesWithoutHashes() throws Exception {
        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 0)
                .endFile()
                .startFile(1, 4, 0)
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String data1 = "dies sind daten in der ersten datei";
        final StubFilePath file1 = new StubFilePath(null, "datei", data1);

        final String data2 = "dies sind daten in der zweiten datei";
        final StubFilePath file2 = new StubFilePath(null, "datei2", data2);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .rawData(data1)
                .endFile(TestHelper.md4(data1))
                .startFile(1)
                .rawData(data2)
                .endFile(TestHelper.md4(data2))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file1, file2);
        assertEquals(expected, actual);
    }

    @Test
    public void testSendWithHashesAndInsertAtFront() throws Exception {
        final String block1 = TestHelper.multiplyString("x", 10);
        final String block2 = TestHelper.multiplyString("y", 10);

        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 10)
                .hash(TestHelper.rollingChecksum(block1), TestHelper.shortMD4(block1, 4))
                .hash(TestHelper.rollingChecksum(block2), TestHelper.shortMD4(block2, 4))
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String newContent = "a" + block1 + block2;

        final StubFilePath file = new StubFilePath(null, "datei", newContent);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .rawData("a")
                .copyBlock(0, 10)
                .copyBlock(10, 10)
                .endFile(TestHelper.md4(newContent))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file);
        assertEquals(expected, actual);
    }

    @Test
    public void testSendWithHashesAndInsertInMiddle() throws Exception {
        final String block1 = TestHelper.multiplyString("x", 10);
        final String block2 = TestHelper.multiplyString("y", 10);

        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 10)
                .hash(TestHelper.rollingChecksum(block1), TestHelper.shortMD4(block1, 4))
                .hash(TestHelper.rollingChecksum(block2), TestHelper.shortMD4(block2, 4))
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String newContent = block1 + "a" + block2;

        final StubFilePath file = new StubFilePath(null, "datei", newContent);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .copyBlock(0, 10)
                .rawData("a")
                .copyBlock(10, 10)
                .endFile(TestHelper.md4(newContent))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file);
        assertEquals(expected, actual);
    }

    @Test
    public void testSendWithHashesAndInsertAtEnd() throws Exception {
        final String block1 = TestHelper.multiplyString("x", 10);
        final String block2 = TestHelper.multiplyString("y", 10);

        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 10)
                .hash(TestHelper.rollingChecksum(block1), TestHelper.shortMD4(block1, 4))
                .hash(TestHelper.rollingChecksum(block2), TestHelper.shortMD4(block2, 4))
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String newContent = block1 + block2 + "a";

        final StubFilePath file = new StubFilePath(null, "datei", newContent);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .copyBlock(0, 10)
                .copyBlock(10, 10)
                .rawData("a")
                .endFile(TestHelper.md4(newContent))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file);
        assertEquals(expected, actual);
    }

    @Test
    public void testSendWithHashesAndSwapAndMultipleInserts() throws Exception {
        final String block1 = TestHelper.multiplyString("x", 10);
        final String block2 = TestHelper.multiplyString("y", 10);

        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 10)
                .hash(TestHelper.rollingChecksum(block1), TestHelper.shortMD4(block1, 4))
                .hash(TestHelper.rollingChecksum(block2), TestHelper.shortMD4(block2, 4))
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String newContent = "12" + block2 + "34" + block1 + "56";

        final StubFilePath file = new StubFilePath(null, "datei", newContent);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .rawData("12")
                .copyBlock(10, 10)
                .rawData("34")
                .copyBlock(0, 10)
                .rawData("56")
                .endFile(TestHelper.md4(newContent))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file);
        assertEquals(expected, actual);
    }

    @Test
    public void testSendWithHashesTotallyDifferentContent() throws Exception {
        final String block1 = TestHelper.multiplyString("x", 10);
        final String block2 = TestHelper.multiplyString("y", 10);

        final SenderCommandBuilder input = SenderCommandBuilder.start()
                .startFile(0, 4, 10)
                .hash(TestHelper.rollingChecksum(block1), TestHelper.shortMD4(block1, 4))
                .hash(TestHelper.rollingChecksum(block2), TestHelper.shortMD4(block2, 4))
                .endFile()
                .enumeratorDone()
                .everythingOk();

        final String newContent = "ganz neuer Inhalt";

        final StubFilePath file = new StubFilePath(null, "datei", newContent);

        final String expected = ReceiverCommandBuilder.start()
                .startFile(0)
                .rawData(newContent)
                .endFile(TestHelper.md4(newContent))
                .enumeratorDone()
                .toHexString();

        final String actual = callSender(input, file);
        assertEquals(expected, actual);
    }

}

package edu.alibaba.mpc4j.crypto.fhe.serialization;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * serialization unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/serialization.cpp.
 *
 * @author Weiran Liu
 * @date 2023/12/12
 */
public class SerializationTest {

    @Test
    public void testIsValidHeader() {
        SealHeader header = new SealHeader();
        Assert.assertTrue(Serialization.isValidHeader(header));

        // change compr_mode_type
        header.comprMode = ComprModeType.NONE;
        Assert.assertTrue(Serialization.isValidHeader(header));
        header.comprMode = ComprModeType.ZLIB;
        Assert.assertTrue(Serialization.isValidHeader(header));
        header.comprMode = ComprModeType.ZSTD;
        Assert.assertTrue(Serialization.isValidHeader(header));

        // invalid header
        SealHeader invalidHeader = new SealHeader();
        invalidHeader.magic = 0x1212;
        Assert.assertFalse(Serialization.isValidHeader(invalidHeader));

        invalidHeader.magic = Serialization.SEAL_MAGIC;
        Assert.assertEquals(invalidHeader.headerSize, Serialization.SEAL_HEADER_SIZE);

        invalidHeader.majorVersion = 0x02;
        Assert.assertFalse(Serialization.isValidHeader(invalidHeader));
    }

    @Test
    public void testSealHeaderSaveLoad() throws IOException {
        // Serialize to stream
        SealHeader header = new SealHeader();
        SealHeader loadedHeader = new SealHeader();
        header.comprMode = Serialization.COMPR_MODE_DEFAULT;
        header.size = 256;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int outSize = Serialization.saveHeader(header, outputStream);
        outputStream.close();
        Assert.assertEquals(Serialization.SEAL_HEADER_SIZE, outSize);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        int inSize = Serialization.loadHeader(inputStream, loadedHeader);
        inputStream.close();
        Assert.assertEquals(outSize, inSize);
        Assert.assertEquals(Serialization.SEAL_MAGIC, loadedHeader.magic);
        Assert.assertEquals(Serialization.SEAL_HEADER_SIZE, loadedHeader.headerSize);
        Assert.assertEquals(Serialization.SEAL_MAJOR_VERSION, loadedHeader.majorVersion);
        Assert.assertEquals(Serialization.SEAL_MINOR_VERSION, loadedHeader.minorVersion);
        Assert.assertEquals(Serialization.COMPR_MODE_DEFAULT, loadedHeader.comprMode);
        Assert.assertEquals(0x00, loadedHeader.reserved);
        Assert.assertEquals(256, loadedHeader.size);
    }

    @Test
    public void testSaveLoadToStream() throws IOException {
        EncryptionParameters encryptionParams = new EncryptionParameters();
        SealContext context = new SealContext(encryptionParams);
        TestCloneable cloneable = new TestCloneable();
        cloneable.a = 3;
        cloneable.b = ~0;
        cloneable.c = 3.14159;

        {
            // test NONE
            TestCloneable loadedCloneable = new TestCloneable();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = Serialization.save(cloneable, outputStream, ComprModeType.NONE);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = Serialization.load(context, loadedCloneable, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertEquals(cloneable.a, loadedCloneable.a);
            Assert.assertEquals(cloneable.b, loadedCloneable.b);
            Assert.assertEquals(cloneable.c, loadedCloneable.c, 1e-7);
        }
        // for such small data, the compressed result length would be larger than none.
        {
            // test ZLIB
            TestCloneable loadedCloneable = new TestCloneable();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = Serialization.save(cloneable, outputStream, ComprModeType.ZLIB);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = Serialization.load(context, loadedCloneable, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertEquals(cloneable.a, loadedCloneable.a);
            Assert.assertEquals(cloneable.b, loadedCloneable.b);
            Assert.assertEquals(cloneable.c, loadedCloneable.c, 1e-7);
        }

        {
            // test ZSTD
            TestCloneable loadedCloneable = new TestCloneable();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = Serialization.save(cloneable, outputStream, ComprModeType.ZSTD);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = Serialization.load(context, loadedCloneable, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertEquals(cloneable.a, loadedCloneable.a);
            Assert.assertEquals(cloneable.b, loadedCloneable.b);
            Assert.assertEquals(cloneable.c, loadedCloneable.c, 1e-7);
        }
    }

    @Test
    public void testSaveLoadToBuffer() throws IOException {
        TestCloneable cloneable = new TestCloneable();
        cloneable.a = 3;
        cloneable.b = ~0;
        cloneable.c = 3.14159;

        {
            // test NONE
            ComprModeType comprMode = ComprModeType.NONE;
            TestCloneable loadedCloneable = new TestCloneable();
            byte[] out = Serialization.save(cloneable, comprMode);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = Serialization.save(cloneable, outputStream, comprMode);
            outputStream.close();
            Assert.assertEquals(outSize, out.length);

            Serialization.load(null, loadedCloneable, out);
            Assert.assertEquals(cloneable.a, loadedCloneable.a);
            Assert.assertEquals(cloneable.b, loadedCloneable.b);
            Assert.assertEquals(cloneable.c, loadedCloneable.c, 1e-7);
        }
        // for such small data, the compressed result length would be larger than none.
        {
            // test ZLIB
            ComprModeType comprMode = ComprModeType.ZLIB;
            TestCloneable loadedCloneable = new TestCloneable();
            byte[] out = Serialization.save(cloneable, comprMode);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = Serialization.save(cloneable, outputStream, comprMode);
            outputStream.close();
            Assert.assertEquals(outSize, out.length);

            Serialization.load(null, loadedCloneable, out);
            Assert.assertEquals(cloneable.a, loadedCloneable.a);
            Assert.assertEquals(cloneable.b, loadedCloneable.b);
            Assert.assertEquals(cloneable.c, loadedCloneable.c, 1e-7);
        }

        {
            // test ZSTD
            ComprModeType comprMode = ComprModeType.ZSTD;
            TestCloneable loadedCloneable = new TestCloneable();
            byte[] out = Serialization.save(cloneable, comprMode);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = Serialization.save(cloneable, outputStream, comprMode);
            outputStream.close();
            Assert.assertEquals(outSize, out.length);

            Serialization.load(null, loadedCloneable, out);
            Assert.assertEquals(cloneable.a, loadedCloneable.a);
            Assert.assertEquals(cloneable.b, loadedCloneable.b);
            Assert.assertEquals(cloneable.c, loadedCloneable.c, 1e-7);
        }
    }
}

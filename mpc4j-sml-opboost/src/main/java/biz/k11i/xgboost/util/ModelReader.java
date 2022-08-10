/*
 * Original Work Copyright 2018 H2O.ai.
 * Modified Work Copyright 2021 Weiran Liu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.k11i.xgboost.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Reads the XGBoost model from stream.
 *
 * @author Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public class ModelReader implements Closeable {
    /**
     * input stream
     */
    private final InputStream stream;
    /**
     * buffer
     */
    private byte[] buffer;

    @Deprecated
    public ModelReader(String filename) throws IOException {
        this(new FileInputStream(filename));
    }

    public ModelReader(InputStream in) {
        stream = in;
    }

    private int fillBuffer(int numBytes) throws IOException {
        if (buffer == null || buffer.length < numBytes) {
            buffer = new byte[numBytes];
        }

        int numBytesRead = 0;
        while (numBytesRead < numBytes) {
            int count = stream.read(buffer, numBytesRead, numBytes - numBytesRead);
            if (count < 0) {
                return numBytesRead;
            }
            numBytesRead += count;
        }

        return numBytesRead;
    }

    public int readByteAsInt() throws IOException {
        return stream.read();
    }

    public byte[] readByteArray(int numBytes) throws IOException {
        int numBytesRead = fillBuffer(numBytes);
        if (numBytesRead < numBytes) {
            throw new EOFException(
                String.format("Cannot read byte array (shortage): expected = %d, actual = %d",
                    numBytes, numBytesRead));
        }

        byte[] result = new byte[numBytes];
        System.arraycopy(buffer, 0, result, 0, numBytes);

        return result;
    }

    public int readInt() throws IOException {
        return readInt(ByteOrder.LITTLE_ENDIAN);
    }

    public int readIntBigEndian() throws IOException {
        return readInt(ByteOrder.BIG_ENDIAN);
    }

    private int readInt(ByteOrder byteOrder) throws IOException {
        int numBytesRead = fillBuffer(Integer.BYTES);
        if (numBytesRead < Integer.BYTES) {
            throw new EOFException("Cannot read int value (shortage): " + numBytesRead);
        }

        return ByteBuffer.wrap(buffer).order(byteOrder).getInt();
    }

    public int[] readIntArray(int numValues) throws IOException {
        int numBytesRead = fillBuffer(numValues * Integer.BYTES);
        if (numBytesRead < numValues * Integer.BYTES) {
            throw new EOFException(
                String.format("Cannot read int array (shortage): expected = %d, actual = %d",
                    numValues * Integer.BYTES, numBytesRead));
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

        int[] result = new int[numValues];
        for (int i = 0; i < numValues; i++) {
            result[i] = byteBuffer.getInt();
        }

        return result;
    }

    public int readUnsignedInt() throws IOException {
        int result = readInt();
        if (result < 0) {
            throw new IOException("Cannot read unsigned int (overflow): " + result);
        }

        return result;
    }

    public long readLong() throws IOException {
        int numBytesRead = fillBuffer(Long.BYTES);
        if (numBytesRead < Long.BYTES) {
            throw new IOException("Cannot read long value (shortage): " + numBytesRead);
        }

        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public float asFloat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public int asUnsignedInt(byte[] bytes) throws IOException {
        int result = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (result < 0) {
            throw new IOException("Cannot treat as unsigned int (overflow): " + result);
        }

        return result;
    }

    public float readFloat() throws IOException {
        int numBytesRead = fillBuffer(Float.BYTES);
        if (numBytesRead < Float.BYTES) {
            throw new IOException("Cannot read float value (shortage): " + numBytesRead);
        }

        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public float[] readFloatArray(int numValues) throws IOException {
        int numBytesRead = fillBuffer(numValues * Float.BYTES);
        if (numBytesRead < numValues * Float.BYTES) {
            throw new EOFException(
                String.format("Cannot read float array (shortage): expected = %d, actual = %d",
                    numValues * 4, numBytesRead));
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

        float[] result = new float[numValues];
        for (int i = 0; i < numValues; i++) {
            result[i] = byteBuffer.getFloat();
        }

        return result;
    }

    public double[] readDoubleArrayBigEndian(int numValues) throws IOException {
        int numBytesRead = fillBuffer(numValues * Double.BYTES);
        if (numBytesRead < numValues * Double.BYTES) {
            throw new EOFException(
                String.format("Cannot read double array (shortage): expected = %d, actual = %d",
                    numValues * Double.BYTES, numBytesRead));
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN);

        double[] result = new double[numValues];
        for (int i = 0; i < numValues; i++) {
            result[i] = byteBuffer.getDouble();
        }

        return result;
    }

    public void skip(long numBytes) throws IOException {
        long numBytesRead = stream.skip(numBytes);
        if (numBytesRead < numBytes) {
            throw new IOException("Cannot skip bytes: " + numBytesRead);
        }
    }

    public String readString() throws IOException {
        long length = readLong();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Too long string: " + length);
        }

        return readString((int) length);
    }

    public String readString(int numBytes) throws IOException {
        int numBytesRead = fillBuffer(numBytes);
        if (numBytesRead < numBytes) {
            throw new IOException(String.format("Cannot read string(%d) (shortage): %d", numBytes, numBytesRead));
        }

        return new String(buffer, 0, numBytes, StandardCharsets.UTF_8);
    }

    public String readUtf() throws IOException {
        int utfLength = readByteAsInt();
        utfLength = (short) ((utfLength << 8) | readByteAsInt());
        return readUtf(utfLength);
    }

    public String readUtf(int utfLength) throws IOException {
        int numBytesRead = fillBuffer(utfLength);
        if (numBytesRead < utfLength) {
            throw new EOFException(
                String.format("Cannot read UTF string bytes: expected = %d, actual = %d",
                    utfLength, numBytesRead));
        }

        char[] charArray = new char[utfLength];

        int c, char2, char3;
        int count = 0;
        int charArrayCount = 0;

        while (count < utfLength) {
            c = (int) buffer[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            charArray[charArrayCount++] = (char) c;
        }

        while (count < utfLength) {
            c = (int) buffer[count] & 0xff;
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    count++;
                    charArray[charArrayCount++] = (char) c;
                    break;
                case 12:
                case 13:
                    // 110x xxxx   10xx xxxx
                    count += 2;
                    if (count > utfLength) {
                        throw new UTFDataFormatException(
                            "malformed input: partial character at end");
                    }
                    char2 = buffer[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException(
                            "malformed input around byte " + count);
                    }
                    charArray[charArrayCount++] = (char) (((c & 0x1F) << 6) |
                        (char2 & 0x3F));
                    break;
                case 14:
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    count += 3;
                    if (count > utfLength) {
                        throw new UTFDataFormatException(
                            "malformed input: partial character at end");
                    }
                    char2 = buffer[count - 2];
                    char3 = buffer[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new UTFDataFormatException(
                            "malformed input around byte " + (count - 1));
                    }
                    charArray[charArrayCount++] = (char) (((c & 0x0F) << 12) |
                        ((char2 & 0x3F) << 6) |
                        ((char3 & 0x3F)));
                    break;
                default:
                    // 10xx xxxx,  1111 xxxx
                    throw new UTFDataFormatException("malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(charArray, 0, charArrayCount);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}

package edu.alibaba.mpc4j.crypto.algs.utils.distribution;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.bouncycastle.util.Bytes;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Coins for generating pseudo-random boolean values under a root key and a seed. The implementation is modified from
 * <a href="https://github.com/ssavvides/jope/blob/master/src/jope/Coins.java">Coins.java</a>.
 *
 * @author Weiran Liu
 * @date 2024/1/6
 */
public class Coins {
    /**
     * 2^32 - 1
     */
    private static final double TWO_32_SUB_1 = (double) (1L << 32) - 1;
    /**
     * we use AES-CTR to generate random boolean values
     */
    private final Cipher cipher;
    /**
     * inner counter for updating IV
     */
    private long innerCounter;
    /**
     * plaintext
     */
    private final byte[] plaintext;
    /**
     * bytes randomness buffer
     */
    private byte[] bytesBuffer;
    /**
     * current byte index
     */
    private int byteIndex;
    /**
     * binary randomness buffer
     */
    private final boolean[] binaryBuffer;
    /**
     * current bit index
     */
    private int bitIndex;

    /**
     * Creates the coins.
     *
     * @param rootKey root key.
     * @param seed seed with arbitrary length.
     */
    public Coins(byte[] rootKey, byte[] seed) {
        this(rootKey, seed, 0L);
    }

    /**
     * Creates the coin.
     *
     * @param rootKey root key.
     * @param seed seed with arbitrary length.
     * @param outerCounter outer counter.
     */
    public Coins(byte[] rootKey, byte[] seed, long outerCounter) {
        MathPreconditions.checkEqual("rootKey.length", "λ", rootKey.length, CommonConstants.BLOCK_BYTE_LENGTH);
        // seed.length > 0
        MathPreconditions.checkPositive("seed.length", seed.length);
        // outer_counter >= 0
        MathPreconditions.checkNonNegative("outer_counter", outerCounter);
        try {
            // derive a key using the data to use in AES
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(rootKey, "HmacSHA256");
            mac.init(secretKey);
            byte[] countSeed = ByteBuffer.allocate(seed.length + Long.BYTES)
                .put(seed)
                .putLong(outerCounter)
                .array();
            byte[] digest = mac.doFinal(countSeed);
            // use AEC-CTR mode
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            assert cipher.getBlockSize() == CommonConstants.BLOCK_BYTE_LENGTH;
            // init cipher
            SecretKeySpec secretKeySpec = new SecretKeySpec(digest, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            assert cipher.getOutputSize(CommonConstants.BLOCK_BYTE_LENGTH) == CommonConstants.BLOCK_BYTE_LENGTH;
            // init counter and plaintext
            innerCounter = 0x0102030405060708L;
            plaintext = BlockUtils.zeroBlock();
            // init buffer
            binaryBuffer = new boolean[Bytes.SIZE];
            byteIndex = CommonConstants.BLOCK_BYTE_LENGTH;
            bitIndex = Byte.SIZE;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new IllegalStateException("Do not support assigned algorithms");
        }
    }

    private void updateBinaryBuffer(byte b) {
        binaryBuffer[0] = ((b & 0x01) != 0);
        binaryBuffer[1] = ((b & 0x02) != 0);
        binaryBuffer[2] = ((b & 0x04) != 0);
        binaryBuffer[3] = ((b & 0x08) != 0);
        binaryBuffer[4] = ((b & 0x10) != 0);
        binaryBuffer[5] = ((b & 0x20) != 0);
        binaryBuffer[6] = ((b & 0x40) != 0);
        binaryBuffer[7] = ((b & 0x80) != 0);
    }

    /**
     * Gets the next coin.
     *
     * @return the next coin.
     */
    public boolean next() {
        if (byteIndex == cipher.getBlockSize()) {
            // if we have consumed all bytes in the ciphertext, encrypt again and reset the byte index
            try {
                // use counter as parts of plaintext
                for (int i = 0; i < Long.BYTES; i++) {
                    plaintext[i] = (byte) ((innerCounter & (0xFFL << (Byte.SIZE * i))) >>> (Byte.SIZE * i));
                }
                bytesBuffer = cipher.doFinal(plaintext);
                innerCounter++;
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
                throw new IllegalStateException("Unknown error for invoking algorithms");
            }
            // reset the byte index so that we start from the first byte again.
            byteIndex = 0;
        }
        // if we have exhausted all bits in the byte indicated by byte index
        if (bitIndex == Byte.SIZE) {
            // convert current byte and move the index forward.
            updateBinaryBuffer(bytesBuffer[byteIndex++]);
            bitIndex = 0;
        }
        // return current bit and move bit index forward.
        return binaryBuffer[bitIndex++];
    }

    /**
     * Generates next float represented as double.
     *
     * @return next float.
     */
    public double nextFloat() {
        long out = 0;
        for (int i = 0; i < Float.SIZE; i++) {
            out += next() ? (1L << i) : 0;
        }
        return out / TWO_32_SUB_1;
    }

    /**
     * Generates next long.
     *
     * @return next long.
     */
    public long nextLong() {
        long out = 0;
        for (int i = 0; i < Long.SIZE; i++) {
            out += next() ? (1L << i) : 0;
        }
        return out;
    }
}

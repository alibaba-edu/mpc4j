package edu.alibaba.mpc4j.s2pc.pso.oprf.cm20;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSenderOutput;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * CM20-MPOPRF发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfSenderOutput implements MpOprfSenderOutput {
    /**
     * 不安全转换函数，参见https://stackoverflow.com/questions/43079234/convert-a-byte-array-into-an-int-array-in-java
     */
    private static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 规约批处理数量
     */
    private final int n;
    /**
     * 批处理数量字节长度
     */
    private final int nByteLength;
    /**
     * 批处理数量偏移量
     */
    private final int nOffset;
    /**
     * PRF输出比特长度（w）
     */
    private final int w;
    /**
     * PRF输出字节长度
     */
    private final int wByteLength;
    /**
     * PRF输出字节长度偏移量
     */
    private final int wOffset;
    /**
     * F: {0,1}^λ × {0,1}^* → [1, m]^w
     */
    private final Prf f;
    /**
     * H_1: {0,1}^* → {0,1}^{2λ}
     */
    private final Hash h1;
    /**
     * 矩阵C
     */
    private final byte[][] matrixC;

    Cm20MpOprfSenderOutput(EnvType envType, int batchSize, int w, byte[] prfKey, byte[][] matrixC) {
        assert batchSize > 0 : "BatchSize must be greater than 0: " + batchSize;
        this.batchSize = batchSize;
        this.n = batchSize == 1 ? 2 : batchSize;
        nByteLength = CommonUtils.getByteLength(n);
        nOffset = nByteLength * Byte.SIZE - n;
        // 输出比特长度至少大于安全常数
        assert w > CommonConstants.BLOCK_BIT_LENGTH;
        this.w = w;
        wByteLength = CommonUtils.getByteLength(w);
        wOffset = wByteLength * Byte.SIZE - w;
        // f对每一个元素要输出w个整数
        f = PrfFactory.createInstance(envType, w * Integer.BYTES);
        f.setKey(prfKey);
        h1 = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        assert matrixC.length == w;
        this.matrixC = Arrays.stream(matrixC)
            .peek(column -> {
                assert column.length == nByteLength;
                assert BytesUtils.isReduceByteArray(column, n);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getPrf(byte[] input) {
        // 计算哈希值
        byte[] extendPrf = f.getBytes(h1.digestToBytes(input));
        // F: {0, 1}^λ × {0, 1}^{2λ} → [m]^w ，这里使用不安全转换函数来提高效率。
        // 不安全转换函数的转换结果与IntUtils.byteArrayToIntArray不一致，使用时一定要保证两边使用相同的转换方法
        int[] encode = new int[w];
        UNSAFE.copyMemory(
            extendPrf, Unsafe.ARRAY_BYTE_BASE_OFFSET, encode, Unsafe.ARRAY_INT_BASE_OFFSET, extendPrf.length
        );
        for (int index = 0; index < w; index++) {
            encode[index] = Math.abs(encode[index] % n) + nOffset;
        }
        // 计算OPRF结果，当输出长度比较短的时候，直接用boolean[]会更快一些
        boolean[] binaryPrf = new boolean[wByteLength * Byte.SIZE];
        IntStream.range(0, w).forEach(wIndex -> binaryPrf[wIndex + wOffset] =
            BinaryUtils.getBoolean(matrixC[wIndex], encode[wIndex]));
        return BinaryUtils.binaryToByteArray(binaryPrf);
    }

    @Override
    public int getPrfByteLength() {
        return wByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }
}

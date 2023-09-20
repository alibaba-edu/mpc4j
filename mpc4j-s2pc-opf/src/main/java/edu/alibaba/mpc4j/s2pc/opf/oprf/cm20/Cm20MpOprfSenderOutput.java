package edu.alibaba.mpc4j.s2pc.opf.oprf.cm20;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * CM20-MP-OPRF sender output.
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfSenderOutput implements MpOprfSenderOutput {
    /**
     * batch size
     */
    private final int batchSize;
    /**
     * n = max(2, batchSize)
     */
    private final int n;
    /**
     * n in byte
     */
    private final int nByteLength;
    /**
     * n offset
     */
    private final int nOffset;
    /**
     * PRF output bit length (w)
     */
    private final int w;
    /**
     * w in byte
     */
    private final int wByteLength;
    /**
     * w offset
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
     * matrix C, organized by columns
     */
    private final byte[][] matrixC;

    Cm20MpOprfSenderOutput(EnvType envType, int batchSize, int w, byte[] prfKey, byte[][] matrixC) {
        MathPreconditions.checkPositive("batchSize", batchSize);
        this.batchSize = batchSize;
        // n = max(2, batchSize)
        n = batchSize == 1 ? 2 : batchSize;
        nByteLength = CommonUtils.getByteLength(n);
        nOffset = nByteLength * Byte.SIZE - n;
        MathPreconditions.checkGreaterOrEqual("w", w, CommonConstants.BLOCK_BIT_LENGTH);
        this.w = w;
        wByteLength = CommonUtils.getByteLength(w);
        wOffset = wByteLength * Byte.SIZE - w;
        f = PrfFactory.createInstance(envType, w * Integer.BYTES);
        f.setKey(prfKey);
        h1 = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        MathPreconditions.checkEqual("matrixC.length", "w", matrixC.length, w);
        this.matrixC = Arrays.stream(matrixC)
            .peek(column -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(column, nByteLength, n)))
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getPrf(byte[] input) {
        byte[] extendPrf = f.getBytes(h1.digestToBytes(input));
        // F: {0, 1}^λ × {0, 1}^{2λ} → [m]^w
        int[] encode = IntUtils.byteArrayToIntArray(extendPrf);
        for (int index = 0; index < w; index++) {
            encode[index] = Math.abs(encode[index] % n) + nOffset;
        }
        byte[] prf = new byte[wByteLength];
        IntStream.range(0, w).forEach(wIndex -> BinaryUtils.setBoolean(
            prf, wIndex + wOffset, BinaryUtils.getBoolean(matrixC[wIndex], encode[wIndex])
        ));
        return prf;
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

package edu.alibaba.mpc4j.s2pc.pcg.ot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;

import java.nio.ByteBuffer;

/**
 * Key derivation function based on oblivious transfer sender output.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class KdfOtSenderOutput {
    /**
     * correlated oblivious transfer sender output
     */
    private final OtSenderOutput otSenderOutput;
    /**
     * key derivation function
     */
    private final Kdf kdf;

    public KdfOtSenderOutput(EnvType envType, OtSenderOutput otSenderOutput) {
        this.otSenderOutput = otSenderOutput;
        kdf = KdfFactory.createInstance(envType);
    }

    /**
     * Get k0 at the index and the counter.
     *
     * @param index the index.
     * @param counter the counter.
     * @return R0.
     */
    public byte[] getK0(int index, long counter) {
        byte[] seed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
            .putLong(counter).put(otSenderOutput.getR0(index))
            .array();
        return kdf.deriveKey(seed);
    }

    /**
     * Get k1 at the index and the counter.
     *
     * @param index the index.
     * @return R1.
     */
    public byte[] getK1(int index, long counter) {
        byte[] seed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
            .putLong(counter).put(otSenderOutput.getR1(index))
            .array();
        return kdf.deriveKey(seed);
    }

    /**
     * Get the num.
     *
     * @return the num.
     */
    public int getNum() {
        return otSenderOutput.getNum();
    }
}

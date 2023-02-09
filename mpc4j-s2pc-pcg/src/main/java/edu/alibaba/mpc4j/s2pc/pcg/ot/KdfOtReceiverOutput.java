package edu.alibaba.mpc4j.s2pc.pcg.ot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;

import java.nio.ByteBuffer;

/**
 * Key derivation function based on oblivious transfer receiver output.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class KdfOtReceiverOutput {
    /**
     * correlated oblivious transfer receiver output
     */
    private final OtReceiverOutput otReceiverOutput;
    /**
     * key derivation function
     */
    private final Kdf kdf;

    public KdfOtReceiverOutput(EnvType envType, OtReceiverOutput otReceiverOutput) {
        this.otReceiverOutput = otReceiverOutput;
        kdf = KdfFactory.createInstance(envType);
    }

    /**
     * Get kb at the index and the counter.
     *
     * @param index the index.
     * @param counter the counter.
     * @return R0.
     */
    public byte[] getKb(int index, long counter) {
        byte[] seed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
            .putLong(counter).put(otReceiverOutput.getRb(index))
            .array();
        return kdf.deriveKey(seed);
    }

    /**
     * Get the choice bit at the index.
     *
     * @param index the index.
     * @return the choice bit.
     */
    public boolean getChoice(int index) {
        return otReceiverOutput.getChoice(index);
    }

    /**
     * Get the num.
     *
     * @return the num.
     */
    public int getNum() {
        return otReceiverOutput.getNum();
    }
}

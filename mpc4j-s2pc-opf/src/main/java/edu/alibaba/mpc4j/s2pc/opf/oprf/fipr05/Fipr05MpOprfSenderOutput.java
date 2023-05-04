package edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

/**
 * FIPR05 multi-query OPRF sender output.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Fipr05MpOprfSenderOutput implements MpOprfSenderOutput {
    /**
     * batch size
     */
    private final int batchSize;
    /**
     * single-query OPRF key
     */
    private final SqOprfKey sqOprfKey;

    Fipr05MpOprfSenderOutput(int batchSize, SqOprfKey sqOprfKey) {
        MathPreconditions.checkPositive("batchSize", batchSize);
        this.batchSize = batchSize;
        this.sqOprfKey = sqOprfKey;
    }

    @Override
    public byte[] getPrf(byte[] input) {
        return sqOprfKey.getPrf(input);
    }

    @Override
    public int getPrfByteLength() {
        return sqOprfKey.getPrfByteLength();
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }
}

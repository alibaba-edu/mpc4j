package edu.alibaba.mpc4j.s2pc.pcg;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.stream.IntStream;

/**
 * abstract batch PCG output
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public abstract class AbstractBatchPcgOutput implements BatchPcgOutput {
    /**
     * batch num
     */
    protected final int batchNum;
    /**
     * each num
     */
    protected final int eachNum;

    public AbstractBatchPcgOutput(PcgPartyOutput[] outputs) {
        batchNum = outputs.length;
        MathPreconditions.checkPositive("batchNum", batchNum);
        // get each num
        eachNum = outputs[0].getNum();
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            PcgPartyOutput output = outputs[batchIndex];
            MathPreconditions.checkEqual("eachNum", batchIndex + "-th num", eachNum, output.getNum());
        });
    }

    @Override
    public int getEachNum() {
        return eachNum;
    }

    @Override
    public int getBatchNum() {
        return batchNum;
    }
}

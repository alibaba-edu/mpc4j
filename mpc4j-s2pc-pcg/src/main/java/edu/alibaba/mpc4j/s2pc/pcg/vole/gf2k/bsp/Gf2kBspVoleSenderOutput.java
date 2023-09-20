package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.BatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleSenderOutput;

import java.util.Arrays;

/**
 * Batch single-point GF2K-VOLE sender output.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVoleSenderOutput implements BatchPcgOutput {
    /**
     * GF2K-SSP-VOLE sender outputs
     */
    private Gf2kSspVoleSenderOutput[] senderOutputs;
    /**
     * num in each GF2K-SSP-VOLE sender output
     */
    private int eachNum;

    /**
     * Creates the sender output.
     *
     * @param gf2kSspVoleSenderOutputs GF2K-SSP-VOLE sender outputs.
     * @return sender output.
     */
    public static Gf2kBspVoleSenderOutput create(Gf2kSspVoleSenderOutput[] gf2kSspVoleSenderOutputs) {
        Gf2kBspVoleSenderOutput senderOutput = new Gf2kBspVoleSenderOutput();
        MathPreconditions.checkPositive("GF2K-SSP-VOLE num", gf2kSspVoleSenderOutputs.length);
        // get each num
        senderOutput.eachNum = gf2kSspVoleSenderOutputs[0].getNum();
        // set other outputs
        senderOutput.senderOutputs = Arrays.stream(gf2kSspVoleSenderOutputs)
            .peek(iOutput ->
                MathPreconditions.checkEqual("each num", "i-th num", senderOutput.eachNum, iOutput.getNum())
            )
            .toArray(Gf2kSspVoleSenderOutput[]::new);
        return senderOutput;
    }

    /**
     * private constructor
     */
    private Gf2kBspVoleSenderOutput() {
        // empty
    }

    /**
     * Gets the assigned GF2K-SSP-VOLE sender output.
     *
     * @param index index.
     * @return GF2K-SSP-VOLE sender output.
     */
    public Gf2kSspVoleSenderOutput get(int index) {
        return senderOutputs[index];
    }

    /**
     * Gets num in each GF2K-SSP-VOLE sender output.
     *
     * @return num in each GF2K-SSP-VOLE sender output.
     */
    public int getEachNum() {
        return eachNum;
    }

    @Override
    public int getBatchNum() {
        return senderOutputs.length;
    }
}

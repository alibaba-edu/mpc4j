package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.BatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleReceiverOutput;

import java.util.Arrays;

/**
 * Batch single-point GF2K-VOLE receiver output.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVoleReceiverOutput implements BatchPcgOutput {
    /**
     * GF2K-SSP-VOLE receiver outputs
     */
    private Gf2kSspVoleReceiverOutput[] receiverOutputs;
    /**
     * Δ
     */
    private byte[] delta;
    /**
     * num in each GF2K-SSP-VOLE receiver output
     */
    private int eachNum;

    /**
     * Creates the receiver output.
     *
     * @param gf2kSspVoleReceiverOutputs GF2K-SSP-VOLE receiver outputs.
     * @return the receiver output.
     */
    public static Gf2kBspVoleReceiverOutput create(Gf2kSspVoleReceiverOutput[] gf2kSspVoleReceiverOutputs) {
        MathPreconditions.checkPositive("num", gf2kSspVoleReceiverOutputs.length);
        Gf2kBspVoleReceiverOutput receiverOutput = new Gf2kBspVoleReceiverOutput();
        // get Δ and each num
        receiverOutput.delta = BytesUtils.clone(gf2kSspVoleReceiverOutputs[0].getDelta());
        receiverOutput.eachNum = gf2kSspVoleReceiverOutputs[0].getNum();
        // set other outputs
        receiverOutput.receiverOutputs = Arrays.stream(gf2kSspVoleReceiverOutputs)
            .peek(iOutput -> {
                Preconditions.checkArgument(BytesUtils.equals(receiverOutput.delta, iOutput.getDelta()));
                MathPreconditions.checkEqual("each num", "i-th num", receiverOutput.eachNum, iOutput.getNum());
            })
            .toArray(Gf2kSspVoleReceiverOutput[]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private Gf2kBspVoleReceiverOutput() {
        // empty
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * Gets the assigned GF2K-SSP-VOLE receiver output.
     *
     * @param index index.
     * @return GF2K-SSP-VOLE receiver output.
     */
    public Gf2kSspVoleReceiverOutput get(int index) {
        return receiverOutputs[index];
    }

    /**
     * Get num in each GF2K-SSP-VOLE receiver output.
     *
     * @return num in each GF2K-SSP-VOLE receiver output.
     */
    public int getEachNum() {
        return eachNum;
    }

    @Override
    public int getBatchNum() {
        return receiverOutputs.length;
    }
}

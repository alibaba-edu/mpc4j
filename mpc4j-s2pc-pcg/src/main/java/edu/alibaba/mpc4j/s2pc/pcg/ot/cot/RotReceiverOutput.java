package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtReceiverOutput;

import java.util.Arrays;

/**
 * Random oblivious transfer receiver output.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class RotReceiverOutput implements OtReceiverOutput {
    /**
     * correlated oblivious transfer receiver output
     */
    private final CotReceiverOutput cotReceiverOutput;
    /**
     * correlated robust hash function
     */
    private final Crhf crhf;

    public RotReceiverOutput(EnvType envType, CrhfFactory.CrhfType crhfType, CotReceiverOutput cotReceiverOutput) {
        this.cotReceiverOutput = cotReceiverOutput;
        crhf = CrhfFactory.createInstance(envType, crhfType);
    }

    @Override
    public boolean getChoice(int index) {
        return cotReceiverOutput.getChoice(index);
    }

    @Override
    public boolean[] getChoices() {
        return cotReceiverOutput.getChoices();
    }

    @Override
    public byte[] getRb(int index) {
        return crhf.hash(cotReceiverOutput.getRb(index));
    }

    @Override
    public byte[][] getRbArray() {
        return Arrays.stream(cotReceiverOutput.getRbArray())
            .map(crhf::hash)
            .toArray(byte[][]::new);
    }

    @Override
    public int getNum() {
        return cotReceiverOutput.getNum();
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtSenderOutput;

import java.util.Arrays;

/**
 * Random oblivious transfer sender output.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class RotSenderOutput implements OtSenderOutput {
    /**
     * correlated oblivious transfer sender output
     */
    private final CotSenderOutput cotSenderOutput;
    /**
     * correlated robust hash function
     */
    private final Crhf crhf;

    public RotSenderOutput(EnvType envType, CrhfFactory.CrhfType crhfType, CotSenderOutput cotSenderOutput) {
        this.cotSenderOutput = cotSenderOutput;
        crhf = CrhfFactory.createInstance(envType, crhfType);
    }

    @Override
    public byte[] getR0(int index) {
        return crhf.hash(cotSenderOutput.getR0(index));
    }

    @Override
    public byte[][] getR0Array() {
        return Arrays.stream(cotSenderOutput.getR0Array())
            .map(crhf::hash)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getR1(int index) {
        return crhf.hash(cotSenderOutput.getR1(index));
    }

    @Override
    public byte[][] getR1Array() {
        return Arrays.stream(cotSenderOutput.getR1Array())
            .map(crhf::hash)
            .toArray(byte[][]::new);
    }

    @Override
    public int getNum() {
        return cotSenderOutput.getNum();
    }
}

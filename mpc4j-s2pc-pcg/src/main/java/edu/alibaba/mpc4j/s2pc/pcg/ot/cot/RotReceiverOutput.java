package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtReceiverOutput;

import java.util.stream.IntStream;

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

    public RotReceiverOutput(EnvType envType, CrhfType crhfType, CotReceiverOutput cotReceiverOutput) {
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
        // we only need to call CRHF for R1, recall R1 is the case when getChoice returns true (so first R1 then R0).
        return cotReceiverOutput.getChoice(index) ?
            crhf.hash(cotReceiverOutput.getRb(index)) : cotReceiverOutput.getRb(index);
    }

    @Override
    public byte[][] getRbArray() {
        return IntStream.range(0, cotReceiverOutput.getNum())
            .mapToObj(this::getRb)
            .toArray(byte[][]::new);
    }

    @Override
    public int getNum() {
        return cotReceiverOutput.getNum();
    }
}

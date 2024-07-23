package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.stream.IntStream;

/**
 * Single Share Translation receiver output.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class SstReceiverOutput implements PcgPartyOutput {
    /**
     * num
     */
    private final int num;
    /**
     * element byte length
     */
    private final int byteLength;
    /**
     * as such that Δs = π(as) ⊕ bs
     */
    private final byte[][] as;
    /**
     * bs such that Δs = π(as) ⊕ bs
     */
    private final byte[][] bs;

    public SstReceiverOutput(byte[][] as, byte[][] bs) {
        num = as.length;
        MathPreconditions.checkPositive("n", num);
        MathPreconditions.checkEqual("n", "bs.length", num, bs.length);
        byteLength = as[0].length;
        MathPreconditions.checkPositive("byte_length", byteLength);
        IntStream.range(0, num).forEach(i -> {
            MathPreconditions.checkEqual("byte_length", "as[" + i + "].length", byteLength, as[i].length);
            MathPreconditions.checkEqual("byte_length", "bs[" + i + "].length", byteLength, bs[i].length);
        });
        this.as = as;
        this.bs = bs;
    }

    /**
     * Gets as[i].
     *
     * @param i i.
     * @return as[i].
     */
    public byte[] getA(int i) {
        return as[i];
    }

    /**
     * Gets as.
     *
     * @return as.
     */
    public byte[][] getAs() {
        return as;
    }

    /**
     * Gets bs[i].
     *
     * @param i i.
     * @return bs[i].
     */
    public byte[] getB(int i) {
        return bs[i];
    }

    /**
     * Gets bs.
     *
     * @return bs.
     */
    public byte[][] getBs() {
        return bs;
    }

    /**
     * Gets element byte length.
     *
     * @return element byte length.
     */
    public int getByteLength() {
        return byteLength;
    }

    @Override
    public int getNum() {
        return num;
    }
}

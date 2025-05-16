package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.stream.IntStream;

/**
 * single-point RDPPRF receiver output.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SpRdpprfReceiverOutput implements PcgPartyOutput {
    /**
     * n
     */
    private final int num;
    /**
     * log(n)
     */
    private final int logNum;
    /**
     * v[0], ..., v[n - 1] where v[i] = ⊥
     */
    private final byte[][] v1Array;
    /**
     * index α
     */
    private final int alpha;

    public SpRdpprfReceiverOutput(int alpha, byte[][] v1Array) {
        MathPreconditions.checkPositive("n", v1Array.length);
        this.num = v1Array.length;
        logNum = LongUtils.ceilLog2(num);
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        IntStream.range(0, num).forEach(index -> {
            if (index == alpha) {
                Preconditions.checkArgument(v1Array[index] == null);
            } else {
                Preconditions.checkArgument(BlockUtils.valid(v1Array[index]));
            }
        });
        this.v1Array = v1Array;
    }

    /**
     * Get α.
     *
     * @return α.
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Get log(n).
     *
     * @return log(n).
     */
    public int getLogNum() {
        return logNum;
    }

    /**
     * Gets v[0], ..., v[n - 1] where v[i] = ⊥.
     *
     * @return v[0], ..., v[n - 1].
     */
    public byte[][] getV1Array() {
        return v1Array;
    }

    /**
     * Gets v[i].
     *
     * @param index index i.
     * @return v[i].
     */
    public byte[] getV1(int index) {
        return v1Array[index];
    }

    @Override
    public int getNum() {
        return num;
    }
}

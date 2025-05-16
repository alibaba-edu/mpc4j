package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.stream.IntStream;

/**
 * single-point RDPPRF sender output.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SpRdpprfSenderOutput implements PcgPartyOutput {
    /**
     * n
     */
    private final int num;
    /**
     * log(n)
     */
    private final int logNum;
    /**
     * v[0], ..., v[n]
     */
    private final byte[][] v0Array;

    public SpRdpprfSenderOutput(byte[][] v0Array) {
        MathPreconditions.checkPositive("n", v0Array.length);
        this.num = v0Array.length;
        logNum = LongUtils.ceilLog2(num);
        IntStream.range(0, num).forEach(index -> Preconditions.checkArgument(BlockUtils.valid(v0Array[index])));
        this.v0Array = v0Array;
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
     * Gets v[0], ..., v[n].
     *
     * @return v[0], ..., v[n].
     */
    public byte[][] getV0Array() {
        return v0Array;
    }

    /**
     * Gets v[i].
     *
     * @param index index i.
     * @return v[i].
     */
    public byte[] getV0(int index) {
        return v0Array[index];
    }

    @Override
    public int getNum() {
        return num;
    }
}

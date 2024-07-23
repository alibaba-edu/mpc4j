package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * single-point CDPPRF sender output.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class SpCdpprfSenderOutput implements PcgPartyOutput {
    /**
     * n
     */
    private final int num;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * v[0], ..., v[n]
     */
    private final byte[][] v0Array;

    public SpCdpprfSenderOutput(byte[] delta, byte[][] v0Array) {
        MathPreconditions.checkEqual(
            "λ", "Δ.length", CommonConstants.BLOCK_BYTE_LENGTH, delta.length
        );
        this.delta = delta;
        num = v0Array.length;
        Preconditions.checkArgument(IntMath.isPowerOfTwo(num));
        byte[] actualDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        IntStream.range(0, num).forEach(index -> {
                MathPreconditions.checkEqual(
                    "λ", "v[" + index + "].length", CommonConstants.BLOCK_BYTE_LENGTH, v0Array[index].length
                );
                BytesUtils.xori(actualDelta, v0Array[index]);
            }
        );
        Preconditions.checkArgument(Arrays.equals(delta, actualDelta));
        this.v0Array = v0Array;
    }

    /**
     * Gets Δ
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
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

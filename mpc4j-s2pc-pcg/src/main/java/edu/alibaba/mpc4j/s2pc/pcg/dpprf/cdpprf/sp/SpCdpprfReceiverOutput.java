package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.stream.IntStream;

/**
 * single-point CDPPRF receiver output.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class SpCdpprfReceiverOutput implements PcgPartyOutput {
    /**
     * n
     */
    private final int num;
    /**
     * v[0], ..., v[n - 1] where v[i] = ⊥
     */
    private final byte[][] v1Array;
    /**
     * index α
     */
    private final int alpha;

    public SpCdpprfReceiverOutput(int alpha, byte[][] v1Array) {
        num = v1Array.length;
        Preconditions.checkArgument(IntMath.isPowerOfTwo(num));
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        IntStream.range(0, num).forEach(index -> {
            if (index == alpha) {
                Preconditions.checkArgument(v1Array[index] == null);
            } else {
                MathPreconditions.checkEqual(
                    "λ", "v[" + index + "].length", CommonConstants.BLOCK_BYTE_LENGTH, v1Array[index].length
                );
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

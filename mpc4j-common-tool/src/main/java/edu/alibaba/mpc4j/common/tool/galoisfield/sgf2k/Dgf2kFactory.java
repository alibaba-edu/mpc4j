package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Direct GF(2^l) factory.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public class Dgf2kFactory {
    /**
     * private constructor.
     */
    private Dgf2kFactory() {
        // empty
    }

    /**
     * Creates an instance.
     *
     * @param envType   environment.
     * @param subfieldL subfield L.
     * @return an instance.
     */
    public static Dgf2k getInstance(EnvType envType, int subfieldL) {
        // l ∈ {1, 2, 4, 8, 16, 32, 64, 128}
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL <= CommonConstants.BLOCK_BIT_LENGTH
        );
        return new Dgf2k(envType, subfieldL);
    }
}

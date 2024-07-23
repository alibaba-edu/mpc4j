package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Subfield GF2K factory.
 *
 * @author Weiran Liu
 * @date 2024/6/3
 */
public class Sgf2kFactory {
    /**
     * NTL SGF2K map
     */
    private static final TIntObjectMap<Sgf2k> NTL_SGF2K_MAP = new TIntObjectHashMap<>();
    /**
     * Rings SGF2K map
     */
    private static final TIntObjectMap<Sgf2k> RINGS_SGF2K_MAP = new TIntObjectHashMap<>();

    /**
     * Gets an instance.
     *
     * @param envType environment.
     * @param subfieldL subfield L.
     * @return an instance.
     */
    public static Sgf2k getInstance(EnvType envType, int subfieldL) {
        // l ∈ {1, 2, 4, 8, 16, 32, 64, 128}
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL <= CommonConstants.BLOCK_BIT_LENGTH
        );
        if (subfieldL == 1) {
            return new Sgf2k001(envType);
        }
        if (subfieldL == CommonConstants.BLOCK_BIT_LENGTH) {
            return new Sgf2k128(envType);
        }
        switch (envType) {
            case STANDARD:
            case INLAND:
                if (NTL_SGF2K_MAP.containsKey(subfieldL)) {
                    return NTL_SGF2K_MAP.get(subfieldL);
                } else {
                    Sgf2k sgf2k = new NtlSubSgf2k(envType, subfieldL);
                    NTL_SGF2K_MAP.put(subfieldL, sgf2k);
                    return sgf2k;
                }
            case INLAND_JDK:
            case STANDARD_JDK:
                if (RINGS_SGF2K_MAP.containsKey(subfieldL)) {
                    return RINGS_SGF2K_MAP.get(subfieldL);
                } else {
                    Sgf2k sgf2k = new RingsSubSgf2k(envType, subfieldL);
                    RINGS_SGF2K_MAP.put(subfieldL, sgf2k);
                    return sgf2k;
                }
            default:
                throw new IllegalArgumentException("Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64, 128}:" + subfieldL);
        }
    }
}

package edu.alibaba.mpc4j.common.tool.crypto.crhf;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShuffle;

/**
 * MMO_σ(x) = π(σ(x)) ⊕ σ(x) using SIMD.
 *
 * @author Weiran Liu
 * @date 2024/6/24
 */
public class SimdMmoSigmaCrhf implements Crhf {
    /**
     * mask = 1^{64} || 0^{64}
     */
    private static final ByteVector MASK = ByteVector.fromArray(
        ByteVector.SPECIES_128,
        new byte[]{
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        },
        0
    );
    /**
     * mm_shuffle_epi32(a, 78) = [a_0, a_1, a_2, a_3] -> [a_1, a_0, a_3, a_2]
     */
    private static final VectorShuffle<Byte> VECTOR_SHUFFLE = VectorShuffle.fromValues(
        ByteVector.SPECIES_128,
        4, 5, 6, 7, 0, 1, 2, 3, 12, 13, 14, 15, 8, 9, 10, 11
    );
    /**
     * pseudo-random permutation
     */
    private final Prp prp;

    /**
     * Creates an MMO_σ(x).
     *
     * @param envType environment.
     */
    SimdMmoSigmaCrhf(EnvType envType) {
        prp = PrpFactory.createInstance(envType);
        prp.setKey(BlockUtils.zeroBlock());
    }

    @Override
    public byte[] hash(byte[] block) {
        // σ(x)
        byte[] sigmaX = sigma(block);
        // π(σ(x))
        byte[] output = prp.prp(sigmaX);
        // π(σ(x)) ⊕ σ(x)
        BlockUtils.xori(output, sigmaX);
        return output;
    }

    @Override
    public CrhfType getCrhfType() {
        return CrhfType.SIMD_MMO_SIGMA;
    }

    /**
     * σ(x) = mm_shuffle_epi32(a, 78) ⊕ and_si128(a, mask) using SIMD.
     *
     * @param x x.
     * @return σ(x).
     */
    private byte[] sigma(byte[] x) {
        assert BlockUtils.valid(x);
        ByteVector vectorX = ByteVector.fromArray(ByteVector.SPECIES_128, x, 0);
        ByteVector sigmaX = vectorX.rearrange(VECTOR_SHUFFLE);
        ByteVector maskX = vectorX.and(MASK);
        return sigmaX.lanewise(VectorOperators.XOR, maskX).toArray();
    }
}

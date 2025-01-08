package edu.alibaba.mpc4j.common.structure.fusefilter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * byte fuse position with arity = 3.
 *
 * @author Weiran Liu
 * @date 2024/7/25
 */
public class Arity3ByteFusePosition<T> extends AbstractArity3ByteFusePosition<T> {

    public Arity3ByteFusePosition(EnvType envType, int size, int valueByteLength, byte[] seed) {
        super(envType, size, valueByteLength);
        MathPreconditions.checkEqual("seed.length", "λ", seed.length, CommonConstants.BLOCK_BYTE_LENGTH);
        System.arraycopy(seed, 0, this.seed, 0, CommonConstants.BLOCK_BYTE_LENGTH);
        hash.setKey(this.seed);
    }
}

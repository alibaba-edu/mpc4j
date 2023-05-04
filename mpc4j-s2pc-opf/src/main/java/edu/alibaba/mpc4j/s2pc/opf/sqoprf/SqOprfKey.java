package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

/**
 * single-query OPRF key.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfKey {
    /**
     * Gets the prf output.
     *
     * @param input the input.
     * @return the prf output.
     */
    byte[] getPrf(byte[] input);

    /**
     * Gets PRF byte length.
     *
     * @return PRF byte length.
     */
    default int getPrfByteLength() {
        return CommonConstants.BLOCK_BYTE_LENGTH;
    }
}

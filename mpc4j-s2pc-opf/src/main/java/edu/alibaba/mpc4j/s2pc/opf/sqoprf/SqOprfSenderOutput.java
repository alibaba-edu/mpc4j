package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

/**
 * single-query OPRF sender output.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfSenderOutput {
    /**
     * Gets the sing-query OPRF key.
     *
     * @return the sing-query OPRF key.
     */
    SqOprfKey getKey();

    /**
     * Gets the prf output.
     *
     * @param input the input.
     * @return the prf output.
     */
    byte[] getPrf(byte[] input);
}

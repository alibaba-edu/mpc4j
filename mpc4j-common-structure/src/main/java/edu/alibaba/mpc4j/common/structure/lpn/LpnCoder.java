package edu.alibaba.mpc4j.common.structure.lpn;

/**
 * LPN coder.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public interface LpnCoder {
    /**
     * Gets code size (n).
     *
     * @return code size (n).
     */
    int getCodeSize();

    /**
     * Gets message size (k).
     *
     * @return message size (k).
     */
    int getMessageSize();

    /**
     * Sets parallel encoding.
     *
     * @param parallel parallel encoding.
     */
    void setParallel(boolean parallel);

    /**
     * Gets parallel encoding.
     *
     * @return parallel encoding.
     */
    boolean getParallel();
}

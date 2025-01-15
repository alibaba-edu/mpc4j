package edu.alibaba.mpc4j.s2pc.pir.cppir.ks;

/**
 * Hint-based client-specific preprocessing keyword PIR client.
 *
 * @author Weiran Liu
 * @date 2025/1/8
 */
public interface HintCpKsPirClient<T> extends CpKsPirClient<T> {
    /**
     * Updates keys.
     */
    void updateKeys();
}

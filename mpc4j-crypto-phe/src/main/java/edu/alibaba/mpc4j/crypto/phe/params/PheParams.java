package edu.alibaba.mpc4j.crypto.phe.params;

import java.util.List;

/**
 * PHE parameters.
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PheParams {
    /**
     * Serializes the PHE parameter into {@code List<byte[]>}.
     *
     * @return serialized result.
     */
    List<byte[]> serialize();
}

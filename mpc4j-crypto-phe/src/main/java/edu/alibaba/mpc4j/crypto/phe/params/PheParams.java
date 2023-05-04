package edu.alibaba.mpc4j.crypto.phe.params;

import java.util.List;

/**
 * 半同态加密参数。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PheParams {
    /**
     * Packets the PHE parameter into {@code List<byte[]>}.
     *
     * @return the packet result.
     */
    List<byte[]> toByteArrayList();
}

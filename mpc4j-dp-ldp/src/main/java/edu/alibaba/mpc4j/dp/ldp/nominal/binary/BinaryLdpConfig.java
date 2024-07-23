package edu.alibaba.mpc4j.dp.ldp.nominal.binary;

import edu.alibaba.mpc4j.dp.ldp.nominal.NominalLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpFactory.BinaryLdpType;

/**
 * Binary LDP config.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public interface BinaryLdpConfig extends NominalLdpConfig {
    /**
     * Gets the type.
     *
     * @return type.
     */
    BinaryLdpType getType();
}

package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * default fixed key PRP.
 *
 * @author Weiran Liu
 * @date 2024/10/26
 */
public class DefaultFixedKeyPrp implements FixedKeyPrp {
    /**
     * prp
     */
    private final Prp prp;

    /**
     * Creates a default fixed key PRP.
     */
    public DefaultFixedKeyPrp(EnvType envType) {
        prp = PrpFactory.createInstance(envType);
        prp.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    @Override
    public byte[] prp(byte[] plaintext) {
        return prp.prp(plaintext);
    }
}

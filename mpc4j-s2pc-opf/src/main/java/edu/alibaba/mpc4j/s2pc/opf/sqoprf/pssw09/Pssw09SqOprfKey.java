package edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

/**
 * PSSW09 single-query OPRF key.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class Pssw09SqOprfKey implements SqOprfKey {
    /**
     * hash function
     */
    private final Hash hash;
    /**
     * prp
     */
    private final Prp prp;
    /**
     * prp evaluation option
     */
    private final boolean isInvPrp;
    /**
     * OPRP key
     */
    private final byte[] oprpKey;

    public Pssw09SqOprfKey(EnvType envType, byte[] key, PrpFactory.PrpType prpType, boolean isInvOprp) {
        MathPreconditions.checkEqual("key.length", "κ", key.length, CommonConstants.BLOCK_BYTE_LENGTH);
        prp = PrpFactory.createInstance(prpType);
        this.isInvPrp = isInvOprp;
        prp.setKey(key);
        oprpKey = BytesUtils.clone(key);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public byte[] getPrf(byte[] input) {
        byte[] hashInput = hash.digestToBytes(input);
        return isInvPrp ? prp.invPrp(hashInput) : prp.prp(hashInput);
    }

    public byte[] getOprpKey() {
        return oprpKey;
    }
}

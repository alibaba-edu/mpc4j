package edu.alibaba.mpc4j.common.tool.crypto.crhf;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.DefaultFixedKeyPrp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * MMO(x) = π(x) ⊕ x, where π is initialized using fixed-key PRP. The scheme is presented in Section 7.2 of the paper:
 * <p>
 * Guo C, Katz J, Wang X, et al. Efficient and secure multiparty computation from fixed-key block ciphers.
 * 2020 IEEE Symposium on Security and Privacy (SP). IEEE, 2020: 825-841.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/10/27
 */
public class FixedKeyMmoCrhf implements Crhf {
    /**
     * fixed key PRP
     */
    private final FixedKeyPrp fixedKeyPrp;

    /**
     * Creates MMO(x).
     *
     * @param envType environment.
     */
    FixedKeyMmoCrhf(EnvType envType) {
        fixedKeyPrp = new DefaultFixedKeyPrp(envType);
    }

    /**
     * Creates MMO(x).
     *
     * @param fixedKeyPrp fixed-key PRP.
     */
    FixedKeyMmoCrhf(FixedKeyPrp fixedKeyPrp) {
        this.fixedKeyPrp = fixedKeyPrp;
    }

    @Override
    public byte[] hash(byte[] block) {
        // MMO(x) = π(x) ⊕ x
        byte[] output = fixedKeyPrp.prp(block);
        BytesUtils.xori(output, block);
        return output;
    }

    @Override
    public CrhfFactory.CrhfType getCrhfType() {
        return CrhfType.FIXED_KEY_MMO;
    }
}

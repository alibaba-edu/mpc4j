package edu.alibaba.mpc4j.common.tool.crypto.crhf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * MMO(x) = π(x) ⊕ x（满足抗关联性），由下述论文第7.2节给出：
 * Guo C, Katz J, Wang X, et al. Efficient and secure multiparty computation from fixed-key block ciphers.
 * 2020 IEEE Symposium on Security and Privacy (SP). IEEE, 2020: 825-841.
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
class MmoCrhf implements Crhf {
    /**
     * 伪随机置换π。
     */
    private final Prp prp;

    /**
     * 构建MMO(x)。
     *
     * @param envType 环境类型。
     */
    MmoCrhf(EnvType envType) {
        prp = PrpFactory.createInstance(envType);
        // 默认伪随机置换密钥为全0
        prp.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    @Override
    public byte[] hash(byte[] block) {
        // MMO(x) = π(x) ⊕ x
        byte[] output = prp.prp(block);
        BytesUtils.xori(output, block);
        return output;
    }

    @Override
    public CrhfFactory.CrhfType getCrhfType() {
        return CrhfFactory.CrhfType.MMO;
    }
}

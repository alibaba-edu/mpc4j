package edu.alibaba.mpc4j.common.tool.crypto.tcrhf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.nio.ByteBuffer;

/**
 * TMMO(x) = π(π(x) ⊕ i) ⊕ π(x)（满足抗关联性），由下述论文第7.4节给出：
 * Guo C, Katz J, Wang X, et al. Efficient and secure multiparty computation from fixed-key block ciphers.
 * 2020 IEEE Symposium on Security and Privacy (SP). IEEE, 2020: 825-841.
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
class TmmoTcrhf implements Tcrhf {
    /**
     * 伪随机置换π。
     */
    private final Prp prp;

    /**
     * 构建TMMO(x)。
     *
     * @param envType 环境类型。
     */
    TmmoTcrhf(EnvType envType) {
        prp = PrpFactory.createInstance(envType);
        // 默认伪随机置换密钥为全0
        prp.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    @Override
    public byte[] hash(int index, byte[] block) {
        // 将索引值转换为indexBytes的最后4个字节
        byte[] indexBytes = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, index)
            .array();
        // π(x)
        byte[] pai = prp.prp(block);
        // π(π(x) ⊕ i)
        byte[] output = BytesUtils.xor(pai, indexBytes);
        output = prp.prp(output);
        // TMMO(x) = π(π(x) ⊕ i) ⊕ π(x)
        BytesUtils.xori(output, pai);
        return output;
    }

    @Override
    public byte[] hash(int leftIndex, int rightIndex, byte[] block) {
        // 将两个索引值转换为indexBytes的最后8个字节
        byte[] indexBytes = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES * 2, leftIndex)
            .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, rightIndex)
            .array();
        // π(x)
        byte[] pai = prp.prp(block);
        // π(π(x) ⊕ i)
        byte[] output = BytesUtils.xor(pai, indexBytes);
        output = prp.prp(output);
        // TMMO(x) = π(π(x) ⊕ i) ⊕ π(x)
        BytesUtils.xori(output, pai);
        return output;
    }

    @Override
    public TcrhfFactory.TcrhfType getTcrhfType() {
        return TcrhfFactory.TcrhfType.TMMO;
    }
}

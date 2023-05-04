package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * OPRP发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class OprpSenderOutput {
    /**
     * PRP类型
     */
    private final PrpType prpType;
    /**
     * 是否为逆PRP运算
     */
    private final boolean invPrp;
    /**
     * 密钥
     */
    private final byte[] key;
    /**
     * 分享值
     */
    private final byte[][] shares;

    public OprpSenderOutput(PrpType prpType, boolean invPrp, byte[] key, byte[][] shares) {
        this.prpType = prpType;
        this.invPrp = invPrp;
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        this.key = BytesUtils.clone(key);
        assert shares.length > 0;
        this.shares = Arrays.stream(shares)
            .peek(share -> {
                assert share.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    /**
     * 返回PRP类型。
     *
     * @return PRP类型。
     */
    public PrpType getPrpType() {
        return prpType;
    }

    /**
     * 返回协议是否为逆映射。
     *
     * @return 协议是否为逆映射。
     */
    public boolean isInvPrp() {
        return invPrp;
    }

    /**
     * 返回密钥。
     *
     * @return 密钥。
     */
    public byte[] getKey() {
        return key;
    }

    public byte[] getShare(int index) {
        return shares[index];
    }

    public int getN() {
        return shares.length;
    }
}

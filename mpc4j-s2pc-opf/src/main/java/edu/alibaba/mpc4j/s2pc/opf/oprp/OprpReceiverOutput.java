package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * OPRP接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class OprpReceiverOutput {
    /**
     * PRP类型
     */
    private final PrpType prpType;
    /**
     * 是否为逆PRP运算
     */
    private final boolean invPrp;
    /**
     * 分享值
     */
    private final byte[][] shares;

    public OprpReceiverOutput(PrpType prpType, boolean invPrp, byte[][] shares) {
        this.prpType = prpType;
        this.invPrp = invPrp;
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

    public byte[] getShare(int index) {
        return shares[index];
    }

    public int getN() {
        return shares.length;
    }
}

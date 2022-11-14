package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * 核COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public abstract class AbstractCoreCotSender extends AbstractSecureTwoPartyPto implements CoreCotSender {
    /**
     * 配置项
     */
    private final CoreCotConfig config;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 关联值Δ的比特值
     */
    protected boolean[] deltaBinary;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractCoreCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, CoreCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public CoreCotFactory.CoreCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ byte length must be " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + delta.length;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta);
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        extraInfo++;
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory.NcCotType;

/**
 * NC-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public abstract class AbstractNcCotSender extends AbstractSecureTwoPartyPto implements NcCotSender {
    /**
     * 配置项
     */
    private final NcCotConfig config;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 数量
     */
    protected int num;

    protected AbstractNcCotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, NcCotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public NcCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int num) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        assert num > 0 && num <= config.maxAllowNum()
            : "num must be in range: (0, " + config.maxAllowNum() + "]: " + num;
        this.num = num;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }
}

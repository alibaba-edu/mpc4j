package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * NC-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public abstract class AbstractNcCotSender extends AbstractTwoPartyPto implements NcCotSender {
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

    protected void setInitInput(byte[] delta, int num) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}

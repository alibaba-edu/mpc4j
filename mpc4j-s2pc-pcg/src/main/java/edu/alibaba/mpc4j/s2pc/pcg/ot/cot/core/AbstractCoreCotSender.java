package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * 核COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public abstract class AbstractCoreCotSender extends AbstractTwoPartyPto implements CoreCotSender {
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
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;

/**
 * 核COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public abstract class AbstractCoreCotReceiver extends AbstractTwoPartyPto implements CoreCotReceiver {
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 选择比特
     */
    protected boolean[] choices;
    /**
     * 数量
     */
    protected int num;

    protected AbstractCoreCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, CoreCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(boolean[] choices) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", choices.length, maxNum);
        // 拷贝一份
        this.choices = BinaryUtils.clone(choices);
        num = choices.length;
        extraInfo++;
    }
}

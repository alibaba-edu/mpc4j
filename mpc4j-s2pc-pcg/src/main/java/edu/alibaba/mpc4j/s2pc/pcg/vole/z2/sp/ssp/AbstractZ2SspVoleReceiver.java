package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z2-SSP-VOLE协议接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public abstract class AbstractZ2SspVoleReceiver extends AbstractSecureTwoPartyPto implements Z2SspVoleReceiver {
    /**
     * 配置项
     */
    private final Z2SspVoleConfig config;
    /**
     * 关联值Δ
     */
    protected boolean delta;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 最大数量对数
     */
    protected int maxH;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;
    /**
     * 偏移量
     */
    protected int numOffset;
    /**
     * 数量对数
     */
    protected int h;

    protected AbstractZ2SspVoleReceiver(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Z2SspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public Z2SspVoleFactory.Z2SspVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(boolean delta, int maxNum) {
        this.delta = delta;
        assert maxNum > 0: "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        maxH = LongUtils.ceilLog2(maxNum);
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        numOffset = byteNum * Byte.SIZE - num;
        h = LongUtils.ceilLog2(num);
        extraInfo++;
    }

    protected void setPtoInput(int num, Z2VoleReceiverOutput preReceiverOutput) {
        setPtoInput(num);
        assert preReceiverOutput.getNum() >= Z2SspVoleFactory.getPrecomputeNum(config, num);
    }
}

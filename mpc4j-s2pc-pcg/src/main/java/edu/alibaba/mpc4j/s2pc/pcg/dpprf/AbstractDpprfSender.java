package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * DPPRF发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractDpprfSender extends AbstractSecureTwoPartyPto implements DpprfSender {
    /**
     * 配置项
     */
    private final DpprfConfig config;
    /**
     * 最大α上界
     */
    protected int maxAlphaBound;
    /**
     * 最大α比特长度
     */
    protected int maxH;
    /**
     * 最大批处理数量
     */
    protected int maxBatchNum;
    /**
     * α上界
     */
    protected int alphaBound;
    /**
     * α比特长度
     */
    protected int h;
    /**
     * 批处理数量
     */
    protected int batchNum;

    protected AbstractDpprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, DpprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public DpprfFactory.DpprfType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxBatchNum, int maxAlphaBound) {
        assert maxBatchNum > 0 : "maxBatchNum must be greater than 0:" + maxBatchNum;
        this.maxBatchNum = maxBatchNum;
        assert maxAlphaBound > 0 : "maxAlphaBound must be greater than 0: " + maxAlphaBound;
        this.maxAlphaBound = maxAlphaBound;
        maxH = LongUtils.ceilLog2(maxAlphaBound);
        initialized = false;
    }

    protected void setPtoInput(int batchNum, int alphaBound) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert batchNum > 0 && batchNum <= maxBatchNum : "BatchNum must be in range (0, " + maxBatchNum + "]: " + batchNum;
        this.batchNum = batchNum;
        assert alphaBound > 0 && alphaBound <= maxAlphaBound
            : "alphaBound must be in range (0, " + maxAlphaBound + "]: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound);
        extraInfo++;
    }

    protected void setPtoInput(int batch, int alphaBound, CotSenderOutput preSenderOutput) {
        setPtoInput(batch, alphaBound);
        assert preSenderOutput.getNum() >= DpprfFactory.getPrecomputeNum(config, batch, alphaBound);
    }
}

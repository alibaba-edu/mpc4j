package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * BSP-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractBspCotReceiver extends AbstractSecureTwoPartyPto implements BspCotReceiver {
    /**
     * 配置项
     */
    private final BspCotConfig config;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 最大批处理数量
     */
    private int maxBatchNum;
    /**
     * 单点索引值数组
     */
    protected int[] alphaArray;
    /**
     * 数量
     */
    protected int num;
    /**
     * 批处理数量
     */
    protected int batchNum;

    protected AbstractBspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public BspCotFactory.BspCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxBatchNum, int maxNum) {
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        assert maxBatchNum > 0 : "maxBatchNum must be greater than 0:" + maxBatchNum;
        this.maxBatchNum = maxBatchNum;
        initialized = false;
    }

    protected void setPtoInput(int[] alphaArray, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        batchNum = alphaArray.length;
        assert batchNum > 0 && batchNum <= maxBatchNum : "batch must be in range (0, " + maxBatchNum + "]: " + batchNum;
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> {
                assert alpha >= 0 && alpha < num : "α must be in range [0, " + num + "): " + alpha;
            })
            .toArray();
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, num);
        assert preReceiverOutput.getNum() >= BspCotFactory.getPrecomputeNum(config, alphaArray.length, num);
    }
}

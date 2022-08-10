package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z2-BSP-VOLE接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public abstract class AbstractZ2BspVoleReceiver extends AbstractSecureTwoPartyPto implements Z2BspVoleReceiver {
    /**
     * 配置项
     */
    private final Z2BspVoleConfig config;
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
     * 最大批处理数量
     */
    protected int maxBatch;
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
    /**
     * 批处理数量
     */
    protected int batch;

    protected AbstractZ2BspVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Z2BspVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public Z2BspVoleFactory.Z2BspVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(boolean delta, int maxBatch, int maxNum) {
        // 拷贝一份
        this.delta = delta;
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        maxH = LongUtils.ceilLog2(maxNum);
        assert maxBatch > 0 : "maxBatch must be greater than 0:" + maxBatch;
        this.maxBatch = maxBatch;
        initialized = false;
    }

    protected void setPtoInput(int batch, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        numOffset = byteNum * Byte.SIZE - num;
        h = LongUtils.ceilLog2(num);
        assert batch > 0 && batch <= maxBatch : "batch must be in range (0, " + maxBatch + "]: " + batch;
        this.batch = batch;
        // 一次并行处理m个数据
        extraInfo += batch;
    }

    protected void setPtoInput(int batch, int num, Z2VoleReceiverOutput preReceiverOutput) {
        setPtoInput(batch, num);
        assert preReceiverOutput.getNum() >= Z2BspVoleFactory.getPrecomputeNum(config, batch, num);
    }
}

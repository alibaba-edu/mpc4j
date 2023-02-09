package edu.alibaba.mpc4j.s2pc.aby.hamming;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * 汉明距离协议参与方。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public abstract class AbstractHammingParty extends AbstractSecureTwoPartyPto implements HammingParty {
    /**
     * 配置项
     */
    private final HammingConfig config;
    /**
     * 最大比特数量
     */
    protected int maxBitNum;
    /**
     * 当前比特数量
     */
    protected int bitNum;

    public AbstractHammingParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, HammingConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        maxBitNum = 0;
        bitNum = 0;
    }

    @Override
    public HammingFactory.HammingType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxBitNum) {
        assert maxBitNum > 0 && maxBitNum <= config.maxAllowBitNum()
            : "maxBitNum must be in range (0, " + config.maxAllowBitNum() + "]";
        this.maxBitNum = maxBitNum;
        initialized = false;
    }

    protected void setPtoInput(SquareSbitVector xi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() <= maxBitNum;
        bitNum = xi.bitNum();
        extraInfo++;
    }
}

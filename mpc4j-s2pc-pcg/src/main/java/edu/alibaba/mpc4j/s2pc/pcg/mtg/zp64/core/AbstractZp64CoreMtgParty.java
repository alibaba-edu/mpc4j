package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.util.stream.IntStream;

/**
 * 核zp64三元组生成协议参与方。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public abstract class AbstractZp64CoreMtgParty extends AbstractSecureTwoPartyPto implements Zp64CoreMtgParty {
    /**
     * 配置项
     */
    protected final Zp64CoreMtgConfig config;
    /**
     * 最大数量
     */
    protected int maxNum;
    /**
     * 数量
     */
    protected int num;

    public AbstractZp64CoreMtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Zp64CoreMtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    @Override
    public Zp64CoreMtgFactory.Zp64CoreMtgType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxNum) {
        assert maxNum <= config.maxAllowNum() : "maxNum must be in range (0, " + config.maxAllowNum() + "]: " + maxNum;
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

    /**
     * 生成随机数数组。
     *
     * @param num        数组长度。
     * @param upperBound 随机数上界。
     * @return 随机数数组。
     */
    protected long[] generateRandom(int num, long upperBound) {
        return IntStream.range(0, num)
            .mapToLong(i -> Math.abs(secureRandom.nextLong()) % upperBound)
            .toArray();
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;

import java.util.stream.IntStream;

/**
 * 核zp64三元组生成协议参与方。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public abstract class AbstractZp64CoreMtgParty extends AbstractTwoPartyPto implements Zp64CoreMtgParty {
    /**
     * 配置项
     */
    protected final Zp64CoreMtgConfig config;
    /**
     * the Zp64 instance
     */
    protected final Zp64 zp64;
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
        zp64 = config.getZp64();
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxNum", maxNum, config.maxAllowNum());
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num,  maxNum);
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

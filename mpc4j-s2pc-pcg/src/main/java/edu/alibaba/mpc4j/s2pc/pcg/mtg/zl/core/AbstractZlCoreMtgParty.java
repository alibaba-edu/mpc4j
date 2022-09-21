package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;

/**
 * 核l比特布三元组生成协议。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public abstract class AbstractZlCoreMtgParty extends AbstractSecureTwoPartyPto implements ZlCoreMtgParty {
    /**
     * 配置项
     */
    protected final ZlCoreMtgConfig config;
    /**
     * 比特长度
     */
    protected int l;
    /**
     * 取模所用的遮掩值
     */
    protected BigInteger mask;
    /**
     * 字节长度
     */
    protected int byteL;
    /**
     * 最大数量
     */
    protected int maxNum;
    /**
     * 数量
     */
    protected int num;

    public AbstractZlCoreMtgParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlCoreMtgConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        l = config.getL();
        mask = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        byteL = CommonUtils.getByteLength(l);
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxNum) {
        assert maxNum > 0 && maxNum <= config.maxAllowNum()
            : "maxNum must be in range (0, " + config.maxAllowNum() + "]: " + maxNum;
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
}

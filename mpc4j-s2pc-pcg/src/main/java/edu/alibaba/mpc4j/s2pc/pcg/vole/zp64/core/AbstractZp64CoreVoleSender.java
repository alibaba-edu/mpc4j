package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * ZP64-核VOLE协议发送方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public abstract class AbstractZp64CoreVoleSender extends AbstractSecureTwoPartyPto implements Zp64CoreVoleSender {
    /**
     * 配置项
     */
    private final Zp64CoreVoleConfig config;
    /**
     * Zp64
     */
    protected Zp64 zp64;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * x
     */
    protected long[] x;
    /**
     * 数量
     */
    protected int num;

    protected AbstractZp64CoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Zp64CoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public Zp64CoreVoleFactory.Zp64CoreVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(long prime, int maxNum) {
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        zp64 = Zp64Factory.createInstance(envType, prime);
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(long[] x) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert x.length > 0 & x.length <= maxNum : "num must be in range [0, " + maxNum + "): " + x.length;
        num = x.length;
        this.x = Arrays.stream(x)
                .peek(xi -> {
                    assert zp64.validateElement(xi) : "xi must be in range [0, " + zp64.getPrime() + "): " + xi;
                })
                .toArray();
        extraInfo++;
    }
}

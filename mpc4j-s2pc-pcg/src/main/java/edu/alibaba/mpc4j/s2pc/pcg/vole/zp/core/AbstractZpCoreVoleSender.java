package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * ZP-核VOLE协议发送方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/06/13
 */
public abstract class AbstractZpCoreVoleSender extends AbstractSecureTwoPartyPto implements ZpCoreVoleSender {
    /**
     * 配置项
     */
    private final ZpCoreVoleConfig config;
    /**
     * 素数域Zp
     */
    protected BigInteger prime;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * x
     */
    protected BigInteger[] x;
    /**
     * 数量
     */
    protected int num;

    protected AbstractZpCoreVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, ZpCoreVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public ZpCoreVoleFactory.ZpCoreVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(BigInteger prime, int maxNum) {
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        this.prime = prime;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(BigInteger[] x) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert x.length > 0 & x.length <= maxNum : "num must be in range [0, " + maxNum + "): " + x.length;
        num = x.length;
        this.x = Arrays.stream(x)
            .peek(xi -> {
                assert BigIntegerUtils.greaterOrEqual(xi, BigInteger.ZERO) && BigIntegerUtils.less(xi, prime)
                    : "xi must be in range [0, " + prime + "): " + xi;
            })
            .toArray(BigInteger[]::new);
        extraInfo++;
    }
}

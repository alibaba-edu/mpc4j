package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.ZpCoreVoleFactory.*;

import java.math.BigInteger;

/**
 * Zp-核VOLE接收方抽象类。
 *
 * @author Hanwen Feng
 * @date 2022/06/13
 */
public abstract class AbstractZpCoreVoleReceiver extends AbstractSecureTwoPartyPto implements ZpCoreVoleReceiver {
    /**
     * 配置项
     */
    private final ZpCoreVoleConfig config;
    /**
     * 关联值Δ
     */
    protected BigInteger delta;
    /**
     * 素数域Zp
     */
    protected Zp zp;
    /**
     * 有限域比特长度
     */
    protected int l;
    /**
     * 质数字节长度
     */
    protected int primeByteLength;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractZpCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, ZpCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public ZpCoreVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(BigInteger prime, BigInteger delta, int maxNum) {
        zp = ZpFactory.createInstance(envType, prime);
        l = zp.getL();
        primeByteLength = zp.getPrimeByteLength();
        assert zp.validateRangeElement(delta) : "Δ must be in range [0, " + zp.getRangeBound() + "): " + delta;
        this.delta = delta;
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        extraInfo++;
    }
}

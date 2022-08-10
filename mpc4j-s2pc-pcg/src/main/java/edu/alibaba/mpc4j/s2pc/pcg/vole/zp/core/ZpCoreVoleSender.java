package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;

import java.math.BigInteger;

/**
 * Zp-核VOLE协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/06/13
 */
public interface ZpCoreVoleSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    ZpCoreVoleFactory.ZpCoreVoleType getPtoType();

    /**
     * 初始化协议。
     *
     * @param prime 素数域。
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(BigInteger prime, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param x x。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    ZpVoleSenderOutput send(BigInteger[] x) throws MpcAbortException;
}

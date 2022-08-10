package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;

/**
 * Zp64-核VOLE协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public interface Zp64CoreVoleSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    Zp64CoreVoleFactory.Zp64CoreVoleType getPtoType();

    /**
     * 初始化协议。
     *
     * @param prime  素数域。
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(long prime, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param x x。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Zp64VoleSenderOutput send(long[] x) throws MpcAbortException;
}

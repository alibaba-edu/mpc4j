package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * GF2K-核VOLE协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public interface Gf2kCoreVoleSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    Gf2kCoreVoleFactory.Gf2kCoreVoleType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param x x。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Gf2kVoleSenderOutput send(byte[][] x) throws MpcAbortException;
}

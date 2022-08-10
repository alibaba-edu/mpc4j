package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

/**
 * Z_2-核VOLE协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public interface Z2CoreVoleSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    Z2CoreVoleFactory.Z2CoreVoleType getPtoType();

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
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2VoleSenderOutput send(byte[] x, int num) throws MpcAbortException;
}
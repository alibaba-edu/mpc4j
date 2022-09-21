package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotSenderOutput;

/**
 * NC-2^l选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/08/16
 */
public interface NcLotSender extends TwoPartyPto, SecurePto {
    /**
     * 返回NC-2^l选1-OT协议类型。
     *
     * @return NC-2^l选1-OT协议类型。
     */
    @Override
    NcLotFactory.NcLotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param inputBitLength 输入比特长度
     * @param num            执行数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int inputBitLength, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    LotSenderOutput send() throws MpcAbortException;
}

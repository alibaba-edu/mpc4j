package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory.NcCotType;

/**
 * NC-COT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface NcCotReceiver extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    NcCotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param num 数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CotReceiverOutput receive() throws MpcAbortException;
}

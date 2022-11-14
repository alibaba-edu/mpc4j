package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 索引PIR协议服务端接口。
 *
 * @author Liqiang Peng
 * @date 2022/8/10
 */
public interface IndexPirServer extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    IndexPirFactory.IndexPirType getPtoType();

    /**
     * 初始化协议。
     *
     * @param indexPirParams    索引PIR协议参数。
     * @param elementArrayList  元素列表。
     * @param elementByteLength 元素字节长度。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(IndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList, int elementByteLength)
        throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void pir() throws MpcAbortException;
}

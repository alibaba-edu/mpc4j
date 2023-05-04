package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * ZZL22-字节椭圆曲线mqRPMT协议信息。论文来源：
 * <p>
 * Chen, Yu, Min Zhang, Cong Zhang, and Minglang Dong. Private Set Operations from Multi-Query Reverse Private
 * Membership Test. Cryptology ePrint Archive (2022).
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
class Czz22ByteEccCwMqRpmtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 7216266257988855911L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CZZ22_BYTE_ECC_mqRPMT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送H(Y)^β
         */
        CLIENT_SEND_HY_BETA,
        /**
         * 服务端发送H(X)^α
         */
        SERVER_SEND_HX_ALPHA,
        /**
         * 服务端发送H(Y)^βα
         */
        CLIENT_SEND_HY_BETA_ALPHA,
    }

    /**
     * 单例模式
     */
    private static final Czz22ByteEccCwMqRpmtPtoDesc INSTANCE = new Czz22ByteEccCwMqRpmtPtoDesc();

    /**
     * 私有构造函数
     */
    private Czz22ByteEccCwMqRpmtPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }

    /**
     * 计算PEQT协议对比字节长度σ + log_2(serverSize) + long(clientSize)，转换为字节长度。
     *
     * @param serverElementSize 服务端元素数量。
     * @param clientElementSize 客户端元素数量。
     * @return PEQT协议对比长度。
     */
    static int getPeqtByteLength(int serverElementSize, int clientElementSize) {
        return CommonConstants.STATS_BYTE_LENGTH
            + CommonUtils.getByteLength(LongUtils.ceilLog2(serverElementSize))
            + CommonUtils.getByteLength(LongUtils.ceilLog2(clientElementSize));
    }
}

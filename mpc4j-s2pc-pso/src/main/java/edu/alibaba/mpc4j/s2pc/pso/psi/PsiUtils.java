package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * PSI协议工具类。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PsiUtils {

    private PsiUtils() {
        // empty
    }

    /**
     * 返回半诚实模型PEQT字节长度。
     *
     * @param serverElementSize 服务端元素数量。
     * @param clientElementSize 客户端元素数量。
     * @return PEQT字节长度。
     */
    public static int getSemiHonestPeqtByteLength(int serverElementSize, int clientElementSize) {
        assert serverElementSize > 0 : "server element size must be greater than 0: " + serverElementSize;
        assert clientElementSize > 0 : "client element size must be greater than 0: " + clientElementSize;
        // λ + log(m) + log(n)
        return CommonConstants.STATS_BYTE_LENGTH
            + CommonUtils.getByteLength(LongUtils.ceilLog2(serverElementSize))
            + CommonUtils.getByteLength(LongUtils.ceilLog2(clientElementSize));
    }

    /**
     * 返回恶意模型下PEQT字节长度。
     *
     * @param serverElementSize 服务端元素数量。
     * @param clientElementSize 客户端元素数量。
     * @return PEQT字节长度。
     */
    public static int getMaliciousPeqtByteLength(int serverElementSize, int clientElementSize) {
        assert serverElementSize > 0 : "server element size must be greater than 0: " + serverElementSize;
        assert clientElementSize > 0 : "client element size must be greater than 0: " + clientElementSize;
        /*
         * PRTY19论文建议输出比特长度为2*计算安全常数，CM20论文给出log_2(Q_2 * n_2) + 统计安全常数，其中Q_2为攻击者可问询PRF的最大次数
         * CM20的实现假定Q_2 = 2^64，因此结果变为log_2(n) + λ + 64。
         */
        return CommonConstants.STATS_BYTE_LENGTH + 64 + LongUtils.ceilLog2(clientElementSize);
    }
}

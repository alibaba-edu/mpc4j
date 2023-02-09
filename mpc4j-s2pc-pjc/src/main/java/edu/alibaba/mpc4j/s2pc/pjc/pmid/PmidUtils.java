package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * PMID协议工具类。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PmidUtils {

    private PmidUtils() {
        // empty
    }

    /**
     * 返回PMID字节长度。
     *
     * @param serverSetSize 服务端元素数量。
     * @param serverU 服务端最大重数。
     * @param clientSetSize 客户端元素数量。
     * @param clientU 客户端最大重数。
     * @return PID字节长度。
     */
    public static int getPmidByteLength(int serverSetSize, int serverU, int clientSetSize, int clientU) {
        // PMID字节长度等于λ + log(m * serverU * clientU) + log(n * serverU * clientU)
        return CommonConstants.STATS_BYTE_LENGTH
            + CommonUtils.getByteLength(LongUtils.ceilLog2((long) serverSetSize * serverU * clientU))
            + CommonUtils.getByteLength(LongUtils.ceilLog2((long) clientSetSize * serverU * clientU));
    }
}

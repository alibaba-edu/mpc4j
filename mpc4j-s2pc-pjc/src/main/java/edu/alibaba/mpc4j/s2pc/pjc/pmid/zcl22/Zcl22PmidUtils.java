package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * ZCL22-PMID协议工具类。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Zcl22PmidUtils {

    private Zcl22PmidUtils() {
        // empty
    }

    /**
     * 返回σ-OKVS值长度。
     *
     * @param serverSetSize 服务端元素数量。
     * @param serverU 服务端最大重数。
     * @param clientSetSize 客户端元素数量。
     * @param clientU 客户端最大重数。
     * @return σ-OKVS值长度。
     */
    static int getSigmaOkvsValueByteLength(int serverSetSize, int serverU, int clientSetSize, int clientU) {
        // σ的OKVS值长度 = λ + Max{log(m * clientU), log(n * serverU)}
        return CommonConstants.STATS_BYTE_LENGTH + Math.max(
            LongUtils.ceilLog2((long) clientU * serverSetSize), LongUtils.ceilLog2((long) serverU * clientSetSize)
        );
    }
}

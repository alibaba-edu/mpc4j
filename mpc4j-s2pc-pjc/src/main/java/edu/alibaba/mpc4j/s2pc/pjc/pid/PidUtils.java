package edu.alibaba.mpc4j.s2pc.pjc.pid;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * PID协议工具类。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PidUtils {

    private PidUtils() {
        // empty
    }

    /**
     * 全局PID字节长度
     */
    public static final int GLOBAL_PID_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;

    /**
     * 返回PID字节长度。
     *
     * @param serverSetSize 服务端元素数量。
     * @param clientSetSize 客户端元素数量。
     * @return PID字节长度。
     */
    public static int getPidByteLength(int serverSetSize, int clientSetSize) {
        return CommonConstants.STATS_BYTE_LENGTH
            + CommonUtils.getByteLength(LongUtils.ceilLog2(serverSetSize))
            + CommonUtils.getByteLength(LongUtils.ceilLog2(clientSetSize));
    }
}

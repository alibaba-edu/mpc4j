package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * JSZ22-SFC-PSU协议信息。置乱服务端的PSU协议，适用于服务端元素数量较少的场景。论文来源：
 * Jia, Yanxue, Shi-Feng Sun, Hong-Sheng Zhou, Jiajun Du, and Dawu Gu. Shuffle-based Private Set Union: Faster and More
 * Secure. Cryptology ePrint Archive (2022), to appear in USENIX Security 2022.
 *
 * @author Weiran Liu
 * @date 2022/03/18
 */
class Jsz22SfsPsuPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)1774625121230472499L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "JSZ22_SFS_PSU";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送布谷鸟哈希密钥
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * 客户端发送OPRF值
         */
        CLIENT_SEND_OPRFS,
        /**
         * 服务端发送Z
         */
        SERVER_SEND_ZS,
    }

    /**
     * 单例模式
     */
    private static final Jsz22SfsPsuPtoDesc INSTANCE = new Jsz22SfsPsuPtoDesc();

    /**
     * 私有构造函数
     */
    private Jsz22SfsPsuPtoDesc() {
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
     * 计算PEQT协议对比字节长度σ + log_2(maxBinSize^2 * binNum)，转换为字节长度。
     *
     * @param binNum     桶数量（β）。
     * @param maxBinSize 最大桶大小（m）。
     * @return PEQT协议对比长度
     */
    static int getOprfByteLength(int binNum, int maxBinSize) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            (int) (DoubleUtils.log2(Math.pow(maxBinSize, 2) * binNum))
        );
    }
}

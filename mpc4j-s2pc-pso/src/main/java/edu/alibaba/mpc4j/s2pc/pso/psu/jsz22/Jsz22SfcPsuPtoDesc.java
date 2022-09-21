package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * JSZ22-SFC-PSU协议信息。置乱客户端的PSU协议，适用于客户端元素数量较少的场景。论文来源：
 * Jia, Yanxue, Shi-Feng Sun, Hong-Sheng Zhou, Jiajun Du, and Dawu Gu. Shuffle-based Private Set Union: Faster and More
 * Secure. Cryptology ePrint Archive (2022), to appear in USENIX Security 2022.
 *
 * @author Weiran Liu
 * @date 2022/03/14
 */
class Jsz22SfcPsuPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)9188479015271825020L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "JSZ22_SFC_PSU";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送布谷鸟哈希密钥
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * 服务端发送OPRF值
         */
        SERVER_SEND_OPRFS,
        /**
         * 服务端发送加密元素
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * 单例模式
     */
    private static final Jsz22SfcPsuPtoDesc INSTANCE = new Jsz22SfcPsuPtoDesc();

    /**
     * 私有构造函数
     */
    private Jsz22SfcPsuPtoDesc() {
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
     * 计算PEQT协议对比字节长度σ + 2 * log_2(binNum)，转换为字节长度。
     *
     * @param binNum 桶数量（β）。
     * @return PEQT协议对比长度。
     */
    static int getOprfByteLength(int binNum) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(2 * (int) (DoubleUtils.log2(binNum)));
    }
}

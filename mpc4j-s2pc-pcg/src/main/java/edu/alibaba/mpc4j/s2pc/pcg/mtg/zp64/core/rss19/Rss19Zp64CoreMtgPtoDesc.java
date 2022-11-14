package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RSS19-核Zp64三元组生成协议信息。论文来源：
 * <p>
 * Rathee, Deevashwer, Thomas Schneider, and K. K. Shukla. Improved multiplication triple generation over rings via
 * RLWE-based AHE. ACNS 2019, pp. 347-359. Springer, Cham, 2019.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Rss19Zp64CoreMtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 3236563065155941099L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RSS19_ZP64_MTG";

    /**
     * 单例模式
     */
    private static final Rss19Zp64CoreMtgPtoDesc INSTANCE = new Rss19Zp64CoreMtgPtoDesc();

    /**
     * 私有构造函数
     */
    private Rss19Zp64CoreMtgPtoDesc() {
        // empty
    }

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送加密方案参数
         */
        SENDER_SEND_ENCRYPTION_PARAMS,
        /**
         * 接收方发送密文ct_d
         */
        RECEIVER_SEND_CT_D,
        /**
         * 发送方发送密文ct_a, ct_b
         */
        SENDER_SEND_CT_A_CT_B,
    }

    /**
     * 返回默认多项式阶。
     *
     * @param plainModulusSize 明文模数比特长度。
     * @return 默认多项式阶。
     */
    public static int defaultPolyModulusDegree(int plainModulusSize) {
        if (plainModulusSize == 14) {
            return 2048;
        } else if (plainModulusSize >= 16 && plainModulusSize <= 22) {
            return 2048;
        } else if (plainModulusSize >= 23 && plainModulusSize <= 28) {
            return 4096;
        } else if (plainModulusSize >= 29 && plainModulusSize <= 34) {
            return 8192;
        } else if (plainModulusSize >= 35 && plainModulusSize <= 39) {
            return 16384;
        } else if (plainModulusSize >= 40 && plainModulusSize <= 44) {
            return 32768;
        } else if (plainModulusSize >= 45 && plainModulusSize <= 58) {
            return 8192;
        } else if (plainModulusSize >= 59 && plainModulusSize <= 60) {
            return 16384;
        } else {
            throw new IllegalStateException("Unexpected value: " + plainModulusSize);
        }
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
}

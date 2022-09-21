package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;

/**
 * CRR21-NC-COT协议信息。论文来源：
 * <p>
 * Geoffroy Couteau, Peter Rindal, Srinivasan Raghuraman. Silver: Silent VOLE and Oblivious Transfer from Hardness of
 * Decoding Structured LDPC Codes. CRYPTO 2021, pp. 502-534. 2021.
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/02/18
 */
class Crr21NcCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 1771187348633492817L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CRR21_NC_COT";

    /**
     * 单例模式
     */
    private static final Crr21NcCotPtoDesc INSTANCE = new Crr21NcCotPtoDesc();

    /**
     * 私有构造函数
     */
    private Crr21NcCotPtoDesc() {
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
     * 单次输出支持的最小COT数量
     */
    static final int MIN_LOG_N = LdpcCreatorUtils.MIN_LOG_N;
    /**
     * 单次输出支持的最大COT数量
     */
    static final int MAX_LOG_N = LdpcCreatorUtils.MAX_LOG_N;

    /**
     * 返回初始化LPN参数。
     *
     * @param config 配置项。
     * @param num    数量。
     * @return 初始化LPN参数。
     */
    static LpnParams getLpnParams(MspCotConfig config, LdpcCreatorUtils.CodeType silverCodeType, int num) {
        int ceilLogN = LongUtils.ceilLog2(num);
        assert ceilLogN <= MAX_LOG_N : "log(num) must be less or equal than " + MAX_LOG_N + ": " + ceilLogN;
        if (ceilLogN < MIN_LOG_N) {
            ceilLogN = MIN_LOG_N;
        }
        MspCotFactory.MspCotType mspCotType = config.getPtoType();
        switch (mspCotType) {
            case BCG19_REG:
            case YWL20_UNI:
                return LdpcCreatorUtils.getLpnParams(ceilLogN, silverCodeType);
            default: throw new IllegalArgumentException("Invalid MspCot Type :" + mspCotType.name());
        }

    }
}

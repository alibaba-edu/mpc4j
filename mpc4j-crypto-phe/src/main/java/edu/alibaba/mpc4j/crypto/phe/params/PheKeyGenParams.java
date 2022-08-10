package edu.alibaba.mpc4j.crypto.phe.params;

import edu.alibaba.mpc4j.crypto.phe.PheSecLevel;

/**
 * 半同态加密密钥生成参数。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public class PheKeyGenParams {
    /**
     * The default base value.
     */
    private static final int DEFAULT_BASE = 16;
    /**
     * 半同态加密安全级别
     */
    private final PheSecLevel pheSecLevel;
    /**
     * 是否为有符号编码
     */
    private final boolean signed;
    /**
     * 编码精度
     */
    private final int precision;
    /**
     * 底数
     */
    private final int base;

    /**
     * 以默认底数{@code DEFAULT_BASE}构造半同态加密密钥生成参数。
     *
     * @param pheSecLevel 半同态加密安全级别。
     * @param signed      是否支持有符号明文。
     * @param precision   精度。
     */
    public PheKeyGenParams(PheSecLevel pheSecLevel, boolean signed, int precision) {
        this(pheSecLevel, signed, precision, DEFAULT_BASE);
    }

    /**
     * 构造半同态加密密钥生成参数。
     *
     * @param pheSecLevel 半同态加密安全级别。
     * @param signed      是否支持有符号明文。
     * @param precision   精度。
     * @param base        底数。
     */
    public PheKeyGenParams(PheSecLevel pheSecLevel, boolean signed, int precision, int base) {
        this.pheSecLevel = pheSecLevel;
        this.signed = signed;
        this.precision = precision;
        this.base = base;
    }

    public PheSecLevel getPheSecLevel() {
        return pheSecLevel;
    }

    public boolean isSigned() {
        return signed;
    }

    public int getPrecision() {
        return precision;
    }

    public int getBase() {
        return base;
    }
}

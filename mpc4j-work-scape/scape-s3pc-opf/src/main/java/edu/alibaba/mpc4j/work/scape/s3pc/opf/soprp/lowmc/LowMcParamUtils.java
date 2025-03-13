package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc;

/**
 * the parameter utils of lowmc prp
 * todo should move the python code here
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class LowMcParamUtils {
    public static LowMcParam getParam(int bitLen, int dataNum, int statisticParam) {
        assert dataNum <= 1 << 30 && statisticParam <= 40;
        return switch (bitLen) {
            case 64 -> new LowMcParam(13, 64, 12);
            case 80 -> new LowMcParam(14, 80, 13);
            default -> throw new IllegalArgumentException("illegal bit length of input data");
        };
    }
}

package edu.alibaba.mpc4j.work.payable.main.psi;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiConfig;
import edu.alibaba.mpc4j.work.payable.psi.baseline.BaselinePayablePsiConfig;
import edu.alibaba.mpc4j.work.payable.psi.zlp24.Zlp24PayablePsiConfig;

import java.util.Properties;

/**
 * Payable PSI config utils.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class PayablePsiConfigUtils {

    /**
     * private constructor.
     */
    private PayablePsiConfigUtils() {
        // empty
    }

    public static PayablePsiConfig createPayablePsiConfig(Properties properties) {
        // read protocol type
        PayablePsiMainType payablePsiMainType = MainPtoConfigUtils.readEnum(
            PayablePsiMainType.class, properties, PayablePsiMain.PTO_NAME_KEY
        );
        return switch (payablePsiMainType) {
            case ZLP24 -> new Zlp24PayablePsiConfig.Builder().build();
            case BASELINE -> new BaselinePayablePsiConfig.Builder().build();
        };
    }
}
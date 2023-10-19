package edu.alibaba.mpc4j.s2pc.pso.main.payablepsi;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.PayablePsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.PayablePsiFactory.PayablePsiType;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23.Zlp23PayablePsiConfig;

import java.util.Properties;

/**
 * Payable PSI config utilities.
 *
 * @author Liqiang Peng
 * @date 2023/9/28
 */
public class PayablePsiConfigUtils {
    /**
     * private constructor.
     */
    private PayablePsiConfigUtils() {
        // empty
    }

    public static PayablePsiConfig createPayablePsiConfig(Properties properties) {
        // read PSI type
        String payablePsiTypeString = PropertiesUtils.readString(properties, "payable_psi_pto_name");
        PayablePsiType payablePsiType = PayablePsiType.valueOf(payablePsiTypeString);
        if (payablePsiType == PayablePsiType.ZLP23) {
            return new Zlp23PayablePsiConfig.Builder().build();
        }
        throw new IllegalArgumentException(
            "Invalid " + PayablePsiType.class.getSimpleName() + ": " + payablePsiType.name()
        );
    }
}

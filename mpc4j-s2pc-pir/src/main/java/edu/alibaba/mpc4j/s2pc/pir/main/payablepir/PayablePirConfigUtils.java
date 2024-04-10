package edu.alibaba.mpc4j.s2pc.pir.main.payablepir;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirFactory.PayablePirType;
import edu.alibaba.mpc4j.s2pc.pir.payable.zlp23.Zlp23PayablePirConfig;

import java.util.Properties;

/**
 * Payable PIR protocol config utils.
 *
 * @author Liqiang Peng
 * @date 2023/9/28
 */
public class PayablePirConfigUtils {

    private PayablePirConfigUtils() {
        // empty
    }

    public static PayablePirConfig createPayablePirConfig(Properties properties) {
        // read protocol type
        String payablePirTypeString = PropertiesUtils.readString(properties, "pto_name");
        PayablePirType payablePirType = PayablePirType.valueOf(payablePirTypeString);
        if (payablePirType == PayablePirType.ZLP23) {
            return new Zlp23PayablePirConfig.Builder().build();
        }
        throw new IllegalArgumentException(
            "Invalid " + PayablePirType.class.getSimpleName() + ": " + payablePirType.name()
        );
    }
}
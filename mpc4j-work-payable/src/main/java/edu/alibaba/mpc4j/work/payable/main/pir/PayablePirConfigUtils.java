package edu.alibaba.mpc4j.work.payable.main.pir;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.work.payable.pir.PayablePirConfig;
import edu.alibaba.mpc4j.work.payable.pir.baseline.BaselinePayablePirConfig;
import edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirConfig;

import java.util.Properties;

/**
 * Payable PIR config utils.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class PayablePirConfigUtils {

    private PayablePirConfigUtils() {
        // empty
    }

    public static PayablePirConfig createPayablePirConfig(Properties properties) {
        // read protocol type
        PayablePirMainType payablePirMainType = MainPtoConfigUtils.readEnum(
            PayablePirMainType.class, properties, PayablePirMain.PTO_NAME_KEY
        );
        return switch (payablePirMainType) {
            case ZLP24 -> new Zlp24PayablePirConfig.Builder().build();
            case BASELINE -> new BaselinePayablePirConfig.Builder().build();
        };
    }
}
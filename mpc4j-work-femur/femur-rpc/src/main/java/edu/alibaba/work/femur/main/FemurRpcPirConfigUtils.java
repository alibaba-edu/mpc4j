package edu.alibaba.work.femur.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.work.femur.FemurRpcPirConfig;
import edu.alibaba.work.femur.FemurSealPirParams;
import edu.alibaba.work.femur.naive.NaiveFemurRpcPirConfig;
import edu.alibaba.work.femur.seal.SealFemurRpcPirConfig;

import java.util.Properties;

import static edu.alibaba.work.femur.FemurRpcPirFactory.FemurPirType;


/**
 * PGM-index range keyword PIR config utilities.
 *
 * @author Liqiang Peng
 * @date 2024/9/18
 */
public class FemurRpcPirConfigUtils {
    /**
     * private constructor.
     */
    private FemurRpcPirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static FemurRpcPirConfig createConfig(Properties properties, boolean dp) {
        FemurPirType rangePirType = MainPtoConfigUtils.readEnum(FemurPirType.class, properties, FemurRpcPirMain.PTO_NAME_KEY);
        return switch (rangePirType) {
            case PGM_INDEX_SEAL_PIR -> new SealFemurRpcPirConfig.Builder()
                .setParams(new FemurSealPirParams(4096, 20, 2))
                .setDp(dp)
                .build();
            case PGM_INDEX_NAIVE_PIR -> new NaiveFemurRpcPirConfig.Builder().setDp(dp).build();
        };
    }
}
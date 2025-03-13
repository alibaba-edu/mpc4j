package edu.alibaba.femur.service.main;


import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.work.femur.FemurSealPirParams;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirType;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoRedisPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoRedisPirConfig;
import redis.clients.jedis.Protocol;

import java.util.Properties;

/**
 * Femur PIR config utilities.
 *
 * @author Liqiang Peng
 * @date 2025/1/9
 */
public class FemurPirConfigUtils {
    /**
     * private constructor
     */
    private FemurPirConfigUtils() {
        // empty
    }

    public static FemurDemoPirConfig createConfig(Properties properties, String ptoName) {
        FemurDemoPirType femurPirType = MainPtoConfigUtils.readEnum(FemurDemoPirType.class, properties, ptoName);
        boolean dp = PropertiesUtils.readBoolean(properties, "differential_privacy");
        int pgmIndexLeafEpsilon = PropertiesUtils.readInt(properties, "pgm_index_leaf_epsilon");
        int redisPort = properties.getProperty("redis_port") == null ? 6379 : PropertiesUtils.readInt(properties, "redis_port");
        String redisHost = properties.getProperty("redis_host") == null ? "127.0.0.1" : PropertiesUtils.readString(properties, "redis_host");
        int timeout = properties.getProperty("time_out") == null ? Protocol.DEFAULT_TIMEOUT : PropertiesUtils.readInt(properties, "time_out");
        return switch (femurPirType) {
            case SEAL_REDIS -> new SealFemurDemoRedisPirConfig.Builder()
                .setParams(new FemurSealPirParams(4096, 20, 2))
                .setDp(dp)
                .setPgmIndexLeafEpsilon(pgmIndexLeafEpsilon)
                .setRedis(redisHost, redisPort, timeout)
                .build();
            case NAIVE_REDIS -> new NaiveFemurDemoRedisPirConfig.Builder()
                .setDp(dp)
                .setPgmIndexLeafEpsilon(pgmIndexLeafEpsilon)
                .setRedis(redisHost, redisPort, timeout)
                .build();
            case SEAL_MEMORY -> new SealFemurDemoMemoryPirConfig.Builder()
                .setParams(new FemurSealPirParams(4096, 20, 2))
                .setDp(dp)
                .setPgmIndexLeafEpsilon(pgmIndexLeafEpsilon)
                .build();
            case NAIVE_MEMORY -> new NaiveFemurDemoMemoryPirConfig.Builder()
                .setDp(dp)
                .setPgmIndexLeafEpsilon(pgmIndexLeafEpsilon)
                .build();
        };
    }
}

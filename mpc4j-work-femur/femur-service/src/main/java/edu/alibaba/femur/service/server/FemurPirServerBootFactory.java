package edu.alibaba.femur.service.server;


import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirType;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoRedisPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoRedisPirConfig;

/**
 * Femur PIR server boot factory.
 *
 * @author Weiran Liu
 * @date 2024/12/11
 */
public class FemurPirServerBootFactory {

    public static FemurPirServerBoot getInstance(String host, int port, FemurDemoPirConfig config) {
        FemurDemoPirType type = config.getPtoType();
        switch (type) {
            case NAIVE_MEMORY -> {
                return FemurNaiveMemoryPirServerBoot.of(host, port, (NaiveFemurDemoMemoryPirConfig) config);
            }
            case NAIVE_REDIS -> {
                return FemurNaiveRedisPirServerBoot.of(host, port, (NaiveFemurDemoRedisPirConfig) config);
            }
            case SEAL_MEMORY -> {
                return FemurSealMemoryPirServerBoot.of(host, port, (SealFemurDemoMemoryPirConfig) config);
            }
            case SEAL_REDIS -> {
                return FemurSealRedisPirServerBoot.of(host, port, (SealFemurDemoRedisPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Not implemented");
        }
    }
}

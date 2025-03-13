package edu.alibaba.work.femur.demo;

import edu.alibaba.work.femur.demo.naive.*;
import edu.alibaba.work.femur.demo.seal.*;

/**
 * Femur demo PIR factory.
 *
 * @author Liqiang Peng
 * @date 2024/12/3
 */
public class FemurDemoPirFactory {
    /**
     * private constructor.
     */
    private FemurDemoPirFactory() {
        // empty
    }

    /**
     * create a server.
     *
     * @param config config.
     * @return a server.
     */
    public static FemurDemoPirServer createServer(FemurDemoPirConfig config) {
        FemurDemoPirType type = config.getPtoType();
        switch (type) {
            case NAIVE_MEMORY -> {
                return new NaiveFemurDemoMemoryPirServer((NaiveFemurDemoMemoryPirConfig) config);
            }
            case SEAL_MEMORY -> {
                return new SealFemurDemoMemoryPirServer((SealFemurDemoMemoryPirConfig) config);
            }
            case NAIVE_REDIS -> {
                return new NaiveFemurDemoRedisPirServer((NaiveFemurDemoRedisPirConfig) config);
            }
            case SEAL_REDIS -> {
                return new SealFemurDemoRedisPirServer((SealFemurDemoRedisPirConfig) config);
            }
            default -> throw new IllegalArgumentException(
                "Invalid " + FemurDemoPirType.class.getSimpleName() + ": " + type.name()
            );
        }
    }

    /**
     * create a client.
     *
     * @param config config.
     * @return a client.
     */
    public static FemurDemoPirClient createClient(FemurDemoPirConfig config) {
        FemurDemoPirType type = config.getPtoType();
        switch (type) {
            case NAIVE_MEMORY -> {
                return new NaiveFemurDemoMemoryPirClient((NaiveFemurDemoMemoryPirConfig) config);
            }
            case SEAL_MEMORY -> {
                return new SealFemurDemoMemoryPirClient((SealFemurDemoMemoryPirConfig) config);
            }
            case NAIVE_REDIS -> {
                return new NaiveFemurDemoRedisPirClient((NaiveFemurDemoRedisPirConfig) config);
            }
            case SEAL_REDIS -> {
                return new SealFemurDemoRedisPirClient((SealFemurDemoRedisPirConfig) config);
            }
            default -> throw new IllegalArgumentException(
                "Invalid " + FemurDemoPirType.class.getSimpleName() + ": " + type.name()
            );
        }
    }
}

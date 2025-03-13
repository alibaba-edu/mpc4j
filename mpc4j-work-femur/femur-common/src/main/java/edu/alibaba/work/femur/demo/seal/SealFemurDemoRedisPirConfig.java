package edu.alibaba.work.femur.demo.seal;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.work.femur.FemurSealPirParams;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirType;
import redis.clients.jedis.Protocol;

/**
 * SEAL Femur demo PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class SealFemurDemoRedisPirConfig implements FemurDemoPirConfig {
    /**
     * host
     */
    private final String host;
    /**
     * port
     */
    private final int port;
    /**
     * time out
     */
    private final int timeout;
    /**
     * SEAL PIR params
     */
    private final FemurSealPirParams params;
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public SealFemurDemoRedisPirConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.timeout = builder.timeout;
        this.params = builder.params;
        this.dp = builder.dp;
        pgmIndexLeafEpsilon = builder.pgmIndexLeafEpsilon;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public FemurDemoPirType getPtoType() {
        return FemurDemoPirType.SEAL_REDIS;
    }

    /**
     * Returns SEAL PIR params.
     *
     * @return SEAL PIR params.
     */
    public FemurSealPirParams getParams() {
        return params;
    }

    /**
     * Returns whether to use differential privacy when querying the range.
     *
     * @return true if using differential privacy; false otherwise.
     */
    public boolean isDp() {
        return dp;
    }

    /**
     * Returns epsilon range used to build this index.
     *
     * @return epsilon range used to build this index.
     */
    public int getPgmIndexLeafEpsilon() {
        return pgmIndexLeafEpsilon;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SealFemurDemoRedisPirConfig> {
        /**
         * host
         */
        private String host;
        /**
         * port
         */
        private int port;
        /**
         * time out
         */
        private int timeout;
        /**
         * SEAL PIR params
         */
        private FemurSealPirParams params;
        /**
         * whether to use differential privacy
         */
        private boolean dp;
        /**
         * epsilon range used to build this index
         */
        private int pgmIndexLeafEpsilon;

        public Builder() {
            host = "127.0.0.1";
            port = 6379;
            timeout = Protocol.DEFAULT_TIMEOUT;
            params = FemurSealPirParams.DEFAULT_PARAMS;
            pgmIndexLeafEpsilon = CommonConstants.PGM_INDEX_LEAF_EPSILON;
            dp = false;
        }

        public Builder setRedis(String host, int port, int timeout) {
            this.host = host;
            this.port = port;
            this.timeout = timeout;
            return this;
        }

        public Builder setParams(FemurSealPirParams params) {
            this.params = params;
            return this;
        }

        public Builder setDp(boolean dp) {
            this.dp = dp;
            return this;
        }

        public Builder setPgmIndexLeafEpsilon(int pgmIndexLeafEpsilon) {
            this.pgmIndexLeafEpsilon = pgmIndexLeafEpsilon;
            return this;
        }

        @Override
        public SealFemurDemoRedisPirConfig build() {
            return new SealFemurDemoRedisPirConfig(this);
        }
    }
}


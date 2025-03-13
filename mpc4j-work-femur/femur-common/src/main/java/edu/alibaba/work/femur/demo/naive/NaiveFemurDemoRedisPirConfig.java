package edu.alibaba.work.femur.demo.naive;


import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirType;
import redis.clients.jedis.Protocol;

/**
 * Naive Femur demo PIR config.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
public class NaiveFemurDemoRedisPirConfig implements FemurDemoPirConfig {
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
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public NaiveFemurDemoRedisPirConfig(Builder builder) {
        host = builder.host;
        port = builder.port;
        timeout = builder.timeout;
        dp = builder.dp;
        pgmIndexLeafEpsilon = builder.pgmIndexLeafEpsilon;
    }

    @Override
    public FemurDemoPirType getPtoType() {
        return FemurDemoPirType.NAIVE_REDIS;
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

    /**
     * Returns whether to use differential privacy when querying the range.
     *
     * @return true if using differential privacy; false otherwise.
     */
    public boolean getDp() {
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveFemurDemoRedisPirConfig> {
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
            dp = false;
            pgmIndexLeafEpsilon = CommonConstants.PGM_INDEX_LEAF_EPSILON;
        }

        public Builder setRedis(String host, int port, int timeout) {
            this.host = host;
            this.port = port;
            this.timeout = timeout;
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
        public NaiveFemurDemoRedisPirConfig build() {
            return new NaiveFemurDemoRedisPirConfig(this);
        }
    }
}

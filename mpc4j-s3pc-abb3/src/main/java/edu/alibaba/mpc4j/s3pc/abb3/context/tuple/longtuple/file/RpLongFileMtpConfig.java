package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgFactory;

/**
 * configure of replicated 3p sharing zl64 mt provider in file mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpLongFileMtpConfig extends AbstractMultiPartyPtoConfig implements RpLongMtpConfig {
    /**
     * mtg config
     */
    private final FilePtoWorkType ptoWorkType;
    /**
     * mtg config
     */
    private final String fileDir;
    /**
     * mtg config
     */
    private final RpLongEnvConfig rpLongEnvConfig;
    /**
     * mtg config
     */
    private final RpLongMtgConfig rpLongMtgConfig;

    public RpLongFileMtpConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        ptoWorkType = builder.ptoWorkType;
        fileDir = builder.fileDir;
        rpLongEnvConfig = builder.rpLongEnvConfig;
        rpLongMtgConfig = builder.rpLongMtgConfig;
    }

    @Override
    public MtProviderType getProviderType() {
        return MtProviderType.FILE;
    }

    public FilePtoWorkType getPtoWorkType() {
        return ptoWorkType;
    }

    public String getFileDir() {
        return fileDir;
    }

    public RpLongEnvConfig getRpZl64EnvConfig() {
        return rpLongEnvConfig;
    }

    public RpLongMtgConfig getRpZl64MtgConfig() {
        return rpLongMtgConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RpLongFileMtpConfig> {
        /**
         * mtg config
         */
        private final FilePtoWorkType ptoWorkType;
        /**
         * mtg config
         */
        private final String fileDir;
        /**
         * mtg config
         */
        private final RpLongEnvConfig rpLongEnvConfig;
        /**
         * mtg config
         */
        private RpLongMtgConfig rpLongMtgConfig;

        public Builder(FilePtoWorkType ptoWorkType, String fileDir) {
            this.ptoWorkType = ptoWorkType;
            this.fileDir = fileDir;
            rpLongEnvConfig = new RpLongEnvConfig.Builder().build();
            rpLongMtgConfig = RpLongMtgFactory.createDefaultConfig();
        }

        public void setRpZ2MtgConfig(RpLongMtgConfig rpLongMtgConfig) {
            this.rpLongMtgConfig = rpLongMtgConfig;
        }

        @Override
        public RpLongFileMtpConfig build() {
            return new RpLongFileMtpConfig(this);
        }
    }
}

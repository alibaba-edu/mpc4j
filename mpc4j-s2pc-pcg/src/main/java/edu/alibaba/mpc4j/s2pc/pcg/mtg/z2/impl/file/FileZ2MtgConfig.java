package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * 文件布尔三元组生成配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/22
 */
public class FileZ2MtgConfig implements Z2MtgConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 文件存储路径
     */
    private final String filePath;
    /**
     * 安全模型
     */
    private final SecurityModel securityModel;

    private FileZ2MtgConfig(Builder builder) {
        securityModel = builder.securityModel;
        envType = builder.envType;
        filePath = builder.filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public Z2MtgFactory.Z2MtgType getPtoType() {
        return Z2MtgFactory.Z2MtgType.FILE;
    }

    @Override
    public int maxBaseNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<FileZ2MtgConfig> {
        /**
         * 安全模型
         */
        private final SecurityModel securityModel;
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 文件路径
         */
        private String filePath;

        public Builder(SecurityModel securityModel) {
            assert securityModel.compareTo(SecurityModel.SEMI_HONEST) <= 0
                : "Only support Security Model less than or equal to " + SecurityModel.SEMI_HONEST + ": " + securityModel;
            this.securityModel = securityModel;
            envType = EnvType.STANDARD;
            filePath = ".";
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        @Override
        public FileZ2MtgConfig build() {
            return new FileZ2MtgConfig(this);
        }
    }
}

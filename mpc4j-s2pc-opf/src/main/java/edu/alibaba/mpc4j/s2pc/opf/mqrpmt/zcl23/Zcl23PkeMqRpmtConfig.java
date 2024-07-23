package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory.MqRpmtType;

/**
 * ZCL23 PKE-mqRPMT config.
 *
 * @author Weiran Liu
 * @date 2024/4/28
 */
public class Zcl23PkeMqRpmtConfig extends AbstractMultiPartyPtoConfig implements MqRpmtConfig {
    /**
     * Zp-DOKVS type
     */
    private final ZpDokvsType zpDokvsType;
    /**
     * ECC-DOKVS type
     */
    private final EccDokvsType eccDokvsType;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * pipeline size
     */
    private final int pipeSize;

    private Zcl23PkeMqRpmtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        eccDokvsType = builder.eccDokvsType;
        zpDokvsType = EccDokvsFactory.getCorrespondingEccDokvsType(eccDokvsType);
        compressEncode = builder.compressEncode;
        pipeSize = builder.pipeSize;
    }

    @Override
    public MqRpmtType getPtoType() {
        return MqRpmtType.ZCL23_PKE;
    }

    @Override
    public int getVectorLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkGreater("server_element_size", serverElementSize, 1);
        MathPreconditions.checkGreater("client_element_size", clientElementSize, 1);
        return serverElementSize;
    }

    public ZpDokvsType getZpDokvsType() {
        return zpDokvsType;
    }

    public EccDokvsType getEccDokvsType() {
        return eccDokvsType;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public int getPipeSize() {
        return pipeSize;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zcl23PkeMqRpmtConfig> {
        /**
         * ECC-DOKVS type
         */
        private EccDokvsType eccDokvsType;
        /**
         * compress encode
         */
        private boolean compressEncode;
        /**
         * pipeline size
         */
        private int pipeSize;

        public Builder() {
            eccDokvsType = EccDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
            compressEncode = true;
            pipeSize = (1 << 8);
        }

        public Builder setEccDokvsType(EccDokvsType eccDokvsType) {
            this.eccDokvsType = eccDokvsType;
            return this;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        public Builder setPipeSize(int pipeSize) {
            MathPreconditions.checkPositive("pipeSize", pipeSize);
            this.pipeSize = pipeSize;
            return this;
        }

        @Override
        public Zcl23PkeMqRpmtConfig build() {
            return new Zcl23PkeMqRpmtConfig(this);
        }
    }
}

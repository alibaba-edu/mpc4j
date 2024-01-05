package edu.alibaba.mpc4j.s2pc.pjc.main.pid;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20ByteEccPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuConfigUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20EccPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21MpPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21SloppyPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;

import java.util.Properties;

/**
 * PID协议配置项工具类。
 *
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class PidConfigUtils {

    private PidConfigUtils() {
        // empty
    }

    /**
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    static PidConfig createConfig(Properties properties) {
        // 读取协议类型
        String pidTypeString = PropertiesUtils.readString(properties, "pid_pto_name");
        PidFactory.PidType pidType = PidFactory.PidType.valueOf(pidTypeString);
        switch (pidType) {
            case BKMS20_BYTE_ECC:
                return createBkms20EccPidConfig();
            case BKMS20_ECC:
                return createBkms20PidConfig(properties);
            case GMR21_MP:
                return createGmr21MpPidConfig(properties);
            case GMR21_SLOPPY:
                return createGmr21SloppyPidConfig(properties);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + PidFactory.PidType.class.getSimpleName() + ":" + pidTypeString
                );
        }
    }

    private static Bkms20ByteEccPidConfig createBkms20EccPidConfig() {
        return new Bkms20ByteEccPidConfig.Builder().build();
    }

    private static Bkms20EccPidConfig createBkms20PidConfig(Properties properties) {
        // 是否使用压缩编码
        boolean compressEncode = PropertiesUtils.readBoolean(properties, "compress_encode", false);

        return new Bkms20EccPidConfig.Builder()
            .setCompressEncode(compressEncode)
            .build();
    }

    private static Gmr21MpPidConfig createGmr21MpPidConfig(Properties properties) {
        // PSU类型
        PsuConfig psuConfig = PsuConfigUtils.createPsuConfig(properties);
        return new Gmr21MpPidConfig.Builder()
            .setMpOprfConfig(OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST))
            .setPsuConfig(psuConfig)
            .build();
    }

    private static Gmr21SloppyPidConfig createGmr21SloppyPidConfig(Properties properties) {
        PsuConfig psuConfig = PsuConfigUtils.createPsuConfig(properties);
        return new Gmr21SloppyPidConfig.Builder()
            .setSloppyOkvsType(Gf2eDokvsType.MEGA_BIN)
            .setPsuConfig(psuConfig)
            .build();
    }
}

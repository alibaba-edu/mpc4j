package edu.alibaba.mpc4j.work.dpsi.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.dpsi.DpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiConfig;

import java.util.Properties;

/**
 * DPSI config utilities.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/10/09
 */
public class DpsiConfigUtils {
    /**
     * private constructor.
     */
    private DpsiConfigUtils() {
        // empty
    }

    public static DpsiConfig createConfig(Properties properties) {
        // read DpPsiMainType
        DpsiMainType dpsiMainType = MainPtoConfigUtils.readEnum(DpsiMainType.class, properties, DpsiMain.PTO_NAME_KEY);
        boolean silentCot = MainPtoConfigUtils.readSilentCot(properties);
        double epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        double psicaEpsilon = PropertiesUtils.readDouble(properties, "psi_ca_epsilon", epsilon / 2);
        double psdcaEpsilon = PropertiesUtils.readDouble(properties, "psd_ca_epsilon", epsilon / 2);
        switch (dpsiMainType) {
            case MQRPMT_CZZ24:
                return new MqRpmtDpsiConfig.Builder(epsilon, psicaEpsilon, psdcaEpsilon)
                    .setMqRpmtConfig(new Czz24CwOprfMqRpmtConfig.Builder().build())
                    .build();
            case MQRPMT_GMR21:
                return new MqRpmtDpsiConfig.Builder(epsilon, psicaEpsilon, psdcaEpsilon)
                    .setMqRpmtConfig(new Gmr21MqRpmtConfig.Builder(silentCot).build())
                    .build();
            case CCPSI_CGS22:
                return new CcpsiDpsiConfig.Builder(epsilon)
                    .setCcpsiConfig(new Cgs22CcpsiConfig.Builder(silentCot).build())
                    .build();
            case CCPSI_RS21:
                return new CcpsiDpsiConfig.Builder(epsilon)
                    .setCcpsiConfig(new Psty19CcpsiConfig.Builder(silentCot).build())
                    .build();
            case CCPSI_PRTY19:
                return new CcpsiDpsiConfig.Builder(epsilon)
                    .setCcpsiConfig(new Rs21CcpsiConfig.Builder(silentCot).build())
                    .build();
            default:
                throw new IllegalArgumentException("Invalid " + DpsiMainType.class.getSimpleName() + ": " + dpsiMainType.name());
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pso.main.psica;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;
import edu.alibaba.mpc4j.s2pc.pso.psica.gmr21.Gmr21PsiCaConfig;

import java.util.Properties;

/**
 * PSICA config utilities.
 *
 * @author Liqiang Peng
 * @date 2023/9/28
 */
public class PsiCaConfigUtils {
    /**
     * private constructor.
     */
    private PsiCaConfigUtils() {
        // empty
    }

    public static PsiCaConfig createPsiCaConfig(Properties properties) {
        // read PSI type
        String psiCaTypeString = PropertiesUtils.readString(properties, "pto_name");
        PsiCaFactory.PsiCaType psiCaType = PsiCaFactory.PsiCaType.valueOf(psiCaTypeString);
        if (psiCaType == PsiCaFactory.PsiCaType.GMR21) {
            return new Gmr21PsiCaConfig.Builder(false).build();
        }
        throw new IllegalArgumentException(
            "Invalid " + PsiCaFactory.PsiCaType.class.getSimpleName() + ": " + psiCaType.name()
        );
    }
}

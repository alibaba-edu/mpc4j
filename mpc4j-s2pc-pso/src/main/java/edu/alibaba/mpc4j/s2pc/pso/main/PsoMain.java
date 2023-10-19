package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.payablepsi.PayablePsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psi.PsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psica.PsiCaMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuBlackIpMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * PSO主函数。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class PsoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);

    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read config
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // 读取协议类型
        String ptoType = PropertiesUtils.readString(properties, "pto_type");
        LOGGER.info("pto_type = " + ptoType);
        switch (ptoType) {
            case PsuBlackIpMain.PTO_TYPE_NAME:
                PsuBlackIpMain psuBlackIpMain = new PsuBlackIpMain(properties);
                psuBlackIpMain.run();
                break;
            case PsuMain.PTO_TYPE_NAME:
                PsuMain psuMain = new PsuMain(properties);
                psuMain.runNetty();
                break;
            case PsiMain.PTO_TYPE_NAME:
                PsiMain psiMain = new PsiMain(properties);
                psiMain.runNetty();
                break;
            case PayablePsiMain.PTO_TYPE_NAME:
                PayablePsiMain payablePsiMain = new PayablePsiMain(properties);
                payablePsiMain.runNetty();
                break;
            case PsiCaMain.PTO_TYPE_NAME:
                PsiCaMain psiCaMain = new PsiCaMain(properties);
                psiCaMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}

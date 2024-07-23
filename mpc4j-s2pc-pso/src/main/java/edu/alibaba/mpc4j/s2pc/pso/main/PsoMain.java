package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.ccpsi.CcpsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psi.PsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuBlackIpMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuMain;
import edu.alibaba.mpc4j.s2pc.pso.main.scpsi.ScpsiMain;

import java.util.Properties;

/**
 * PSO main.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class PsoMain {
    /**
     * main function.
     *
     * @param args two arguments, config and party.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String ptoType = MainPtoConfigUtils.readPtoType(properties);
        switch (ptoType) {
            case PsuBlackIpMain.PTO_TYPE_NAME:
                PsuBlackIpMain psuBlackIpMain = new PsuBlackIpMain(properties, ownName);
                psuBlackIpMain.runNetty();
                break;
            case PsuMain.PTO_TYPE_NAME:
                PsuMain psuMain = new PsuMain(properties, ownName);
                psuMain.runNetty();
                break;
            case PsiMain.PTO_TYPE_NAME:
                PsiMain psiMain = new PsiMain(properties, ownName);
                psiMain.runNetty();
                break;
            case CcpsiMain.PTO_TYPE_NAME:
                CcpsiMain ccpsiMain = new CcpsiMain(properties, ownName);
                ccpsiMain.runNetty();
                break;
            case ScpsiMain.PTO_TYPE_NAME:
                ScpsiMain scpsiMain = new ScpsiMain(properties, ownName);
                scpsiMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid " + MainPtoConfigUtils.PTO_TYPE_KEY + ": " + ptoType);
        }
        System.exit(0);
    }
}

package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.ccpsi.CcpsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psi.PsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.OoPsuMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuBlackIpMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuMain;
import edu.alibaba.mpc4j.s2pc.pso.main.scpsi.ScpsiMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author Feng Han
 * @date 2024/12/10
 */
public class PsoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);

    /**
     * main function.
     *
     * @param args two arguments, config and party.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        File inputFile = new File(args[0]);
        String ownName = args[1];
        List<String> allFiles;
        if (inputFile.isDirectory()) {
            // we support directory containing many config files.
            File[] fs = inputFile.listFiles();
            allFiles = new LinkedList<>();
            assert fs != null;
            for (File f : fs) {
                if ((!f.isDirectory()) && f.getPath().endsWith(".conf")) {
                    allFiles.add(f.getPath());
                }
            }
        } else {
            // single file
            allFiles = List.of(inputFile.getPath());
        }
        String[] names = allFiles.stream().sorted().toArray(String[]::new);
        LOGGER.info(Arrays.toString(names));
        for (String name : names) {
            Properties properties = PropertiesUtils.loadProperties(name);
            String ptoType = MainPtoConfigUtils.readPtoType(properties);
            switch (ptoType) {
                case OoPsuMain.PTO_TYPE_NAME: {
                    OoPsuMain main = new OoPsuMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case PsuMain.PTO_TYPE_NAME: {
                    PsuMain main = new PsuMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case PsuBlackIpMain.PTO_TYPE_NAME: {
                    PsuBlackIpMain psuBlackIpMain = new PsuBlackIpMain(properties, ownName);
                    psuBlackIpMain.runNetty();
                    break;
                }
                case PsiMain.PTO_TYPE_NAME: {
                    PsiMain psiMain = new PsiMain(properties, ownName);
                    psiMain.runNetty();
                    break;
                }
                case CcpsiMain.PTO_TYPE_NAME: {
                    CcpsiMain ccpsiMain = new CcpsiMain(properties, ownName);
                    ccpsiMain.runNetty();
                    break;
                }
                case ScpsiMain.PTO_TYPE_NAME: {
                    ScpsiMain scpsiMain = new ScpsiMain(properties, ownName);
                    scpsiMain.runNetty();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid " + MainPtoConfigUtils.PTO_TYPE_KEY + ": " + ptoType);
            }
        }
        System.exit(0);
    }
}

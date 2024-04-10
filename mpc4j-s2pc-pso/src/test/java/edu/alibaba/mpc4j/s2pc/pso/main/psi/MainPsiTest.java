package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * PSI main tests.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
@RunWith(Parameterized.class)
public class MainPsiTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RR22
        configurations.add(new Object[] {
            PsiType.RR22.name() + "(" + SecurityModel.SEMI_HONEST + ")", "psi/rr22_semi_honest.txt",
        });
        configurations.add(new Object[] {
            PsiType.RR22.name() + "(" + SecurityModel.MALICIOUS + ")", "psi/rr22_malicious.txt",
        });
        configurations.add(new Object[] {
            PsiType.RR22.name() + "(" + Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT + ")", "psi/rr22_h3_binary_blaze_gct.txt",
        });
        // RS21
        configurations.add(new Object[] {
            PsiType.RS21.name() + "(" + SecurityModel.SEMI_HONEST + ")", "psi/rs21_semi_honest.txt",
        });
        configurations.add(new Object[] {
            PsiType.RS21.name() + "(" + SecurityModel.MALICIOUS + ")", "psi/rs21_malicious.txt",
        });
        // RT21
        configurations.add(new Object[] {
            PsiType.RT21.name(), "psi/rt21.txt",
        });
        // OOS17
        configurations.add(new Object[] {
            PsiType.OOS17.name(), "psi/oos17.txt",
        });
        // HFH99_ECC
        configurations.add(new Object[] {
            PsiType.HFH99_ECC.name() + " (uncompress)", "psi/hfh99_ecc_uncompress.txt",
        });
        configurations.add(new Object[] {
            PsiType.HFH99_ECC.name() + " (compress)", "psi/hfh99_ecc_compress.txt",
        });
        // HFH99_BYTE_ECC
        configurations.add(new Object[] {
            PsiType.HFH99_BYTE_ECC.name(), "psi/hfh99_byte_ecc.txt",
        });
        // KKRT16
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (no-stash)", "psi/kkrt16_naive_no_stash.txt",
        });
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (4 hash)", "psi/kkrt16_naive_4_hash.txt",
        });
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name(), "psi/kkrt16_naive_3_hash.txt",
        });
        // DCW13
        configurations.add(new Object[] {
            PsiType.DCW13.name(), "psi/dcw13.txt",
        });
        // PSZ14
        configurations.add(new Object[] {
            PsiType.PSZ14.name(), "psi/psz14.txt",
        });
        // RA17_BYTE_ECC
        configurations.add(new Object[] {
            PsiType.RA17_BYTE_ECC.name(), "psi/ra17_byte_ecc.txt",
        });
        // RA17_ECC
        configurations.add(new Object[] {
            PsiType.RA17_ECC.name(), "psi/ra17_ecc.txt",
        });
        // PRTY19_FAST
        configurations.add(new Object[] {
            PsiType.PRTY19_FAST.name(), "psi/prty19_fast.txt",
        });
        // PRTY20
        configurations.add(new Object[] {
            PsiType.PRTY20.name() + "(" + SecurityModel.SEMI_HONEST + ")", "psi/prty20_semi_honest.txt",
        });
        configurations.add(new Object[] {
            PsiType.PRTY20.name() + "(" + SecurityModel.MALICIOUS + ")", "psi/prty20_malicious.txt",
        });
        configurations.add(new Object[] {
            PsiType.PRTY20.name() + "(" + Gf2kDokvsType.H3_CLUSTER_BINARY_BLAZE_GCT + ")", "psi/prty20_h3_binary_blaze_gct.txt",
        });
        // CM20
        configurations.add(new Object[] {
            PsiType.CM20.name(), "psi/cm20.txt",
        });
        // GMR21
        configurations.add(new Object[] {
            PsiType.GMR21.name() + "(silent)", "psi/gmr21_silent.txt",
        });
        configurations.add(new Object[] {
            PsiType.GMR21.name() + "(no-silent)", "psi/gmr21_no_silent.txt",
        });
        // CZZ22
        configurations.add(new Object[] {
            PsiType.CZZ22.name(), "psi/czz22.txt",
        });
        // RR16
        configurations.add(new Object[] {
            PsiType.RR16.name(), "psi/rr16.txt",
        });
        // RR17_DE
        configurations.add(new Object[] {
            PsiType.RR17_DE.name(), "psi/rr17_de_lan.txt",
        });
        configurations.add(new Object[] {
            PsiType.RR17_DE.name(), "psi/rr17_de_wan.txt",
        });
        // RR17_EC
        configurations.add(new Object[] {
            PsiType.RR17_EC.name(), "psi/rr17_ec_lan.txt",
        });
        configurations.add(new Object[] {
            PsiType.RR17_EC.name(), "psi/rr17_ec_wan.txt",
        });

        return configurations;
    }

    /**
     * file name
     */
    private final String filePath;

    public MainPsiTest(String name, String filePath) {
        super(name);
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
        this.filePath = filePath;
    }

    @Test
    public void testPsi() throws InterruptedException {
        Properties properties = readConfig(filePath);
        runTest(new PsiMain(properties));
    }

    private void runTest(PsiMain psiMain) throws InterruptedException {
        MainPsiServerThread serverThread = new MainPsiServerThread(firstRpc, secondRpc.ownParty(), psiMain);
        MainPsiClientThread clientThread = new MainPsiClientThread(secondRpc, firstRpc.ownParty(), psiMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
        Assert.assertTrue(serverThread.getSuccess());
        Assert.assertTrue(clientThread.getSuccess());
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPsiTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}

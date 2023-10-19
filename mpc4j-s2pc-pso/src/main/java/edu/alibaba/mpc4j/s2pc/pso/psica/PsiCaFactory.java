package edu.alibaba.mpc4j.s2pc.pso.psica;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.psica.cgt12.Cgt12EccPsiCaClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.cgt12.Cgt12EccPsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.cgt12.Cgt12EccPsiCaServer;
import edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi.CcPsiCaClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi.CcPsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi.CcPsiCaServer;
import edu.alibaba.mpc4j.s2pc.pso.psica.gmr21.Gmr21PsiCaClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.gmr21.Gmr21PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.gmr21.Gmr21PsiCaServer;
import edu.alibaba.mpc4j.s2pc.pso.psica.hfh99.Hfh99EccPsiCaClient;
import edu.alibaba.mpc4j.s2pc.pso.psica.hfh99.Hfh99EccPsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.hfh99.Hfh99EccPsiCaServer;

/**
 * PSI Cardinality factory.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class PsiCaFactory implements PtoFactory {
    /**
     * private constructor
     */
    private PsiCaFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum PsiCaType {
        /**
         * HFH99 based on ECC
         */
        HFH99_ECC,
        /**
         * HFH99 based on Byte ECC
         */
        HFH99_BYTE_ECC,
        /**
         * CGT12 based on ECC
         */
        CGT12_ECC,
        /**
         * CGR12 based on Byte ECC
         */
        CGR12_BYTE_ECC,
        /**
         * client-payload circuit PSI
         */
        CCPSI,
        /**
         * GMR21
         */
        GMR21,
    }

    /**
     * Creates a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static <X> PsiCaServer<X> createServer(Rpc serverRpc, Party clientParty, PsiCaConfig config) {
        PsiCaType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiCaServer<>(serverRpc, clientParty, (Hfh99EccPsiCaConfig) config);
            case CGT12_ECC:
                return new Cgt12EccPsiCaServer<>(serverRpc, clientParty, (Cgt12EccPsiCaConfig) config);
            case CCPSI:
                return new CcPsiCaServer<>(serverRpc, clientParty, (CcPsiCaConfig) config);
            case GMR21:
                return new Gmr21PsiCaServer<>(serverRpc, clientParty, (Gmr21PsiCaConfig) config);
            case HFH99_BYTE_ECC:
            case CGR12_BYTE_ECC:
            default:
                throw new IllegalArgumentException("Invalid " + PsiCaType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static <X> PsiCaClient<X> createClient(Rpc clientRpc, Party serverParty, PsiCaConfig config) {
        PsiCaType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiCaClient<>(clientRpc, serverParty, (Hfh99EccPsiCaConfig) config);
            case CGT12_ECC:
                return new Cgt12EccPsiCaClient<>(clientRpc, serverParty, (Cgt12EccPsiCaConfig) config);
            case CCPSI:
                return new CcPsiCaClient<>(clientRpc, serverParty, (CcPsiCaConfig) config);
            case GMR21:
                return new Gmr21PsiCaClient<>(clientRpc, serverParty, (Gmr21PsiCaConfig) config);
            case HFH99_BYTE_ECC:
            case CGR12_BYTE_ECC:
            default:
                throw new IllegalArgumentException("Invalid " + PsiCaType.class.getSimpleName() + ": " + type.name());
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17.Oos17PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17.Oos17PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17.Oos17PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22.Rr22PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22.Rr22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22.Rr22PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21.Rs21PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21.Rs21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21.Rs21PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22.Czz22PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22.Czz22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22.Czz22PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21.Gmr21PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21.Gmr21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21.Gmr21PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20.Cm20PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20.Cm20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20.Cm20PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16.Rr16PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16.Rr16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16.Rr16PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.psz14.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21.Rt21PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21.Rt21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21.Rt21PsiServer;

/**
 * PSI factory.
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PsiFactory() {
        // empty
    }

    /**
     * PSI type
     */
    public enum PsiType {
        /**
         * RR22
         */
        RR22,
        /**
         * RS21
         */
        RS21,
        /**
         * HFH99 (ECC)
         */
        HFH99_ECC,
        /**
         * HFH99 (byte ECC)
         */
        HFH99_BYTE_ECC,
        /**
         * KKRT16
         */
        KKRT16,
        /**
         * CM20
         */
        CM20,
        /**
         * CZZ22
         */
        CZZ22,
        /**
         * GMR21
         */
        GMR21,
        /**
         * PRTY19 (fast computation)
         */
        PRTY19_FAST,
        /**
         * PRTY19 (low communication)
         */
        PRTY19_LOW,
        /**
         * PRTY20 (semi-honest)
         */
        PRTY20,
        /**
         * OOS17
         */
        OOS17,
        /**
         * DCW13
         */
        DCW13,
        /**
         * PSZ14
         */
        PSZ14,
        /**
         * RA17 (ECC)
         */
        RA17_ECC,
        /**
         * RA17 (byte ECC)
         */
        RA17_BYTE_ECC,
        /**
         * RT21
         */
        RT21,
        /**
         * RR17 Dual Execution
         */
        RR17_DE,
        /**
         * RR17 Encode-Commit
         */
        RR17_EC,
        /**
         * RR16
         */
        RR16
    }

    /**
     * Creates a PSI server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a PSI server.
     */
    public static <X> PsiServer<X> createServer(Rpc serverRpc, Party clientParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiServer<>(serverRpc, clientParty, (Hfh99EccPsiConfig) config);
            case HFH99_BYTE_ECC:
                return new Hfh99ByteEccPsiServer<>(serverRpc, clientParty, (Hfh99ByteEccPsiConfig) config);
            case KKRT16:
                return new Kkrt16PsiServer<>(serverRpc, clientParty, (Kkrt16PsiConfig) config);
            case CM20:
                return new Cm20PsiServer<>(serverRpc, clientParty, (Cm20PsiConfig) config);
            case CZZ22:
                return new Czz22PsiServer<>(serverRpc, clientParty, (Czz22PsiConfig) config);
            case GMR21:
                return new Gmr21PsiServer<>(serverRpc, clientParty, (Gmr21PsiConfig) config);
            case PRTY20:
                return new Prty20PsiServer<>(serverRpc, clientParty, (Prty20PsiConfig) config);
            case RA17_ECC:
                return new Ra17EccPsiServer<>(serverRpc, clientParty, (Ra17EccPsiConfig) config);
            case RA17_BYTE_ECC:
                return new Ra17ByteEccPsiServer<>(serverRpc, clientParty, (Ra17ByteEccPsiConfig) config);
            case OOS17:
                return new Oos17PsiServer<>(serverRpc, clientParty, (Oos17PsiConfig) config);
            case DCW13:
                return new Dcw13PsiServer<>(serverRpc, clientParty, (Dcw13PsiConfig) config);
            case PSZ14:
                return new Psz14PsiServer<>(serverRpc, clientParty, (Psz14PsiConfig) config);
            case PRTY19_FAST:
                return new Prty19FastPsiServer<>(serverRpc, clientParty, (Prty19FastPsiConfig) config);
            case PRTY19_LOW:
                return new Prty19LowPsiServer<>(serverRpc, clientParty, (Prty19LowPsiConfig) config);
            case RT21:
                return new Rt21PsiServer<>(serverRpc, clientParty, (Rt21PsiConfig) config);
            case RS21:
                return new Rs21PsiServer<>(serverRpc, clientParty, (Rs21PsiConfig) config);
            case RR22:
                return new Rr22PsiServer<>(serverRpc, clientParty, (Rr22PsiConfig) config);
            case RR17_DE:
                return new Rr17DePsiServer<>(serverRpc, clientParty, (Rr17DePsiConfig) config);
            case RR17_EC:
                return new Rr17EcPsiServer<>(serverRpc, clientParty, (Rr17EcPsiConfig) config);
            case RR16:
                return new Rr16PsiServer<>(serverRpc, clientParty, (Rr16PsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + type.name());
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
    public static <X> PsiClient<X> createClient(Rpc clientRpc, Party serverParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiClient<>(clientRpc, serverParty, (Hfh99EccPsiConfig) config);
            case HFH99_BYTE_ECC:
                return new Hfh99ByteEccPsiClient<>(clientRpc, serverParty, (Hfh99ByteEccPsiConfig) config);
            case KKRT16:
                return new Kkrt16PsiClient<>(clientRpc, serverParty, (Kkrt16PsiConfig) config);
            case CM20:
                return new Cm20PsiClient<>(clientRpc, serverParty, (Cm20PsiConfig) config);
            case CZZ22:
                return new Czz22PsiClient<>(clientRpc, serverParty, (Czz22PsiConfig) config);
            case GMR21:
                return new Gmr21PsiClient<>(clientRpc, serverParty, (Gmr21PsiConfig) config);
            case PRTY20:
                return new Prty20PsiClient<>(clientRpc, serverParty, (Prty20PsiConfig) config);
            case RA17_ECC:
                return new Ra17EccPsiClient<>(clientRpc, serverParty, (Ra17EccPsiConfig) config);
            case RA17_BYTE_ECC:
                return new Ra17ByteEccPsiClient<>(clientRpc, serverParty, (Ra17ByteEccPsiConfig) config);
            case OOS17:
                return new Oos17PsiClient<>(clientRpc, serverParty, (Oos17PsiConfig) config);
            case DCW13:
                return new Dcw13PsiClient<>(clientRpc, serverParty, (Dcw13PsiConfig) config);
            case PSZ14:
                return new Psz14PsiClient<>(clientRpc, serverParty, (Psz14PsiConfig) config);
            case PRTY19_FAST:
                return new Prty19FastPsiClient<>(clientRpc, serverParty, (Prty19FastPsiConfig) config);
            case PRTY19_LOW:
                return new Prty19LowPsiClient<>(clientRpc, serverParty, (Prty19LowPsiConfig) config);
            case RT21:
                return new Rt21PsiClient<>(clientRpc, serverParty, (Rt21PsiConfig) config);
            case RS21:
                return new Rs21PsiClient<>(clientRpc, serverParty, (Rs21PsiConfig) config);
            case RR22:
                return new Rr22PsiClient<>(clientRpc, serverParty, (Rr22PsiConfig) config);
            case RR17_DE:
                return new Rr17DePsiClient<>(clientRpc, serverParty, (Rr17DePsiConfig) config);
            case RR17_EC:
                return new Rr17EcPsiClient<>(clientRpc, serverParty, (Rr17EcPsiConfig) config);
            case RR16:
                return new Rr16PsiClient<>(clientRpc, serverParty, (Rr16PsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pso.pid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.pid.bkms20.Bkms20PidClient;
import edu.alibaba.mpc4j.s2pc.pso.pid.bkms20.Bkms20PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.bkms20.Bkms20PidServer;
import edu.alibaba.mpc4j.s2pc.pso.pid.gmr21.*;

/**
 * PID协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public class PidFactory {
    /**
     * 私有构造函数
     */
    private PidFactory() {
        // empty
    }

    /**
     * PID协议类型。
     */
    public enum PidType {
        /**
         * Facebook的PID方案
         */
        BKMS20,
        /**
         * GMR21的多点OPRF方案
         */
        GMR21_MP,
        /**
         * GMR21的Sloppy方案
         */
        GMR21_SLOPPY,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @param <X>         集合类型。
     * @return 服务端。
     */
    public static <X> PidParty<X> createServer(Rpc serverRpc, Party clientParty, PidConfig config) {
        PidType type = config.getPtoType();
        switch (type) {
            case BKMS20:
                return new Bkms20PidServer<>(serverRpc, clientParty, (Bkms20PidConfig) config);
            case GMR21_SLOPPY:
                return new Gmr21SloppyPidServer<>(serverRpc, clientParty, (Gmr21SloppyPidConfig) config);
            case GMR21_MP:
                return new Gmr21MpPidServer<>(serverRpc, clientParty, (Gmr21MpPidConfig) config);
            default:
                throw new IllegalArgumentException("Invalid PidType: " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc   客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @param <X>         集合类型。
     * @return 客户端。
     */
    public static <X> PidParty<X> createClient(Rpc clientRpc, Party serverParty, PidConfig config) {
        PidType type = config.getPtoType();
        switch (type) {
            case BKMS20:
                return new Bkms20PidClient<>(clientRpc, serverParty, (Bkms20PidConfig) config);
            case GMR21_SLOPPY:
                return new Gmr21SloppyPidClient<>(clientRpc, serverParty, (Gmr21SloppyPidConfig) config);
            case GMR21_MP:
                return new Gmr21MpPidClient<>(clientRpc, serverParty, (Gmr21MpPidConfig) config);
            default:
                throw new IllegalArgumentException("Invalid PidType: " + type.name());
        }
    }
}

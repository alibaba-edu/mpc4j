package edu.alibaba.mpc4j.work.db.dynamic;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;

import java.util.Arrays;

/**
 * 2pc z2 party with default config
 *
 * @author Feng Han
 * @date 2025/3/10
 */
public class DynamicDb2pcZ2Party extends AbstractTwoPartyMemoryRpcPto {

    public DynamicDb2pcZ2Party(String name) {
        super(name);
    }

    public MpcZ2cParty[] genParties(boolean parallel){
        Z2cConfig z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
        Z2cParty[] parties = new Z2cParty[]{
            Z2cFactory.createSender(firstRpc, secondRpc.ownParty(), z2cConfig),
            Z2cFactory.createReceiver(secondRpc, firstRpc.ownParty(), z2cConfig)
        };
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }
}

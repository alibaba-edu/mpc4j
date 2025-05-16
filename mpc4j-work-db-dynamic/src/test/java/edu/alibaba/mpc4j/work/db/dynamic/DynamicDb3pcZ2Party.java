package edu.alibaba.mpc4j.work.db.dynamic;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 3pc z2 party with default config
 *
 * @author Feng Han
 * @date 2025/3/12
 */
public class DynamicDb3pcZ2Party extends AbstractThreePartyMemoryRpcPto {
    public DynamicDb3pcZ2Party(String name) {
        super(name);
    }

    public MpcZ2cParty[] genParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        Aby3Z2cConfig config = new Aby3Z2cConfig.Builder(false).build();
        TripletProvider[] tripletProviders = IntStream.range(0, 3)
            .mapToObj(i ->
                new TripletProvider(rpcAll[i], new TripletProviderConfig.Builder(false).build()))
            .toArray(TripletProvider[]::new);
        TripletZ2cParty[] parties = IntStream.range(0, 3).mapToObj(i ->
            Aby3Z2cFactory.createParty(rpcAll[i], config, tripletProviders[i])).toArray(TripletZ2cParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }
}

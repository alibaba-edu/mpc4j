package edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.AbstractAidPsiParty;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive.Kmrs14ShAidPsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * KMRS14 semi-honest aid PSI client.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class Kmrs14ShAidPsiClient<T> extends AbstractAidPsiParty<T> {
    /**
     * coin-tossing receiver
     */
    private final CoinTossParty coinTossReceiver;
    /**
     * hash
     */
    private final Hash hash;
    /**
     * PRP
     */
    private final Prp prp;

    public Kmrs14ShAidPsiClient(Rpc clientRpc, Party serverParty, Party aiderParty, Kmrs14ShAidPsiConfig config) {
        super(Kmrs14ShAidPsiPtoDesc.getInstance(), clientRpc, serverParty, aiderParty, config);
        coinTossReceiver = CoinTossFactory.createReceiver(clientRpc, serverParty, config.getCoinTossConfig());
        addSubPto(coinTossReceiver);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        prp = PrpFactory.createInstance(envType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // P1 samples a random k-bit key K and sends it to P2. Here we use coin-tossing protocol
        coinTossReceiver.init();
        byte[] key = coinTossReceiver.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH)[0];
        prp.setKey(key);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P2 sends T2 = π_2(F_K(S_2)) to the aider
        Stream<T> clientElementStream = clientElementSet.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        Map<ByteBuffer, T> clientPrpElementMap = clientElementStream
            .collect(Collectors.toMap(
                    element -> {
                        byte[] hashElement = hash.digestToBytes(ObjectUtils.objectToByteArray(element));
                        return ByteBuffer.wrap(prp.prp(hashElement));
                    },
                    element -> element
                )
            );
        List<byte[]> clientPrpElementPayload = clientPrpElementMap.keySet().stream()
            .map(ByteBuffer::array)
            .collect(Collectors.toList());
        Collections.shuffle(clientPrpElementPayload, secureRandom);
        DataPacketHeader clientPrpElementHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_TO_AIDER_TC.ordinal(), extraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPrpElementHeader, clientPrpElementPayload));
        stopWatch.stop();
        long prpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, prpTime);

        // aider computes the intersection and returns it to all the parties
        DataPacketHeader clientIntersectionPrpElementHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.AIDER_TO_CLIENT_T_I.ordinal(), extraInfo,
            rightParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientIntersectionPrpElementPayload = rpc.receive(clientIntersectionPrpElementHeader).getPayload();

        stopWatch.start();
        // P2 outputs the intersection
        Set<T> intersection = clientIntersectionPrpElementPayload.stream()
            .map(ByteBuffer::wrap)
            .map(clientPrpElementMap::get)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, intersectionTime);

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psi.aid.kmrs14;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pso.psi.aid.AbstractAidPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.aid.kmrs14.Kmrs14AidPsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * KMRS14 semi-honest aid PSI server.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class Kmrs14AidPsiServer<T> extends AbstractAidPsiServer<T> {
    /**
     * coin-tossing sender
     */
    private final CoinTossParty coinTossSender;
    /**
     * hash
     */
    private final Hash hash;
    /**
     * PRP
     */
    private final Prp prp;

    public Kmrs14AidPsiServer(Rpc serverRpc, Party clientParty, Party aiderParty, Kmrs14AidPsiConfig config) {
        super(Kmrs14AidPsiPtoDesc.getInstance(), serverRpc, clientParty, aiderParty, config);
        coinTossSender = CoinTossFactory.createSender(serverRpc, clientParty, config.getCoinTossConfig());
        addSubPto(coinTossSender);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        prp = PrpFactory.createInstance(envType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // P1 samples a random k-bit key K and sends it to P2. Here we use coin-tossing protocol
        coinTossSender.init();
        byte[] key = coinTossSender.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH)[0];
        prp.setKey(key);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P1 sends T1 = π_1(F_K(S_1)) to the aider
        Stream<T> serverElementStream = serverElementSet.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        Map<ByteBuffer, T> serverPrpElementMap = serverElementStream
            .collect(Collectors.toMap(
                    element -> {
                        byte[] hashElement = hash.digestToBytes(ObjectUtils.objectToByteArray(element));
                        return ByteBuffer.wrap(prp.prp(hashElement));
                    },
                    element -> element
                )
            );
        List<byte[]> serverPrpElementPayload = serverPrpElementMap.keySet().stream()
            .map(ByteBuffer::array)
            .collect(Collectors.toList());
        Collections.shuffle(serverPrpElementPayload, secureRandom);
        sendAidPartyPayload(PtoStep.SERVER_TO_AIDER_TS.ordinal(), serverPrpElementPayload);
        stopWatch.stop();
        long prpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, prpTime);

        logPhaseInfo(PtoState.PTO_END);
    }
}

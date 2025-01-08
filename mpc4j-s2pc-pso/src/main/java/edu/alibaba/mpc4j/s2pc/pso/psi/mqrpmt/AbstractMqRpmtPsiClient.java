package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.MqRpmtPsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * abstract mq-RPMT PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/9
 */
public abstract class AbstractMqRpmtPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * mq-RPMT server
     */
    private final MqRpmtServer mqRpmtServer;
    /**
     * hash
     */
    private final Hash hash;

    public AbstractMqRpmtPsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, MqRpmtPsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        mqRpmtServer = MqRpmtFactory.createServer(clientRpc, serverParty, config.getMqRpmtConfig());
        addSubPto(mqRpmtServer);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int refineMaxClientElementSize = Math.max(maxClientElementSize, 2);
        int refineMaxServerElementSize = Math.max(maxServerElementSize, 2);
        mqRpmtServer.init(refineMaxClientElementSize, refineMaxServerElementSize);
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
        int refineServerElementSize = Math.max(serverElementSize, 2);
        // client hashes all elements to 128 bits
        Stream<T> clientElementStream = clientElementSet.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        Map<ByteBuffer, T> clientHashElementMap = clientElementStream
            .collect(Collectors.toMap(
                element -> ByteBuffer.wrap(hash.digestToBytes(ObjectUtils.objectToByteArray(element))),
                element -> element
            ));
        if (clientElementSize == 1) {
            byte[] paddingHashElement = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(paddingHashElement);
            clientHashElementMap.put(ByteBuffer.wrap(paddingHashElement), null);
        }
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, setupTime, "Client init elements");

        stopWatch.start();
        ByteBuffer[] clientVector = mqRpmtServer.mqRpmt(clientHashElementMap.keySet(), refineServerElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, mqRpmtTime, "Client runs mq-RPMT");

        List<byte[]> serverBitVectorPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_BIT_VECTOR.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(serverBitVectorPayload.size() == 1);
        BitVector serverBitVector = BitVectorFactory.create(clientVector.length, serverBitVectorPayload.get(0));
        Set<T> intersection = IntStream.range(0, clientVector.length)
            .mapToObj(index -> serverBitVector.get(index) ? clientVector[index] : null)
            .filter(Objects::nonNull)
            .map(clientHashElementMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long serverCipherTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverCipherTime, "Client obtains intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.MqRpmtPsiPtoDesc.PtoStep;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * abstract mq-RPMT PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/9
 */
public abstract class AbstractMqRpmtPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * mq-RPMT client
     */
    private final MqRpmtClient mqRpmtClient;
    /**
     * hash
     */
    private final Hash hash;

    public AbstractMqRpmtPsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, MqRpmtPsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        mqRpmtClient = MqRpmtFactory.createClient(serverRpc, clientParty, config.getMqRpmtConfig());
        addSubPto(mqRpmtClient);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int refineMaxServerElementSize = Math.max(maxServerElementSize, 2);
        int refineMaxClientElementSize = Math.max(maxClientElementSize, 2);
        mqRpmtClient.init(refineMaxServerElementSize, refineMaxClientElementSize);
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
        int refineClientElementSize = Math.max(clientElementSize, 2);
        // server hashes all elements to 128 bits
        Stream<T> serverElementStream = serverElementSet.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        Set<ByteBuffer> serverHashElementSet = serverElementStream
            .map(element -> hash.digestToBytes(ObjectUtils.objectToByteArray(element)))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        // pad a random element when server_size = 1, this is because mq-RPMT cannot support n = 1.
        if (serverElementSize == 1) {
            byte[] paddingHashElement = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            secureRandom.nextBytes(paddingHashElement);
            serverHashElementSet.add(ByteBuffer.wrap(paddingHashElement));
        }
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, setupTime, "Server init elements");

        stopWatch.start();
        boolean[] serverVector = mqRpmtClient.mqRpmt(serverHashElementSet, refineClientElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, mqRpmtTime, "Server runs mq-RPMT");

        stopWatch.start();
        BitVector serverBitVector = BitVectorFactory.createZeros(serverVector.length);
        for (int i = 0; i < serverVector.length; i++) {
            serverBitVector.set(i, serverVector[i]);
        }
        List<byte[]> serverBitVectorPayload = Collections.singletonList(serverBitVector.getBytes());
        sendOtherPartyPayload(PtoStep.SERVER_SEND_BIT_VECTOR.ordinal(), serverBitVectorPayload);
        stopWatch.stop();
        long serverCipherTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverCipherTime, "Server sends bit vector");

        logPhaseInfo(PtoState.PTO_END);
    }
}

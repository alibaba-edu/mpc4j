package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
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
     * mq-RPMT client
     */
    private final MqRpmtClient mqRpmtClient;
    /**
     * mq-RPMT config
     */
    private final MqRpmtConfig mqRpmtConfig;
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * hash
     */
    private final Hash hash;

    public AbstractMqRpmtPsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, MqRpmtPsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
        mqRpmtClient = MqRpmtFactory.createClient(clientRpc, serverParty, config.getMqRpmtConfig());
        mqRpmtConfig = config.getMqRpmtConfig();
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(mqRpmtClient);
        addSubPto(coreCotReceiver);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int refineMaxClientElementSize = Math.max(maxClientElementSize, 2);
        int refineMaxServerElementSize = Math.max(maxServerElementSize, 2);
        mqRpmtClient.init(refineMaxClientElementSize, refineMaxServerElementSize);
        coreCotReceiver.init(mqRpmtConfig.getVectorLength(refineMaxServerElementSize, refineMaxClientElementSize));
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
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Client init elements");

        stopWatch.start();
        boolean[] clientVector = mqRpmtClient.mqRpmt(clientHashElementMap.keySet(), refineServerElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, mqRpmtTime, "Client runs mq-RPMT");

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(clientVector);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, cotTime, "Client runs ROT");

        DataPacketHeader serverCipherHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CIPHER.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverCipherPayload = rpc.receive(serverCipherHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(serverCipherPayload.size() == clientVector.length);
        byte[][] serverCiphers = serverCipherPayload.toArray(new byte[0][]);
        IntStream clientVectorStream = IntStream.range(0, clientVector.length);
        clientVectorStream = parallel ? clientVectorStream.parallel() : clientVectorStream;
        Set<T> intersection = clientVectorStream
            .mapToObj(index -> {
                if (clientVector[index]) {
                    byte[] r1 = rotReceiverOutput.getRb(index);
                    return ByteBuffer.wrap(BytesUtils.xor(r1, serverCiphers[index]));
                } else {
                    return null;
                }
            })
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

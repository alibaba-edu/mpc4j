package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtServer;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.MqRpmtPsiPtoDesc.PtoStep;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * abstract mq-RPMT PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/9
 */
public abstract class AbstractMqRpmtPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * mq-RPMT server
     */
    private final MqRpmtServer mqRpmtServer;
    /**
     * mq-RPMT config
     */
    private final MqRpmtConfig mqRpmtConfig;
    /**
     * cort COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * hash
     */
    private final Hash hash;

    public AbstractMqRpmtPsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, MqRpmtPsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        mqRpmtServer = MqRpmtFactory.createServer(serverRpc, clientParty, config.getMqRpmtConfig());
        mqRpmtConfig = config.getMqRpmtConfig();
        coreCotSender = CoreCotFactory.createSender(serverRpc,clientParty,config.getCoreCotConfig());
        addSubPto(mqRpmtServer);
        addSubPto(coreCotSender);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int refineMaxServerElementSize = Math.max(maxServerElementSize, 2);
        int refineMaxClientElementSize = Math.max(maxClientElementSize, 2);
        mqRpmtServer.init(refineMaxServerElementSize, refineMaxClientElementSize);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, mqRpmtConfig.getVectorLength(refineMaxServerElementSize, refineMaxClientElementSize));
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
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Server init elements");

        stopWatch.start();
        ByteBuffer[] serverVector = mqRpmtServer.mqRpmt(serverHashElementSet, refineClientElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, mqRpmtTime, "Server runs mq-RPMT");

        stopWatch.start();
        CotSenderOutput cotSenderOutput =  coreCotSender.send(serverVector.length);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, rotTime, "Server runs ROT");

        stopWatch.start();
        IntStream serverVectorIntStream = IntStream.range(0, serverVector.length);
        serverVectorIntStream = parallel ? serverVectorIntStream.parallel() : serverVectorIntStream;
        List<byte[]> serverCipherPayload = serverVectorIntStream
            .mapToObj(index -> BytesUtils.xor(rotSenderOutput.getR1(index), serverVector[index].array()))
            .collect(Collectors.toList());
        DataPacketHeader serverCipherHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CIPHER.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverCipherHeader, serverCipherPayload));
        stopWatch.stop();
        long serverCipherTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverCipherTime, "Server sends encrypted elements");

        logPhaseInfo(PtoState.PTO_END);
    }
}

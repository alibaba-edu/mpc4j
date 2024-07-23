package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtServer;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23PkePsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ZCL23-PKE-PSU server.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl23PkePsuServer extends AbstractPsuServer {
    /**
     * ZCL23-PKE-mqRPMT
     */
    private final Zcl23PkeMqRpmtServer zcl23PkeMqRpmtServer;
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;

    public Zcl23PkePsuServer(Rpc serverRpc, Party clientParty, Zcl23PkePsuConfig config) {
        super(Zcl23PkePsuPtoDesc.getInstance(), serverRpc, clientParty, config);
        zcl23PkeMqRpmtServer = new Zcl23PkeMqRpmtServer(serverRpc, clientParty, config.getZcl23PkeMqRpmtConfig());
        addSubPto(zcl23PkeMqRpmtServer);
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zcl23PkeMqRpmtServer.init(maxServerElementSize, maxClientElementSize);
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        ByteBuffer[] serverVector = zcl23PkeMqRpmtServer.mqRpmt(serverElementSet, clientElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, mqRpmtTime, "Server runs mqRPMT");

        stopWatch.start();
        int coreCotNum = serverVector.length;
        CotSenderOutput cotSenderOutput = coreCotSender.send(coreCotNum);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream encIntStream = IntStream.range(0, coreCotNum);
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        List<byte[]> encPayload = encIntStream
            .mapToObj(index -> {
                // do not need CRHF since we call prg
                byte[] ciphertext = encPrg.extendToBytes(cotSenderOutput.getR0(index));
                BytesUtils.xori(ciphertext, serverVector[index].array());
                return ciphertext;
            })
            .collect(Collectors.toList());
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, encTime, "Server handles union");

        logPhaseInfo(PtoState.PTO_END);
    }
}

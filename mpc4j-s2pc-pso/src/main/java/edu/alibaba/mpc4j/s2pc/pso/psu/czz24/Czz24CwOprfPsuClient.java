package edu.alibaba.mpc4j.s2pc.pso.psu.czz24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.czz24.Czz24CwOprfPsuPtoDesc.PtoStep;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CZZ24-cwOPRF-PSU client.
 *
 * @author Yufei Wang
 * @date 2022/8/1
 */
public class Czz24CwOprfPsuClient extends AbstractPsuClient {
    /**
     * CZZ24-cwOPRF-mqRPMT
     */
    private final Czz24CwOprfMqRpmtClient czz24CwOprfMqRpmtClient;
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;

    public Czz24CwOprfPsuClient(Rpc clientRpc, Party serverParty, Czz24CwOprfPsuConfig config) {
        super(Czz24CwOprfPsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        czz24CwOprfMqRpmtClient = new Czz24CwOprfMqRpmtClient(clientRpc, serverParty, config.getCzz24CwOprfPsuConfig());
        addSubPto(czz24CwOprfMqRpmtClient);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        czz24CwOprfMqRpmtClient.init(maxClientElementSize, maxServerElementSize);
        coreCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PsuClientOutput psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        boolean[] choices = czz24CwOprfMqRpmtClient.mqRpmt(clientElementSet, serverElementSize);
        int psica = (int) IntStream.range(0, choices.length).filter(i -> choices[i]).count();
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, mqRpmtTime, "Server runs mqRPMT");

        stopWatch.start();
        int coreCotNum = choices.length;
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choices);
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == coreCotNum);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        // Y \cup Z
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = IntStream.range(0, serverElementSize);
        decIntStream = parallel ? decIntStream.parallel() : decIntStream;
        Set<ByteBuffer> union = decIntStream
            .mapToObj(index -> {
                if (choices[index]) {
                    return botElementByteBuffer;
                } else {
                    // do not need CRHF since we call prg
                    byte[] message = encPrg.extendToBytes(cotReceiverOutput.getRb(index));
                    BytesUtils.xori(message, encArrayList.get(index));
                    return ByteBuffer.wrap(message);
                }
            })
            .collect(Collectors.toSet());
        union.addAll(clientElementSet);
        union.remove(botElementByteBuffer);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, unionTime, "Client handles union");

        logPhaseInfo(PtoState.PTO_END);
        return new PsuClientOutput(union, psica);
    }
}

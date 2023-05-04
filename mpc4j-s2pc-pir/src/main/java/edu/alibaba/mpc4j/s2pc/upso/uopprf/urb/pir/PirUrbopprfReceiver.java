package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.AbstractUrbopprfReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir.PirUrbopprfPtoDesc.*;

/**
 * sparse unbalanced related-batch OPPRF receiver.
 *
 * @author Liqiang Peng
 * @date 2023/4/21
 */
public class PirUrbopprfReceiver extends AbstractUrbopprfReceiver {
    /**
     * single-query OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * d
     */
    private final int d;
    /**
     * bin num
     */
    private int binNum;
    /**
     * h_1, ... h_d
     */
    private Prf[] binHashes;
    /**
     * garbled table
     */
    private byte[][] garbledTable;
    /**
     * batch index pir
     */
    private final BatchIndexPirClient batchIndexPirClient;

    public PirUrbopprfReceiver(Rpc receiverRpc, Party senderParty, PirUrbopprfConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPtos(sqOprfReceiver);
        batchIndexPirClient = BatchIndexPirFactory.createClient(receiverRpc, senderParty, config.getBatchIndexPirConfig());
        addSubPtos(batchIndexPirClient);
        cuckooHashBinType = config.getCuckooHashBinType();
        d = config.getD();
    }

    @Override
    public void init(int l, int batchSize, int pointNum) throws MpcAbortException {
        setInitInput(l, batchSize, pointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init oprf
        sqOprfReceiver.init(batchSize);
        // init batch PIR
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, pointNum);
        batchIndexPirClient.init(binNum, l, batchSize * d);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][][] opprf(byte[][] inputArray) throws MpcAbortException {
        setPtoInput(inputArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive garbled hash table keys
        DataPacketHeader garbledTableKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> garbledTableKeysPayload = rpc.receive(garbledTableKeysHeader).getPayload();

        stopWatch.start();
        // parse garbled table keys
        MpcAbortPreconditions.checkArgument(garbledTableKeysPayload.size() == d);
        byte[][] garbledTableKeys = garbledTableKeysPayload.toArray(new byte[0][]);
        binHashes = Arrays.stream(garbledTableKeys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        List<Integer> retrievalIndexList = generateRetrievalIndexList();
        Map<Integer, byte[]> garbledTablePayload = batchIndexPirClient.pir(retrievalIndexList);
        stopWatch.stop();
        long batchPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, batchPirTime, "Receiver runs batch PIR");

        stopWatch.start();
        // recover garbled table
        generateGarbledTable(retrievalIndexList, garbledTablePayload);
        stopWatch.stop();
        long garbledTableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 0, 2, garbledTableTime, "Receiver handles GT");

        stopWatch.start();
        // OPRF
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(inputArray);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Receiver runs OPRF");

        stopWatch.start();
        byte[][][] outputArray = handleOprfOutput(sqOprfReceiverOutput);
        stopWatch.stop();

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    private byte[][][] handleOprfOutput(SqOprfReceiverOutput sqOprfReceiverOutput) {
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL * d);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(j -> {
                byte[] input = inputArray[j];
                byte[][] prfs = new byte[d][];
                // Compute f_1 || f_2 || f_3 ← F(k, x), where f_b ∈ {0,1}^l for all b ∈ [3].
                byte[] inputPrf = sqOprfReceiverOutput.getPrf(j);
                inputPrf = prf.getBytes(inputPrf);
                for (int b = 0; b < d; b++) {
                    // Compute pos_b ← h_b(x) for all b ∈ [d].
                    int posb = binHashes[b].getInteger(input, binNum);
                    // Return list W = [f_b ⊕ GT[pos_b]]_{b ∈ [d]}
                    prfs[b] = new byte[byteL];
                    System.arraycopy(inputPrf, byteL * b, prfs[b], 0, byteL);
                    BytesUtils.reduceByteArray(prfs[b], l);
                    BytesUtils.xori(prfs[b], garbledTable[posb]);
                }
                return prfs;
            })
            .toArray(byte[][][]::new);
    }

    private List<Integer> generateRetrievalIndexList() {
        List<Integer> retrievalIndexList = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < d; j++) {
                retrievalIndexList.add(binHashes[j].getInteger(inputArray[i], binNum));
            }
        }
        return retrievalIndexList.stream()
            .distinct()
            .collect(Collectors.toList());
    }

    private void generateGarbledTable(List<Integer> retrievalIndexList, Map<Integer, byte[]> garbledTablePayload)
        throws MpcAbortException {
        garbledTable = new byte[binNum][];
        MpcAbortPreconditions.checkArgument(retrievalIndexList.size() == garbledTablePayload.size());
        for (Integer integer : retrievalIndexList) {
            garbledTable[integer] = garbledTablePayload.get(integer);
        }
    }
}

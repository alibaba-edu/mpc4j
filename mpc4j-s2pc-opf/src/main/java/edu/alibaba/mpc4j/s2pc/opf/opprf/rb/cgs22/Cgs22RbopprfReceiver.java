package edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.AbstractRbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22.Cgs22RbopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGS22 Related-Batch OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Cgs22RbopprfReceiver extends AbstractRbopprfReceiver {
    /**
     * the OPRF receiver
     */
    private final OprfReceiver oprfReceiver;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * d
     */
    private final int d;
    /**
     * h_1, ... h_d
     */
    private Prf[] binHashes;

    public Cgs22RbopprfReceiver(Rpc receiverRpc, Party senderParty, Cgs22RbopprfConfig config) {
        super(Cgs22RbopprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(receiverRpc, senderParty, config.getOprfConfig());
        addSubPto(oprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        d = config.getD();
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfReceiver.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][][] opprf(int l, byte[][] inputArray, int pointNum) throws MpcAbortException {
        setPtoInput(l, inputArray, pointNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(inputArray);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, oprfTime, "Receiver runs OPRF");

        // receive garbled hash table keys
        DataPacketHeader garbledTableKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> garbledTableKeysPayload = rpc.receive(garbledTableKeysHeader).getPayload();

        stopWatch.start();
        handleGarbledTableKeys(garbledTableKeysPayload);
        stopWatch.stop();
        long garbledTableKeysTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, garbledTableKeysTime, "Receiver handles GT keys");

        // receive Garbled Table
        DataPacketHeader garbledTableHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> garbledTablePayload = rpc.receive(garbledTableHeader).getPayload();

        stopWatch.start();
        byte[][][] outputArray = handleGarbledTablePayload(oprfReceiverOutput, garbledTablePayload);
        stopWatch.stop();
        long garbledTableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, garbledTableTime, "Receiver handles GT");

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    private void handleGarbledTableKeys(List<byte[]> garbledTableKeysPayload) throws MpcAbortException {
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
    }

    private byte[][][] handleGarbledTablePayload(OprfReceiverOutput oprfReceiverOutput, List<byte[]> garbledTablePayload)
        throws MpcAbortException {
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL * d);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        int binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, pointNum);
        // Interpret hint as a garbled hash table GT.
        MpcAbortPreconditions.checkArgument(garbledTablePayload.size() == binNum);
        byte[][] garbledTable = garbledTablePayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(j -> {
                byte[] input = inputArray[j];
                byte[][] prfs = new byte[d][];
                // Compute f_1 || f_2 || f_3 ← F(k, x), where f_b ∈ {0,1}^l for all b ∈ [3].
                byte[] inputPrf = oprfReceiverOutput.getPrf(j);
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
}

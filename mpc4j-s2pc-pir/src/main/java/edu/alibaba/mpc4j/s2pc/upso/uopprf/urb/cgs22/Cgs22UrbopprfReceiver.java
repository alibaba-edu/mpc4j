package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22;

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
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.AbstractUrbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGS22 unbalanced related-batch OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public class Cgs22UrbopprfReceiver extends AbstractUrbopprfReceiver {
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
     * sent OKVS
     */
    private boolean sent;
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

    public Cgs22UrbopprfReceiver(Rpc receiverRpc, Party senderParty, Cgs22UrbopprfConfig config) {
        super(Cgs22UrbopprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPtos(sqOprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        d = config.getD();
        sent = false;
    }

    @Override
    public void init(int l, int batchSize, int pointNum) throws MpcAbortException {
        setInitInput(l, batchSize, pointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sent = false;
        // init oprf
        sqOprfReceiver.init(batchSize);
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

        if (!sent) {
            // receive garbled hash table keys
            DataPacketHeader garbledTableKeysHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE_KEYS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> garbledTableKeysPayload = rpc.receive(garbledTableKeysHeader).getPayload();
            // receive Garbled Table
            DataPacketHeader garbledTableHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> garbledTablePayload = rpc.receive(garbledTableHeader).getPayload();

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
            // Interpret hint as a garbled hash table GT.
            binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, this.pointNum);
            MpcAbortPreconditions.checkArgument(garbledTablePayload.size() == binNum);
            garbledTable = garbledTablePayload.toArray(new byte[0][]);
            sent = true;
            stopWatch.stop();
            long garbledTableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 0, 2, garbledTableTime, "Receiver handles GT");
        }

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
}

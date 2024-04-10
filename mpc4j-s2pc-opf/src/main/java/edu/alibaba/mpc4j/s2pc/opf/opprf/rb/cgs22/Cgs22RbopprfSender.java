package edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.AbstractRbopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22.Cgs22RbopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CGS22 Related-Batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Cgs22RbopprfSender extends AbstractRbopprfSender {
    /**
     * the OPRF sender
     */
    private final OprfSender oprfSender;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * d
     */
    private final int d;
    /**
     * the cuckoo hash table HT
     */
    private CuckooHashBin<byte[]> cuckooHashTable;
    /**
     * h_1, ... h_d
     */
    private Prf[] binHashes;

    public Cgs22RbopprfSender(Rpc senderRpc, Party receiverParty, Cgs22RbopprfConfig config) {
        super(Cgs22RbopprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        oprfSender = OprfFactory.createOprfSender(senderRpc, receiverParty, config.getOprfConfig());
        addSubPto(oprfSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        d = config.getD();
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfSender.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void opprf(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays, targetArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfSenderOutput oprfSenderOutput = oprfSender.oprf(batchSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Sender runs OPRF");

        stopWatch.start();
        // generate garbled table keys
        List<byte[]> garbledHashTableKeysPayload = generateGarbledTableKeyPayload();
        DataPacketHeader garbledHashTableKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(garbledHashTableKeysHeader, garbledHashTableKeysPayload));
        stopWatch.stop();
        long garbledHashTableKeysTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, garbledHashTableKeysTime, "Sender sends GT keys");

        stopWatch.start();
        // generate garbled table
        List<byte[]> garbledTablePayload = generateGarbledTablePayload(oprfSenderOutput);
        DataPacketHeader garbledTableHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(garbledTableHeader, garbledTablePayload));
        stopWatch.stop();
        long garbledTableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, garbledTableTime, "Sender sends GT");

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateGarbledTableKeyPayload() {
        // set the target points
        List<byte[]> inputs = Arrays.stream(inputArrays)
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        cuckooHashTable = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, pointNum, inputs, secureRandom
        );
        // init bin hashes
        byte[][] hashKeys = cuckooHashTable.getHashKeys();
        binHashes = Arrays.stream(cuckooHashTable.getHashKeys())
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        return Arrays.stream(hashKeys).collect(Collectors.toList());
    }

    private List<byte[]> generateGarbledTablePayload(OprfSenderOutput oprfSenderOutput) {
        int binNum = cuckooHashTable.binNum();
        // Let E be a mapping that maps elements to the index of the hash function that was eventually used to
        // insert that element into HT, i.e., E(X_j(i)) = idx such that HT[h_{idx}(X_j(i))] = X_j(i).
        Map<ByteBuffer, Integer> eMap = new HashMap<>(pointNum);
        IntStream.range(0, binNum).forEach(binIndex -> {
            HashBinEntry<byte[]> binEntry = cuckooHashTable.getHashBinEntry(binIndex);
            if (binEntry != null) {
                eMap.put(ByteBuffer.wrap(binEntry.getItem()), binEntry.getHashIndex());
            }
        });
        cuckooHashTable = null;
        byte[][] garbledTable = new byte[binNum][];
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL * d);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        // for j ∈ [β] do
        batchIntStream.forEach(j -> {
            byte[][] inputArray = inputArrays[j];
            byte[][] targetArray = targetArrays[j];
            // for i ∈ [|Xj|] do
            for (int i = 0; i < inputArray.length; i++) {
                byte[] input = inputArray[i];
                byte[] target = targetArray[i];
                // Compute f_1 || f_2 || f_3 ← F(k_j, X_j(i)), where f_b ∈ {0,1}^l for all b ∈ [d].
                byte[] inputPrf = oprfSenderOutput.getPrf(j, input);
                inputPrf = prf.getBytes(inputPrf);
                // For idx ← E(X_j(i)), and pos ← h_{idx}(X_j(i)), set GT[pos] ← f_{idx} ⊕ T_j(i).
                int idx = eMap.get(ByteBuffer.wrap(input));
                int pos = binHashes[idx].getInteger(input, binNum);
                garbledTable[pos] = new byte[byteL];
                System.arraycopy(inputPrf, idx * byteL, garbledTable[pos], 0, byteL);
                BytesUtils.reduceByteArray(garbledTable[pos], l);
                BytesUtils.xori(garbledTable[pos], target);
            }
        });
        binHashes = null;
        // For every empty bin i in GT, pick r_i ← {0,1}^l and set GT[i] ← r_i.
        for (int i = 0; i < garbledTable.length; i++) {
            if (garbledTable[i] == null) {
                garbledTable[i] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        return Arrays.stream(garbledTable).collect(Collectors.toList());
    }
}

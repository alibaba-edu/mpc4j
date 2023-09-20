package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22;

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
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.AbstractUrbopprfSender;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CGS22 unbalanced related-batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Cgs22UrbopprfSender extends AbstractUrbopprfSender {
    /**
     * single-query OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * d
     */
    private final int d;
    /**
     * single-query OPRF key
     */
    private SqOprfKey sqOprfKey;
    /**
     * the cuckoo hash table HT
     */
    private CuckooHashBin<byte[]> cuckooHashTable;
    /**
     * garbled table keys
     */
    private byte[][] garbledTableKeys;
    /**
     * garbled table
     */
    private byte[][] garbledTable;

    public Cgs22UrbopprfSender(Rpc senderRpc, Party receiverParty, Cgs22UrbopprfConfig config) {
        super(Cgs22UrbopprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        sqOprfSender = SqOprfFactory.createSender(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPtos(sqOprfSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        d = config.getD();
    }

    @Override
    public void init(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException {
        setInitInput(l, inputArrays, targetArrays);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        sqOprfKey = sqOprfSender.keyGen();
        generateGarbledTable();
        // send garbled table keys
        List<byte[]> garbledHashTableKeysPayload = Arrays.stream(garbledTableKeys).collect(Collectors.toList());
        DataPacketHeader garbledHashTableKeysHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(garbledHashTableKeysHeader, garbledHashTableKeysPayload));
        garbledTableKeys = null;
        // init oprf
        sqOprfSender.init(batchSize, sqOprfKey);
        stopWatch.stop();
        long gtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, gtTime, "Sender runs OPRF + GT");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void opprf() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // send garbled table
        List<byte[]> garbledTablePayload = Arrays.stream(garbledTable).collect(Collectors.toList());
        DataPacketHeader garbledTableHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_GARBLED_TABLE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(garbledTableHeader, garbledTablePayload));
        garbledTable = null;
        stopWatch.stop();
        long garbledTableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 0, 1, garbledTableTime, "Sender sends GT");

        stopWatch.start();
        sqOprfSender.oprf(batchSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, oprfTime, "Sender runs OPRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void generateGarbledTable() {
        // set the target points
        List<byte[]> inputs = Arrays.stream(inputArrays)
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        cuckooHashTable = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, pointNum, inputs, secureRandom
        );
        garbledTableKeys = cuckooHashTable.getHashKeys();
        // set hashes
        Prf[] binHashes = Arrays.stream(garbledTableKeys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        // generate garbled table
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
        garbledTable = new byte[binNum][];
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
                byte[] inputPrf = sqOprfKey.getPrf(input);
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
        // For every empty bin i in GT, pick r_i ← {0,1}^l and set GT[i] ← r_i.
        for (int i = 0; i < garbledTable.length; i++) {
            if (garbledTable[i] == null) {
                garbledTable[i] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
    }
}

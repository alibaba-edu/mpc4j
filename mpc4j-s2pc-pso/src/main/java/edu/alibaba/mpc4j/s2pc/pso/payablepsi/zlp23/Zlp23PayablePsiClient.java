package edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.*;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.AbstractPayablePsiClient;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getBinNum;
import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getHashNum;
import static edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23.Zlp23PayablePsiPtoDesc.*;

/**
 * ZLP23 payable PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public class Zlp23PayablePsiClient<T> extends AbstractPayablePsiClient<T> {

    /**
     * prf key
     */
    private SqOprfKey sqOprfKey;
    /**
     * sqOPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * sqOPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * OSN receiver
     */
    private final OsnReceiver osnReceiver;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * hash bin num
     */
    private int binNum;
    /**
     * permutation map
     */
    private int[] permutationMap;

    public Zlp23PayablePsiClient(Rpc clientRpc, Party serverParty, Zlp23PayablePsiConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(clientRpc, serverParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        osnReceiver = OsnFactory.createReceiver(clientRpc, serverParty, config.getOsnConfig());
        addSubPto(osnReceiver);
        sqOprfSender = SqOprfFactory.createSender(clientRpc, serverParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = getHashNum(cuckooHashBinType);
    }


    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init sq-oprf receiver
        sqOprfReceiver.init(maxClientElementSize);
        // init osn receiver
        binNum = getBinNum(cuckooHashBinType, maxServerElementSize);
        osnReceiver.init(binNum);
        // init sq-oprf receiver
        sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(binNum, sqOprfKey);
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
        byte[][] clientElementByteArray = IntStream.range(0, clientElementSize)
            .mapToObj(i -> ObjectUtils.objectToByteArray(clientElementArrayList.get(i)))
            .toArray(byte[][]::new);
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(clientElementByteArray);
        Map<ByteBuffer, T> prfElementMap = IntStream.range(0, clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                i -> ByteBuffer.wrap(sqOprfReceiverOutput.getPrf(i)), i -> clientElementArrayList.get(i),
                (a, b) -> b, () -> new HashMap<>(clientElementSize)));
        stopWatch.stop();
        long firstOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, firstOprfTime, "Client executes sq-OPRF");

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == hashNum);

        stopWatch.start();
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        List<byte[][]> binItems = generateSimpleHashBin(sqOprfReceiverOutput);
        stopWatch.stop();
        long hashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, hashBinTime, "Client generates hash bin");

        stopWatch.start();
        List<Integer> shufflePermutationList = IntStream.range(0, binNum)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(shufflePermutationList, secureRandom);
        permutationMap = shufflePermutationList.stream().mapToInt(permutation -> permutation).toArray();
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(permutationMap, sqOprfReceiverOutput.getPrfByteLength());
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, osnTime, "Client executes OSN");

        stopWatch.start();
        sqOprfSender.oprf(binNum);
        stopWatch.stop();
        long secondOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, secondOprfTime, "Client executes sq-OPRF");

        stopWatch.start();
        List<byte[]> checkPayload = generateServerCheckPayload(osnPartyOutput, binItems);
        DataPacketHeader checkPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CHECK.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(checkPayloadHeader, checkPayload));
        stopWatch.stop();
        long consistencyCheckTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, consistencyCheckTime, "Client generates consistency check");

        DataPacketHeader zPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_Z.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> zPayload = rpc.receive(zPayloadHeader).getPayload();

        stopWatch.start();
        Set<T> intersectionSet = generateClientOutput(zPayload, osnPartyOutput, binItems, prfElementMap);
        stopWatch.stop();
        long handleClientOutputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, handleClientOutputTime, "Client generates intersection set");

        return intersectionSet;
    }

    private Set<T> generateClientOutput(List<byte[]> zPayload, OsnPartyOutput osnPartyOutput, List<byte[][]> binItems,
                                        Map<ByteBuffer, T> prfElementMap) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(zPayload.size() == binNum);
        byte[] abortElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(abortElementByteArray, (byte) 0x00);
        ByteBuffer abortElementByteBuffer = ByteBuffer.wrap(abortElementByteArray);
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        Set<T> intersectionSet = intStream.mapToObj(i -> {
            ByteBuffer z = ByteBuffer.wrap(zPayload.get(i));
            if (!(z.equals(abortElementByteBuffer))) {
                ByteBuffer t = ByteBuffer.wrap(BytesUtils.xor(z.array(), osnPartyOutput.getShare(i)));
                int index = permutationMap[i];
                int binSize = binItems.get(index).length;
                for (int j = 0; j < binSize; j++) {
                    if (t.equals(ByteBuffer.wrap(binItems.get(index)[j]))) {
                        return prfElementMap.get(ByteBuffer.wrap(binItems.get(index)[j]));
                    }
                }
            }
            return null;
        }).collect(Collectors.toSet());
        intersectionSet.remove(null);
        return intersectionSet;
    }

    private List<byte[][]> generateSimpleHashBin(SqOprfReceiverOutput sqOprfReceiverOutput) {
        List<ByteBuffer> itemList = IntStream.range(0, clientElementSize)
            .mapToObj(sqOprfReceiverOutput::getPrf)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(() -> new ArrayList<>(clientElementSize)));
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, clientElementSize, hashKeys);
        completeHash.insertItems(itemList);
        return IntStream.range(0, binNum)
            .mapToObj(i -> new ArrayList<>(completeHash.getBin(i)))
            .map(entries -> entries.stream()
                .map(HashBinEntry::getItemByteArray)
                .toArray(byte[][]::new))
            .collect(Collectors.toList());
    }

    private List<byte[]> generateServerCheckPayload(OsnPartyOutput osnPartyOutput, List<byte[][]> items) {
        int maxBinSize = IntStream.range(0, binNum).map(i -> items.get(i).length).max().orElse(0);
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(i -> {
            int permutationIndex = permutationMap[i];
            int binSize = items.get(permutationIndex).length;
            byte[] a = osnPartyOutput.getShare(i);
            List<byte[]> binItem = IntStream.range(0, binSize)
                .mapToObj(j -> BytesUtils.xor(a, items.get(permutationIndex)[j]))
                .map(input -> sqOprfKey.getPrf(input))
                .collect(Collectors.toList());
            IntStream.range(0, maxBinSize - binSize)
                .mapToObj(j -> new byte[sqOprfKey.getPrfByteLength()])
                .forEach(padding -> {
                    secureRandom.nextBytes(padding);
                    binItem.add(padding);
                });
            return binItem;
        })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }
}
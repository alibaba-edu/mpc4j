package edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.*;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.AbstractPayablePsiServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23.Zlp23PayablePsiPtoDesc.*;

/**
 * ZLP23 payable PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public class Zlp23PayablePsiServer<T> extends AbstractPayablePsiServer<T> {
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
     * OSN sender
     */
    private final OsnSender osnSender;
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
     * intersection set size
     */
    private int intersectionSetSize;

    public Zlp23PayablePsiServer(Rpc serverRpc, Party clientParty, Zlp23PayablePsiConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        sqOprfSender = SqOprfFactory.createSender(serverRpc, clientParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        osnSender = OsnFactory.createSender(serverRpc, clientParty, config.getOsnConfig());
        addSubPto(osnSender);
        sqOprfReceiver = SqOprfFactory.createReceiver(serverRpc, clientParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init sq-oprf sender
        sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(maxClientElementSize, sqOprfKey);
        // init osn sender
        binNum = getBinNum(cuckooHashBinType, maxServerElementSize);
        osnSender.init(binNum);
        // init sq-oprf receiver
        sqOprfReceiver.init(binNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public int psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        sqOprfSender.oprf(clientElementSize);
        List<ByteBuffer> elementPrf = computePrf();
        stopWatch.stop();
        long firstOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, firstOprfTime, "Server executes sq-OPRF");

        stopWatch.start();
        Vector<byte[]> binItems = generateCuckooHashBin(elementPrf);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashBinTime, "Server generates cuckoo hash bin");

        stopWatch.start();
        OsnPartyOutput osnPartyOutput = osnSender.osn(binItems, sqOprfKey.getPrfByteLength());
        byte[][] sharedBytes = handleOsnPartyOutput(osnPartyOutput);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, osnTime, "Server executes OSN");

        stopWatch.start();
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(sharedBytes);
        stopWatch.stop();
        long secondOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, secondOprfTime, "Server executes sq-OPRF");

        DataPacketHeader checkPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CHECK.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
        List<byte[]> checkPayload = rpc.receive(checkPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(checkPayload.size() % binNum == 0);

        stopWatch.start();
        List<byte[]> zPayload = handleConsistencyCheckPayload(sqOprfReceiverOutput, checkPayload, sharedBytes);
        DataPacketHeader zPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_Z.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(zPayloadHeader, zPayload));
        stopWatch.stop();
        long consistencyCheckTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, consistencyCheckTime, "Server handles consistency check");

        logPhaseInfo(PtoState.PTO_END);
        return intersectionSetSize;
    }

    private List<byte[]> handleConsistencyCheckPayload(SqOprfReceiverOutput sqOprfReceiverOutput,
                                                       List<byte[]> checkPayload, byte[][] sharedBytes) {
        int binSize = checkPayload.size() / binNum;
        List<ByteBuffer> byteBufferCheckPayload = checkPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        List<List<ByteBuffer>> partitions = Lists.partition(byteBufferCheckPayload, binSize);
        intersectionSetSize = 0;
        byte[] abortElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(abortElementByteArray, (byte) 0x00);
        boolean[] intersectionSetFlag = new boolean[binNum];
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> z = intStream.mapToObj(i -> {
            ByteBuffer a = ByteBuffer.wrap(sqOprfReceiverOutput.getPrf(i));
            Set<ByteBuffer> binItems = new HashSet<>(partitions.get(i));
            if (binItems.contains(a)) {
                intersectionSetFlag[i] = true;
                return sharedBytes[i];
            } else {
                return abortElementByteArray;
            }
        }).collect(Collectors.toList());
        IntStream.range(0, binNum).filter(i -> intersectionSetFlag[i]).forEach(i -> intersectionSetSize++);
        return z;
    }

    private List<ByteBuffer> computePrf() {
        Stream<T> stream = serverElementArrayList.stream();
        stream = parallel ? stream.parallel() : stream;
        return stream
            .map(ObjectUtils::objectToByteArray)
            .map(bytes -> sqOprfKey.getPrf(bytes))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    private Vector<byte[]> generateCuckooHashBin(List<ByteBuffer> items) {
        CuckooHashBin<ByteBuffer> cuckooHashBin = createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, serverElementSize, items, secureRandom
        );
        hashKeys = cuckooHashBin.getHashKeys();
        byte[] botElementByteArray = new byte[sqOprfKey.getPrfByteLength()];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        ByteBuffer botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return IntStream.range(0, binNum)
            .mapToObj(i -> cuckooHashBin.getHashBinEntry(i).getItem().array())
            .collect(Collectors.toCollection(() -> new Vector<>(binNum)));
    }

    private byte[][] handleOsnPartyOutput(OsnPartyOutput osnPartyOutput) {
        return IntStream.range(0, binNum)
            .mapToObj(osnPartyOutput::getShare)
            .toArray(byte[][]::new);
    }
}
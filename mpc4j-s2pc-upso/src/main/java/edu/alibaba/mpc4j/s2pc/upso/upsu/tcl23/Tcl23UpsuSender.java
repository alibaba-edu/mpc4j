package edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsu.AbstractUpsuSender;
import edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23.Tcl23UpsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * TCL23 UPSU sender.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public class Tcl23UpsuSender extends AbstractUpsuSender {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * single - query OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * pm-PEQT sender
     */
    private final PmPeqtSender pmPeqtSender;
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * UPSU params
     */
    private Tcl23UpsuParams params;
    /**
     * zp64
     */
    private Zp64 zp64;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * secret key
     */
    private byte[] secretKey;
    /**
     * partition count
     */
    private int alpha;

    public Tcl23UpsuSender(Rpc senderRpc, Party receiverParty, Tcl23UpsuConfig config) {
        super(Tcl23UpsuPtoDesc.getInstance(), senderRpc, receiverParty, config);
        sqOprfReceiver = SqOprfFactory.createReceiver(senderRpc, receiverParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        pmPeqtSender = PmPeqtFactory.createSender(senderRpc, receiverParty, config.getPmPeqtConfig());
        addSubPto(pmPeqtSender);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(int maxSenderElementSize, int receiverElementSize) throws MpcAbortException {
        setInitInput(maxSenderElementSize, receiverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        params = Tcl23UpsuParams.RECEIVER_16M_SENDER_MAX_1024;
        assert maxSenderElementSize <= params.maxSenderElementSize() : "the sender element size is too large";
        // init OPRF
        sqOprfReceiver.init(maxSenderElementSize);
        // create zp64 poly
        zp64 = Zp64Factory.createInstance(envType, params.getPlainModulus());
        // init pm-PEQT
        int expectBinSize = MaxBinSizeUtils.expectMaxBinSize(
            receiverElementSize * params.getCuckooHashNum(), params.getBinNum()
        );
        int expectAlpha = CommonUtils.getUnitNum(expectBinSize, params.getMaxPartitionSizePerBin());
        pmPeqtSender.init(expectAlpha, params.getBinNum());
        // init core COT
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, params.getBinNum());
        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            hashKeyPayload.size() == params.getCuckooHashNum(),
            "the size of hash keys should be {}", params.getCuckooHashNum()
        );
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        // generate key pair
        List<byte[]> keyPair = Tcl23UpsuNativeUtils.keyGen(params.getEncryptionParameters());
        byte[] relinKeys = handleKeyPair(keyPair);
        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keyPairHeader, Collections.singletonList(relinKeys)));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psu(Set<ByteBuffer> senderElementSet, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(senderElementSet, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // batch OPRF
        byte[][] oprfInput = IntStream.range(0, senderElementSize)
            .mapToObj(i -> senderElementList.get(i).array())
            .toArray(byte[][]::new);
        SqOprfReceiverOutput oprfReceiverOutput = sqOprfReceiver.oprf(oprfInput);
        Map<ByteBuffer, byte[]> oprfItemMap = handleReceiverOprfOutput(oprfReceiverOutput);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, oprfTime, "sender executes OPRF");

        stopWatch.start();
        CuckooHashBin<ByteBuffer> cuckooHashBin = generateCuckooHashBin(oprfItemMap);
        List<byte[]> senderQueryPayload = encodeQuery(cuckooHashBin);
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryHeader, senderQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, genQueryTime, "sender generates query");

        DataPacketHeader receiverResponsePayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> receiverResponsePayload = rpc.receive(receiverResponsePayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(receiverResponsePayload.size() % params.getCiphertextNum() == 0);
        alpha = receiverResponsePayload.size() / params.getCiphertextNum();

        stopWatch.start();
        int bitLength = CommonConstants.STATS_BIT_LENGTH + 2 * LongUtils.ceilLog2((long) alpha * params.getBinNum()) + 7;
        int byteLength = CommonUtils.getByteLength(bitLength);
        byte[][][] pmPeqtInput = decodeResponse(receiverResponsePayload, byteLength);
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, decodeTime, "sender decodes response");

        stopWatch.start();
        // row permutation map
        List<Integer> shufflePermutationMap = IntStream.range(0, alpha).boxed().collect(Collectors.toList());
        Collections.shuffle(shufflePermutationMap, secureRandom);
        int[] rowPermutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
        // column permutation map
        shufflePermutationMap = IntStream.range(0, params.getBinNum()).boxed().collect(Collectors.toList());
        Collections.shuffle(shufflePermutationMap, secureRandom);
        int[] columnPermutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
        pmPeqtSender.pmPeqt(pmPeqtInput, rowPermutationMap, columnPermutationMap, byteLength);
        stopWatch.stop();
        long pmPeqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, pmPeqtTime, "sender executes PM-PEQT");

        stopWatch.start();
        CotSenderOutput cotSenderOutput = coreCotSender.send(params.getBinNum());
        List<byte[]> encPayload = handleCotSenderOutput(cotSenderOutput, cuckooHashBin, oprfItemMap, columnPermutationMap);
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, encTime, "sender executes COT");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * sender handles COT sender output.
     *
     * @param cotSenderOutput      COT sender output.
     * @param cuckooHashBin        cuckoo hash bin.
     * @param oprfItemMap          OPRF item map.
     * @param columnPermutationMap column permutation map.
     * @return enc payload.
     */
    private List<byte[]> handleCotSenderOutput(CotSenderOutput cotSenderOutput, CuckooHashBin<ByteBuffer> cuckooHashBin,
                                               Map<ByteBuffer, byte[]> oprfItemMap, int[] columnPermutationMap) {
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream encIntStream = IntStream.range(0, params.getBinNum());
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        return encIntStream
            .mapToObj(index -> {
                // do not need CRHF since we call prg
                byte[] ciphertext = encPrg.extendToBytes(cotSenderOutput.getR0(index));
                ByteBuffer item = cuckooHashBin.getHashBinEntry(columnPermutationMap[index]).getItem();
                BytesUtils.xori(ciphertext, oprfItemMap.containsKey(item) ? oprfItemMap.get(item) : botElementByteBuffer.array());
                return ciphertext;
            })
            .collect(Collectors.toList());
    }

    /**
     * sender handles key pair.
     *
     * @param keyPair key pair.
     * @return relinearization keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public byte[] handleKeyPair(List<byte[]> keyPair) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(keyPair.size() == 3);
        this.secretKey = keyPair.get(1);
        return keyPair.get(2);
    }

    /**
     * sender handles OPRF output.
     *
     * @param sqOprfReceiverOutput single - query OPRF receiver output.
     * @return OPRF item map.
     */
    private Map<ByteBuffer, byte[]> handleReceiverOprfOutput(SqOprfReceiverOutput sqOprfReceiverOutput) {
        List<ByteBuffer> oprfOutput = IntStream.range(0, senderElementSize)
            .mapToObj(i -> ByteBuffer.wrap(sqOprfReceiverOutput.getPrf(i)))
            .collect(Collectors.toList());
        return IntStream.range(0, senderElementSize)
            .boxed()
            .collect(Collectors.toMap(oprfOutput::get, i -> senderElementList.get(i).array(), (a, b) -> b));
    }

    /**
     * sender generates no stash cuckoo hash bin.
     *
     * @param oprfItemMap oprf item map.
     * @return cuckoo hash bin.
     */
    private CuckooHashBin<ByteBuffer> generateCuckooHashBin(Map<ByteBuffer, byte[]> oprfItemMap) {
        CuckooHashBin<ByteBuffer> cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
            envType, params.getCuckooHashBinType(), senderElementSize, params.getBinNum(), hashKeys
        );
        cuckooHashBin.insertItems(new ArrayList<>(oprfItemMap.keySet()));
        assert cuckooHashBin.itemNumInStash() == 0;
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return cuckooHashBin;
    }

    /**
     * sender generates query.
     *
     * @param cuckooHashBin cuckoo hash bin.
     * @return query.
     */
    public List<byte[]> encodeQuery(CuckooHashBin<ByteBuffer> cuckooHashBin) {
        // encode query
        long[][] coeffs = new long[params.getCiphertextNum()][params.getPolyModulusDegree()];
        for (int i = 0; i < params.getCiphertextNum(); i++) {
            for (int j = 0; j < params.getItemPerCiphertext(); j++) {
                long[] coeff = UpsoUtils.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * params.getItemPerCiphertext() + j), true,
                    params.getItemEncodedSlotSize(), params.getPlainModulus()
                );
                System.arraycopy(coeff, 0, coeffs[i], j * params.getItemEncodedSlotSize(), params.getItemEncodedSlotSize());
            }
        }
        List<long[][]> encodedQuery = IntStream.range(0, params.getCiphertextNum())
            .mapToObj(i -> UpsoUtils.computePowers(coeffs[i], zp64, params.getQueryPowers(), parallel))
            .collect(Collectors.toCollection(ArrayList::new));
        // encrypt query
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        return encodeStream
            .map(query -> Tcl23UpsuNativeUtils.generateQuery(params.getEncryptionParameters(), secretKey, query))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * sender decodes response.
     *
     * @param receiverResponse receiver response.
     * @param byteLength       byte length.
     * @return PM-PEQT input.
     */
    public byte[][][] decodeResponse(List<byte[]> receiverResponse, int byteLength) {
        Stream<byte[]> responseStream = parallel ? receiverResponse.stream().parallel() : receiverResponse.stream();
        List<long[]> coeffs = responseStream
            .map(ciphertext -> Tcl23UpsuNativeUtils.decodeReply(params.getEncryptionParameters(), secretKey, ciphertext))
            .collect(Collectors.toCollection(ArrayList::new));
        Hash peqtHash = HashFactory.createInstance(envType, byteLength);
        byte[][][] pmPeqtInput = new byte[alpha][params.getBinNum()][];
        for (int i = 0; i < params.getBinNum(); i++) {
            for (int j = 0; j < alpha; j++) {
                int cipherIndex = i / params.getItemPerCiphertext();
                int coeffIndex = (i % params.getItemPerCiphertext()) * params.getItemEncodedSlotSize();
                long[] item = new long[params.getItemEncodedSlotSize()];
                System.arraycopy(coeffs.get(cipherIndex * alpha + j), coeffIndex, item, 0, params.getItemEncodedSlotSize());
                byte[] bytes = PirUtils.convertCoeffsToBytes(item, params.getPlainModulusSize());
                BytesUtils.reduceByteArray(bytes, params.getL());
                pmPeqtInput[j][i] = peqtHash.digestToBytes(bytes);
            }
        }
        return pmPeqtInput;
    }
}

package edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsu.AbstractUpsuReceiver;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuReceiverOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23.Tcl23UpsuPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23.Tcl23UpsuPtoDesc.getInstance;


/**
 * TCL23 UPSU receiver.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public class Tcl23UpsuReceiver extends AbstractUpsuReceiver {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * single-query OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * pm-PEQT receiver
     */
    private final PmPeqtReceiver pmPeqtReceiver;
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * UPSU params
     */
    private Tcl23UpsuParams params;
    /**
     * relinearization keys
     */
    private byte[] relinKeys;
    /**
     * zp64 poly
     */
    private Zp64Poly zp64Poly;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * partition count
     */
    private int alpha;
    /**
     * encode database
     */
    List<List<byte[]>> encodeDatabase;

    public Tcl23UpsuReceiver(Rpc receiverRpc, Party senderParty, Tcl23UpsuConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        sqOprfSender = SqOprfFactory.createSender(receiverRpc, senderParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        pmPeqtReceiver = PmPeqtFactory.createReceiver(receiverRpc, senderParty, config.getPmPeqtConfig());
        addSubPto(pmPeqtReceiver);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(Set<ByteBuffer> receiverElementSet, int maxSenderElementSize, int elementByteLength)
        throws MpcAbortException {
        setInitInput(receiverElementSet, maxSenderElementSize, elementByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        params = Tcl23UpsuParams.RECEIVER_16M_SENDER_MAX_1024;
        assert maxSenderElementSize <= params.maxSenderElementSize() : "the sender element size is too large";
        // init OPRF
        SqOprfKey sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(maxSenderElementSize, sqOprfKey);
        // create zp64 poly
        zp64Poly = Zp64PolyFactory.createInstance(envType, params.getPlainModulus());
        // init pm-PEQT
        int expectBinSize = MaxBinSizeUtils.expectMaxBinSize(
            receiverElementSize * params.getCuckooHashNum(), params.getBinNum()
        );
        int expectAlpha = CommonUtils.getUnitNum(expectBinSize, params.getMaxPartitionSizePerBin());
        pmPeqtReceiver.init(expectAlpha, params.getBinNum());
        // init core COT
        coreCotReceiver.init(params.getBinNum());
        // generate hash keys
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashNum(), secureRandom);
        DataPacketHeader hashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(hashKeyHeader, hashKeyPayload));
        // receive public keys
        DataPacketHeader relinKeysPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> relinKeysPayload = rpc.receive(relinKeysPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(relinKeysPayload.size() == 1);
        this.relinKeys = relinKeysPayload.get(0);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime, "receiver init params");

        stopWatch.start();
        // encode database
        List<long[][]> coeffs = encodeDatabase(sqOprfKey);
        IntStream intStream = IntStream.range(0, coeffs.size());
        intStream = parallel ? intStream.parallel() : intStream;
        encodeDatabase = intStream
            .mapToObj(i -> Tcl23UpsuNativeUtils.preprocessDatabase(
                params.getEncryptionParameters(), coeffs.get(i), params.getPsLowDegree()
            ))
            .collect(Collectors.toList());
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, encodeTime, "receiver encodes database");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UpsuReceiverOutput psu(int senderElementSize)
        throws MpcAbortException {
        setPtoInput(senderElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // batch OPRF
        sqOprfSender.oprf(senderElementSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, oprfTime, "receiver executes OPRF");

        // receive query
        DataPacketHeader senderQueryPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(senderQueryPayloadHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == params.getCiphertextNum() * params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        stopWatch.start();
        // generate response
        List<long[]> mask = generateRandomMask();
        List<byte[]> responsePayload = computeResponse(queryPayload, mask);
        DataPacketHeader receiverResponsePayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverResponsePayloadHeader, responsePayload));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, replyTime, "receiver generates response");

        stopWatch.start();
        int bitLength = CommonConstants.STATS_BIT_LENGTH + 2 * LongUtils.ceilLog2((long) alpha * params.getBinNum()) + 7;
        int byteLength = CommonUtils.getByteLength(bitLength);
        byte[][][] pmPeqtInput = generatePmPeqtInput(mask, byteLength);
        boolean[][] pmPeqtOutput = pmPeqtReceiver.pmPeqt(pmPeqtInput, byteLength, alpha, params.getBinNum());
        stopWatch.stop();
        long pmPeqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, pmPeqtTime, "receiver executes PM-PEQT");

        stopWatch.start();
        boolean[] choiceArray = new boolean[params.getBinNum()];
        for (int i = 0; i < params.getBinNum(); i++) {
            for (int j = 0; j < alpha; j++) {
                if (pmPeqtOutput[j][i]) {
                    choiceArray[i] = true;
                    break;
                }
            }
        }
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choiceArray);
        int intersectionSetSize = (int) IntStream.range(0, choiceArray.length).filter(i -> choiceArray[i]).count();
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == params.getBinNum());
        Set<ByteBuffer> union = handleEncPayload(encPayload, choiceArray, cotReceiverOutput);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, cotTime, "receiver executes COT");

        logPhaseInfo(PtoState.PTO_END);
        return new UpsuReceiverOutput(union, intersectionSetSize);
    }

    /**
     * receiver encodes database.
     *
     * @param sqOprfKey single - query OPRF key
     * @return encoded database.
     */
    private List<long[][]> encodeDatabase(SqOprfKey sqOprfKey) {
        Stream<ByteBuffer> receiverInputStream = receiverElementList.stream();
        receiverInputStream = parallel ? receiverInputStream.parallel() : receiverInputStream;
        List<ByteBuffer> inputPrfs = receiverInputStream
            .map(ByteBuffer::array)
            .map(sqOprfKey::getPrf)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(
            envType, params.getBinNum(), receiverElementSize, hashKeys
        );
        completeHash.insertItems(inputPrfs);
        int maxBinSize = IntStream.range(0, params.getBinNum()).map(completeHash::binSize).max().orElse(0);
        alpha = CommonUtils.getUnitNum(maxBinSize, params.getMaxPartitionSizePerBin());
        List<List<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        for (int i = 0; i < completeHash.binNum(); i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        inputPrfs.clear();
        return UpsoUtils.encodeDatabase(
            zp64Poly, completeHashBins, maxBinSize, params.getPlainModulus(), params.getMaxPartitionSizePerBin(),
            params.getItemEncodedSlotSize(), params.getItemPerCiphertext(), params.getBinNum(),
            params.getCiphertextNum(), params.getPolyModulusDegree(), parallel
        );
    }

    /**
     * receiver generates response.
     *
     * @param queryList query list.
     * @param mask      mask.
     * @return response.
     */
    private List<byte[]> computeResponse(List<byte[]> queryList, List<long[]> mask) {
        int[][] powerDegree = UpsoUtils.computePowerDegree(
            params.getPsLowDegree(), params.getQueryPowers(), params.getMaxPartitionSizePerBin()
        );
        IntStream intStream = IntStream.range(0, params.getCiphertextNum());
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> queryPowers = intStream
            .mapToObj(i -> Tcl23UpsuNativeUtils.computeEncryptedPowers(
                params.getEncryptionParameters(),
                relinKeys,
                queryList.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree())
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            return IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, alpha).parallel() : IntStream.range(0, alpha))
                        .mapToObj(j -> Tcl23UpsuNativeUtils.optComputeMatches(
                            params.getEncryptionParameters(),
                            relinKeys,
                            encodeDatabase.get(i * alpha + j),
                            queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            params.getPsLowDegree(),
                            mask.get(i * alpha + j))
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            return IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, alpha).parallel() : IntStream.range(0, alpha))
                        .mapToObj(j -> Tcl23UpsuNativeUtils.naiveComputeMatches(
                            params.getEncryptionParameters(),
                            encodeDatabase.get(i * alpha + j),
                            queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            mask.get(i * alpha + j))
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        }
    }

    /**
     * receiver generates random mask.
     *
     * @return random mask.
     */
    private List<long[]> generateRandomMask() {
        List<long[]> coeffList = new ArrayList<>();
        for (int i = 0; i < params.getCiphertextNum(); i++) {
            for (int j = 0; j < alpha; j++) {
                long[] r = IntStream.range(0, params.getPolyModulusDegree())
                    .mapToLong(l -> Math.abs(secureRandom.nextLong()) % params.getPlainModulus())
                    .toArray();
                coeffList.add(r);
            }
        }
        return coeffList;
    }

    /**
     * receiver generates PM-PEQT input.
     *
     * @param mask       random mask.
     * @param byteLength byte length.
     * @return PM-PEQT input.
     */
    private byte[][][] generatePmPeqtInput(List<long[]> mask, int byteLength) {
        Hash peqtHash = HashFactory.createInstance(envType, byteLength);
        byte[][][] pmPeqtInput = new byte[alpha][params.getBinNum()][];
        for (int i = 0; i < params.getBinNum(); i++) {
            for (int j = 0; j < alpha; j++) {
                int cipherIndex = i  / params.getItemPerCiphertext();
                int coeffIndex = (i % params.getItemPerCiphertext()) * params.getItemEncodedSlotSize();
                long[] item = new long[params.getItemEncodedSlotSize()];
                System.arraycopy(mask.get(cipherIndex * alpha + j), coeffIndex, item, 0, params.getItemEncodedSlotSize());
                byte[] bytes = UpsoUtils.convertCoeffsToBytes(item, params.getPlainModulusSize());
                BytesUtils.reduceByteArray(bytes, params.getL());
                pmPeqtInput[j][i] = peqtHash.digestToBytes(bytes);
            }
        }
        return pmPeqtInput;
    }

    /**
     * receiver handles enc payload.
     *
     * @param encPayload        enc payload.
     * @param choiceArray       choice array.
     * @param cotReceiverOutput cot receiver output.
     * @return difference set of receiver set in sender set.
     */
    private Set<ByteBuffer> handleEncPayload(List<byte[]> encPayload, boolean[] choiceArray,
                                             CotReceiverOutput cotReceiverOutput) {
        List<byte[]> encArrayList = new ArrayList<>(encPayload);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = IntStream.range(0, params.getBinNum());
        decIntStream = parallel ? decIntStream.parallel() : decIntStream;
        Set<ByteBuffer> union = decIntStream
            .mapToObj(index -> {
                if (choiceArray[index]) {
                    return botElementByteBuffer;
                } else {
                    // do not need CRHF since we call prg
                    byte[] message = encPrg.extendToBytes(cotReceiverOutput.getRb(index));
                    BytesUtils.xori(message, encArrayList.get(index));
                    return ByteBuffer.wrap(message);
                }
            })
            .collect(Collectors.toSet());
        union.remove(botElementByteBuffer);
        union.addAll(receiverElementList);
        return union;
    }
}
package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.AbstractUpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiParams;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CMG21 UPSI server.
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class Cmg21UpsiServer<T> extends AbstractUpsiServer<T> {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * MP-OPRF sender
     */
    private final MpOprfSender mpOprfSender;
    /**
     * UPSI params
     */
    public Cmg21UpsiParams params;
    /**
     * zp64
     */
    private Zp64Poly zp64Poly;

    public Cmg21UpsiServer(Rpc serverRpc, Party clientParty, Cmg21UpsiConfig config) {
        super(Cmg21UpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfSender);
    }

    @Override
    public void init(UpsiParams upsiParams) throws MpcAbortException {
        setInitInput(upsiParams.maxClientElementSize());
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        assert (upsiParams instanceof Cmg21UpsiParams);
        params = (Cmg21UpsiParams) upsiParams;
        mpOprfSender.init(params.maxClientElementSize());
        zp64Poly = Zp64PolyFactory.createInstance(envType, params.getPlainModulus());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_5535;
        mpOprfSender.init(params.maxClientElementSize());
        zp64Poly = Zp64PolyFactory.createInstance(envType, params.getPlainModulus());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // MP-OPRF
        List<ByteBuffer> prfOutputList = oprf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, oprfTime, "OPRF");

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        MpcAbortPreconditions.checkArgument(
            hashKeyPayload.size() == params.getCuckooHashNum(),
            "the size of hash keys " + "should be {}", params.getCuckooHashNum()
        );
        byte[][] hashKeys = hashKeyPayload.toArray(new byte[0][]);

        stopWatch.start();
        List<List<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(prfOutputList, hashKeys);
        int binSize = hashBins.get(0).size();
        List<long[][]> encodeDatabase = UpsoUtils.encodeDatabase(
            zp64Poly, hashBins, binSize, params.getPlainModulus(), params.getMaxPartitionSizePerBin(),
            params.getItemEncodedSlotSize(), params.getItemPerCiphertext(), params.getBinNum(),
            params.getCiphertextNum(), params.getPolyModulusDegree(), parallel
        );
        hashBins.clear();
        stopWatch.stop();
        long encodedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, encodedTime, "Server encodes database");

        DataPacketHeader encryptionParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> encryptionParamsPayload = rpc.receive(encryptionParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            encryptionParamsPayload.size() == 2, "the size of encryption parameters should be 2"
        );
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload =rpc.receive(queryHeader).getPayload();

        stopWatch.start();
        List<byte[]> responsePayload = computeResponse(
            encodeDatabase, queryPayload, encryptionParamsPayload.get(0), encryptionParamsPayload.get(1), binSize
        );
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        logStepInfo(PtoState.PTO_STEP, 3, 3, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate complete hash bin.
     *
     * @param elementList element list.
     * @param hashKeys    hash keys.
     * @return complete hash bin.
     */
    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(List<ByteBuffer> elementList,
                                                                         byte[][] hashKeys) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(
            envType, params.getBinNum(), serverElementSize, hashKeys
        );
        completeHash.insertItems(elementList);
        int maxBinSize = IntStream.range(0, params.getBinNum()).map(completeHash::binSize).max().orElse(0);
        List<List<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        for (int i = 0; i < completeHash.binNum(); i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        elementList.clear();
        return completeHashBins;
    }

    /**
     * server executes MP-OPRF protocol.
     *
     * @return MP-OPRF output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<ByteBuffer> oprf() throws MpcAbortException {
        MpOprfSenderOutput oprfSenderOutput = mpOprfSender.oprf(clientElementSize);
        IntStream intStream = IntStream.range(0, serverElementSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return new ArrayList<>(Arrays.asList(intStream
            .mapToObj(i -> ByteBuffer.wrap(oprfSenderOutput.getPrf(serverElementList.get(i).array())))
            .toArray(ByteBuffer[]::new)));
    }

    /**
     * server generate response.
     *
     * @param database         database.
     * @param queryList        query list.
     * @param encryptionParams encryption params.
     * @param relinKeys        relinearization keys.
     * @param binSize          bin size.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> computeResponse(List<long[][]> database, List<byte[]> queryList, byte[] encryptionParams,
                                         byte[] relinKeys, int binSize) throws MpcAbortException {
        int partitionCount = CommonUtils.getUnitNum(binSize, params.getMaxPartitionSizePerBin());
        MpcAbortPreconditions.checkArgument(
            queryList.size() == params.getCiphertextNum() * params.getQueryPowers().length,
            "The size of query is incorrect"
        );
        int[][] powerDegree = UpsoUtils.computePowerDegree(
            params.getPsLowDegree(), params.getQueryPowers(), params.getMaxPartitionSizePerBin()
        );
        IntStream intStream = IntStream.range(0, params.getCiphertextNum());
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> queryPowers = intStream
            .mapToObj(i -> Cmg21UpsiNativeUtils.computeEncryptedPowers(
                encryptionParams,
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
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Cmg21UpsiNativeUtils.optComputeMatches(
                            encryptionParams,
                            relinKeys,
                            database.get(i * partitionCount + j),
                            queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            params.getPsLowDegree())
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else if (params.getPsLowDegree() == 0) {
            return IntStream.range(0, params.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Cmg21UpsiNativeUtils.naiveComputeMatches(
                                encryptionParams,
                                database.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)
                            )
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree is incorrect");
        }
    }
}

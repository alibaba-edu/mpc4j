package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirPtoDesc.*;

/**
 * vectorized batch index PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirServer extends AbstractBatchIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * vectorized Batch PIR params
     */
    private Mr23BatchIndexPirParams params;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;
    /**
     * Relin Keys
     */
    private byte[] relinKeys;
    /**
     * plaintext in NTT form
     */
    private List<List<byte[]>> encodedDatabase;
    /**
     * simple hash bin
     */
    List<List<HashBinEntry<Integer>>> completeHashBins;
    /**
     * group bin size
     */
    private int groupBinSize;

    public Mr23BatchIndexPirServer(Rpc serverRpc, Party clientParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        params = Mr23BatchIndexPirParams.getParams(database.rows(), maxRetrievalSize);
        MpcAbortPreconditions.checkArgument(params != null);
        setInitInput(database, params.getMaxRetrievalSize());
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        databases = database.partitionZl(params.getPlainModulusBitLength() - 1);
        partitionSize = CommonUtils.getUnitNum(elementBitLength, params.getPlainModulusBitLength() - 1);
        hashKeys = CommonUtils.generateRandomKeys(params.getHashNum(), secureRandom);
        int maxBinSize = generateSimpleHashBin();
        checkDimensionLength(maxBinSize);
        groupBinSize = (params.getPolyModulusDegree() / 2) / params.getFirstTwoDimensionSize();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> keyPairPayload = rpc.receive(keyPairHeader).getPayload();
        handleKeyPairPayload(keyPairPayload);

        stopWatch.start();
        encodedDatabase = new ArrayList<>();
        for (int partitionIndex = 0; partitionIndex < partitionSize; partitionIndex++) {
            // vectorized batch pir setup
            IntStream intStream = IntStream.range(0, completeHashBins.size());
            intStream = parallel ? intStream.parallel() : intStream;
            int finalPartitionIndex = partitionIndex;
            List<long[][]> coeffs = intStream
                .mapToObj(i -> vectorizedPirSetup(i, completeHashBins.get(i), finalPartitionIndex, maxBinSize))
                .collect(Collectors.toList());
            List<long[][]> mergedCoeffs = batchPirSetup(coeffs);
            IntStream stream = IntStream.range(0, mergedCoeffs.size());
            stream = parallel ? stream.parallel() : stream;
            encodedDatabase.addAll(
                stream.mapToObj(i -> Mr23BatchIndexPirNativeUtils.preprocessDatabase(
                    params.getEncryptionParams(), mergedCoeffs.get(i), params.getFirstTwoDimensionSize())
                    )
                .collect(Collectors.toList()));
        }
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive client query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        int count = CommonUtils.getUnitNum(completeHashBins.size() / 2, groupBinSize);
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == count * params.getDimension());

        // generate response
        stopWatch.start();
        List<byte[]> mergedResponse = new ArrayList<>();
        for (int partitionIndex = 0; partitionIndex < partitionSize; partitionIndex++) {
            IntStream intStream = IntStream.range(0, count);
            intStream = parallel ? intStream.parallel() : intStream;
            int finalPartitionIndex = partitionIndex * count;
            List<byte[]> response = intStream
                .mapToObj(i ->
                    Mr23BatchIndexPirNativeUtils.generateReply(
                        params.getEncryptionParams(),
                        clientQueryPayload.subList(i * params.getDimension(), (i + 1) * params.getDimension()),
                        encodedDatabase.get(finalPartitionIndex + i),
                        publicKey,
                        relinKeys,
                        galoisKeys,
                        params.getFirstTwoDimensionSize()
                    ))
                .collect(Collectors.toList());
            // merge response ciphertexts
            mergedResponse.add(Mr23BatchIndexPirNativeUtils.mergeResponse(
                params.getEncryptionParams(), galoisKeys, response, groupBinSize
                )
            );
        }
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, mergedResponse));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate simple hash bin.
     *
     * @return max bin size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private int generateSimpleHashBin() throws MpcAbortException {
        int binNum = IntCuckooHashBinFactory.getBinNum(
            IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, params.getMaxRetrievalSize()
        );
        if (binNum % 2 == 1) {
            binNum = binNum + 1;
        }
        MpcAbortPreconditions.checkArgument(params.getPolyModulusDegree() >= binNum);
        List<Integer> totalIndexList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toCollection(() -> new ArrayList<>(num)));
        RandomPadHashBin<Integer> completeHash = new RandomPadHashBin<>(envType, binNum, num, hashKeys);
        completeHash.insertItems(totalIndexList);
        int maxBinSize = completeHash.binSize(0);
        for (int i = 1; i < completeHash.binNum(); i++) {
            if (completeHash.binSize(i) > maxBinSize) {
                maxBinSize = completeHash.binSize(i);
            }
        }
        completeHashBins = new ArrayList<>();
        HashBinEntry<Integer> paddingEntry = HashBinEntry.fromEmptyItem(-1);
        for (int i = 0; i < completeHash.binNum(); i++) {
            List<HashBinEntry<Integer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return maxBinSize;
    }

    /**
     * vectorized PIR setup.
     *
     * @param binIndex       bin index.
     * @param binItems       bin items.
     * @param partitionIndex partition index.
     * @param binSize        bin size.
     * @return plaintexts.
     */
    private long[][] vectorizedPirSetup(int binIndex, List<HashBinEntry<Integer>> binItems, int partitionIndex,
                                        int binSize) {
        long[] items = new long[binSize];
        IntStream.range(0, binSize).forEach(i -> {
            if (binItems.get(i).getHashIndex() == -1) {
                items[i] = 0L;
            } else {
                items[i] = IntUtils.fixedByteArrayToNonNegInt(databases[partitionIndex].getBytesData(binItems.get(i).getItem()));
            }
        });
        int dimLength = params.getFirstTwoDimensionSize();
        int size = CommonUtils.getUnitNum(binSize, dimLength);
        int length = dimLength;
        int offset = (binIndex < (completeHashBins.size() / 2)) ? 0 : (params.getPolyModulusDegree() / 2);
        long[][] coeffs = new long[size][params.getPolyModulusDegree()];
        for (int i = 0; i < size; i++) {
            long[] temp = new long[params.getPolyModulusDegree()];
            if (i == (size - 1)) {
                length = binSize - dimLength * i;
            }
            for (int j = 0; j < length; j++) {
                temp[j * groupBinSize + offset] = items[i * dimLength + j];
            }
            coeffs[i] = PirUtils.plaintextRotate(temp, (i % dimLength) * groupBinSize);
        }
        return coeffs;
    }

    /**
     * vectorized batch pir setup.
     *
     * @param binCoeffs bin coefficients.
     * @return merged coefficients.
     */
    private List<long[][]> batchPirSetup(List<long[][]> binCoeffs) {
        int binNum = completeHashBins.size();
        int count = CommonUtils.getUnitNum(binNum / 2, groupBinSize);
        List<long[][]> mergedDatabase = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long[][] vec = new long[binCoeffs.get(0).length][params.getPolyModulusDegree()];
            for (int j = 0; j < groupBinSize; j++) {
                if ((i * groupBinSize + j) >= (binNum / 2)) {
                    break;
                } else {
                    long[][] temp = PirUtils.plaintextRotate(binCoeffs.get(i * groupBinSize + j), j);
                    for (int l = 0; l < temp.length; l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            vec[l][k] += temp[l][k];
                        }
                    }
                    temp = PirUtils.plaintextRotate(
                        binCoeffs.get(i * groupBinSize + j + binNum / 2), j
                    );
                    for (int l = 0; l < temp.length; l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            vec[l][k] += temp[l][k];
                        }
                    }
                }
            }
            mergedDatabase.add(vec);
        }
        return mergedDatabase;
    }

    /**
     * handle key pair payload.
     *
     * @param keyPairPayload key pair payload.
     * @exception MpcAbortException the protocol failure aborts.
     */
    private void handleKeyPairPayload(List<byte[]> keyPairPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(keyPairPayload.size() == 3);
        publicKey = keyPairPayload.remove(0);
        relinKeys = keyPairPayload.remove(0);
        galoisKeys = keyPairPayload.remove(0);
    }

    /**
     * check the validity of the dimension length.
     *
     * @param elementSize bin element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void checkDimensionLength(int elementSize)
        throws MpcAbortException {
        int product =
            params.getFirstTwoDimensionSize() * params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
        MpcAbortPreconditions.checkArgument(product >= elementSize);
    }
}

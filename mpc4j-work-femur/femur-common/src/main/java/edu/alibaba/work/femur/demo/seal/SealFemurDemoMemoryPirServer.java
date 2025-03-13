package edu.alibaba.work.femur.demo.seal;

import com.carrotsearch.hppc.LongArrayList;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.FemurSealPirNativeUtils;
import edu.alibaba.work.femur.FemurSealPirParams;
import edu.alibaba.work.femur.demo.AbstractFemurDemoPirServer;
import edu.alibaba.work.femur.demo.FemurStatus;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.IntStream;

/**
 * SEAL Femur demo PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class SealFemurDemoMemoryPirServer extends AbstractFemurDemoPirServer {

    static {
        System.loadLibrary("femur-native-fhe");
    }

    /**
     * params
     */
    private final FemurSealPirParams params;
    /**
     * plaintext size
     */
    private int plaintextSize;
    /**
     * element size per plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * BFV plaintext in NTT form, which should be stored in Redis
     */
    private List<byte[][]> encodedDatabase;
    /**
     * version
     */
    private long version;
    /**
     * query payload size
     */
    private int queryPayloadSize;
    /**
     * clients, which should be stored in Redis
     */
    private Map<String, byte[]> clients;
    /**
     * write lock
     */
    private final WriteLock writeLock;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;
    /**
     * partition num
     */
    private int partitionNum;
    /**
     * partition byte l
     */
    private int partitionByteL;

    public SealFemurDemoMemoryPirServer(SealFemurDemoMemoryPirConfig config) {
        super(config);
        params = config.getParams();
        version = 0L;
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void init(int n, int l) {
        setInitInput(n, l);
        clients = new HashMap<>();
        // number of coefficients that can store one element
        partitionNum = 0;
        int partitionL;
        do {
            partitionNum++;
            partitionL = CommonUtils.getUnitNum(l + Long.SIZE, partitionNum);
            partitionByteL = CommonUtils.getByteLength(partitionL);
            int coeffSizeOfElement = CommonUtils.getUnitNum(partitionByteL * Byte.SIZE, params.getPlainModulusBitLength());
            elementSizeOfPlaintext = (params.getPolyModulusDegree() / 2) / coeffSizeOfElement;
        } while (elementSizeOfPlaintext < LongApproxPgmIndex.bound(pgmIndexLeafEpsilon));
        plaintextSize = CommonUtils.getUnitNum(n, elementSizeOfPlaintext);
        // query size
        queryPayloadSize = params.getDimension();
    }

    @Override
    public void setDatabase(TLongObjectMap<byte[]> keyValueDatabase) {
        checkSetDatabaseInput(keyValueDatabase);
        // create PGM-index
        long[] sortedKeys = keyValueDatabase.keys();
        Arrays.sort(sortedKeys);
        LongArrayList keyList = new LongArrayList();
        keyList.add(sortedKeys, 0, n);
        LongApproxPgmIndex.LongApproxPgmIndexBuilder builder = new LongApproxPgmIndex.LongApproxPgmIndexBuilder()
            .setSortedKeys(keyList)
            .setEpsilon(pgmIndexLeafEpsilon)
            .setEpsilonRecursive(CommonConstants.PGM_INDEX_RECURSIVE_EPSILON);
        // preprocess database
        List<byte[][]> partitionDatabase = new ArrayList<>();
        byte[][] database = new byte[n][partitionNum * partitionByteL];
        for (int i = 0; i < n; i++) {
            byte[] key = LongUtils.longToByteArray(sortedKeys[i]);
            assert key.length == Long.BYTES;
            byte[] value = keyValueDatabase.get(sortedKeys[i]);
            assert value.length == byteL;
            System.arraycopy(key, 0, database[i], 0, Long.BYTES);
            System.arraycopy(value, 0, database[i], 8, byteL);
        }
        for (int i = 0; i < partitionNum; i++) {
            byte[][] temp = new byte[n][partitionByteL];
            for (int j = 0; j < n; j++) {
                System.arraycopy(database[j], i * partitionByteL, temp[j], 0, partitionByteL);
            }
            partitionDatabase.add(temp);
        }
        List<byte[][]> newEncodedDatabase = IntStream.range(0, partitionNum)
            .mapToObj(i -> preprocessDatabase(partitionDatabase.get(i)))
            .toList();
        LongApproxPgmIndex pgmIndex = builder.build();
        writeLock.lock();
        pgmIndexPair = Pair.of(Long.toUnsignedString(version), pgmIndex);
        encodedDatabase = newEncodedDatabase;
        version++;
        writeLock.unlock();
    }

    @Override
    public Pair<FemurStatus, List<byte[]>> register(List<byte[]> registerPayload) {
        if (!init) {
            return Pair.of(FemurStatus.SERVER_NOT_INIT, new LinkedList<>());
        }
        if (pgmIndexPair == null) {
            return Pair.of(FemurStatus.SERVER_NOT_KVDB, new LinkedList<>());
        }
        MathPreconditions.checkEqual("registerPayload.size()", "1", registerPayload.size(), 2);
        String clientId = new String(registerPayload.get(0), CommonConstants.DEFAULT_CHARSET);
        clients.put(clientId, registerPayload.get(1));
        List<byte[]> paramsPayload = new ArrayList<>();
        paramsPayload.add(IntUtils.intToByteArray(n));
        paramsPayload.add(IntUtils.intToByteArray(l));
        return Pair.of(FemurStatus.SERVER_SUCC_RES, paramsPayload);
    }

    @Override
    public Pair<FemurStatus, List<byte[]>> response(List<byte[]> queryPayload) {
        MathPreconditions.checkEqual("queryPayload.size()", "1", queryPayload.size(), queryPayloadSize + 4);
        String clientId = new String(queryPayload.get(0), CommonConstants.DEFAULT_CHARSET);
        // client does not register
        writeLock.lock();
        if (!clients.containsKey(clientId)) {
            writeLock.unlock();
            return Pair.of(FemurStatus.CLIENT_NOT_REGS, new LinkedList<>());
        }
        byte[] galoisKeys = clients.get(clientId);
        writeLock.unlock();
        String version = new String(queryPayload.get(1), CommonConstants.DEFAULT_CHARSET);
        int leftRange = IntUtils.byteArrayToInt(queryPayload.get(queryPayloadSize + 2));
        int rangePlaintextSize = IntUtils.byteArrayToInt(queryPayload.get(queryPayloadSize + 3));
        int[] rangeDimensionSize = FemurSealPirNativeUtils.computeDimensionLength(rangePlaintextSize, params.getDimension());
        int prod = Arrays.stream(rangeDimensionSize).reduce(1, (a, b) -> a * b);
        byte[][] rangeEncodeDatabase = new byte[prod][];
        // PGM-index version mismatch
        if (!version.equals(pgmIndexPair.getKey())) {
            return Pair.of(FemurStatus.HINT_V_MISMATCH, new LinkedList<>());
        } else {
            List<byte[]> responsePayload = new ArrayList<>();
            for (int i = 0; i < partitionNum; i++) {
                int finalI = i;
                writeLock.lock();
                IntStream.range(0, prod)
                    .forEach(j -> {
                        int idx = (leftRange + j) % plaintextSize;
                        if (idx < 0) {
                            idx = idx + plaintextSize;
                        }
                        rangeEncodeDatabase[j] = Arrays.copyOf(encodedDatabase.get(finalI)[idx],
                            encodedDatabase.get(finalI)[idx].length);
                    });
                writeLock.unlock();
                responsePayload.addAll(FemurSealPirNativeUtils.generateReply(
                    params.getEncryptionParams(),
                    galoisKeys,
                    queryPayload.subList(2, queryPayloadSize + 2),
                    rangeEncodeDatabase,
                    rangeDimensionSize
                ));
            }
            return Pair.of(FemurStatus.SERVER_SUCC_RES, responsePayload);
        }
    }

    @Override
    public void reset() {
        writeLock.lock();
        innerReset();
        version = 0;
        encodedDatabase = null;
        if (clients != null) {
            clients.clear();
            clients = null;
        }
        writeLock.unlock();
    }

    @Override
    public boolean updateValue(long key, byte[] value) {
        byte[] ele = new byte[partitionNum * partitionByteL];
        byte[] keyBytes = LongUtils.longToByteArray(key);
        assert keyBytes.length == Long.BYTES;
        assert value.length == byteL;
        System.arraycopy(keyBytes, 0, ele, 0, Long.BYTES);
        System.arraycopy(value, 0, ele, 8, byteL);
        byte[][] partitionEle = new byte[partitionNum][partitionByteL];
        IntStream.range(0, partitionNum)
            .forEach(i -> System.arraycopy(ele, i * partitionByteL, partitionEle[i], 0, partitionByteL));
        TIntList idxList = getIdxList(key);
        if (!idxList.isEmpty()) {
            List<byte[][]> nttPlaintexts = IntStream.range(0, partitionNum)
                .mapToObj(i ->
                    IntStream.range(0, idxList.size())
                        .mapToObj(j -> encodedDatabase.get(i)[idxList.get(j)])
                        .toArray(byte[][]::new))
                .toList();
            List<long[][]> coeffsArr = IntStream.range(0, partitionNum)
                .mapToObj(i -> FemurSealPirNativeUtils.transformFromNtt(params.getEncryptionParams(), nttPlaintexts.get(i)))
                .toList();
            // we find the plaintext indexes that needs to be updated, put updated results
            List<long[]> coeffsList = new ArrayList<>();
            TIntList updateIdx = new TIntArrayList();
            for (int i = 0; i < idxList.size(); i++) {
                int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteL;
                byte[] paddingBytes = new byte[byteSizeOfPlaintext * 2];
                byte[] bytes = FemurSealPirNativeUtils.convertCoeffsToBytes(
                    coeffsArr.get(0)[i], params.getPlainModulusBitLength()
                );
                System.arraycopy(bytes, 0, paddingBytes, 0, Math.min(bytes.length, byteSizeOfPlaintext * 2));
                for (int j = 0; j < elementSizeOfPlaintext * 2; j++) {
                    byte[] temp = new byte[Long.BYTES];
                    System.arraycopy(paddingBytes, j * partitionByteL, temp, 0, Long.BYTES);
                    long originalKey = LongUtils.byteArrayToLong(temp);
                    if (originalKey == key) {
                        updateIdx.add(idxList.get(i));
                        for (int k = 0; k < partitionByteL - Long.BYTES; k++) {
                            paddingBytes[Long.BYTES + k + j * partitionByteL] = value[k];
                        }
                        long[] coeffs = FemurSealPirNativeUtils.convertBytesToCoeffs(
                            byteSizeOfPlaintext * 2, params.getPlainModulusBitLength(), paddingBytes
                        );
                        assert (coeffs.length <= params.getPolyModulusDegree());
                        long[] paddingCoeffsArray = new long[params.getPolyModulusDegree()];
                        System.arraycopy(coeffs, 0, paddingCoeffsArray, 0, coeffs.length);
                        // Pad the rest with 1s
                        IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(k -> paddingCoeffsArray[k] = 1L);
                        coeffsList.add(paddingCoeffsArray);
                        for (int k = 1; k < partitionNum; k++) {
                            paddingBytes = new byte[byteSizeOfPlaintext * 2];
                            bytes = FemurSealPirNativeUtils.convertCoeffsToBytes(
                                coeffsArr.get(k)[i], params.getPlainModulusBitLength()
                            );
                            System.arraycopy(bytes, 0, paddingBytes, 0, Math.min(bytes.length, byteSizeOfPlaintext * 2));
                            for (int m = 0; m < partitionByteL; m++) {
                                if (partitionByteL - Long.BYTES + (k - 1) * partitionByteL + m < byteL) {
                                    paddingBytes[m + j * partitionByteL] = value[partitionByteL - Long.BYTES + (k - 1) * partitionByteL + m];
                                }
                            }
                            coeffs = FemurSealPirNativeUtils.convertBytesToCoeffs(
                                byteSizeOfPlaintext * 2, params.getPlainModulusBitLength(), paddingBytes
                            );
                            assert (coeffs.length <= params.getPolyModulusDegree());
                            long[] partitionPaddingCoeffsArray = new long[params.getPolyModulusDegree()];
                            System.arraycopy(coeffs, 0, partitionPaddingCoeffsArray, 0, coeffs.length);
                            // Pad the rest with 1s
                            IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(m -> partitionPaddingCoeffsArray[m] = 1L);
                            coeffsList.add(partitionPaddingCoeffsArray);
                        }
                        break;
                    }
                }
            }
            // do NTT for updated plaintexts
            List<byte[]> plaintexts = FemurSealPirNativeUtils.transformToNtt(params.getEncryptionParams(), coeffsList);
            // update database
            for (int i = 0; i < updateIdx.size(); i++) {
                for (int j = 0; j < partitionNum; j++) {
                    encodedDatabase.get(j)[updateIdx.get(i)] = plaintexts.get(i * partitionNum + j);
                }
            }
            return true;
        }
        return false;
    }

    private TIntList getIdxList(long key) {
        TIntList idxList = new TIntArrayList();
        int[] range = pgmIndexPair.getValue().approximateIndexRangeOf(key);
        if (range[0] >= 0) {
            int leftBound = range[1] / elementSizeOfPlaintext;
            int rightBound = range[2] / elementSizeOfPlaintext;
            for (int i = leftBound; i <= rightBound; i++) {
                idxList.add(i);
            }
        }
        return idxList;
    }

    private byte[][] preprocessDatabase(byte[][] partitionDatabase) {
        // number of FV plaintexts needed to create the d-dimensional matrix
        int[] dimensionSize = FemurSealPirNativeUtils.computeDimensionLength(plaintextSize, params.getDimension());
        int prod = Arrays.stream(dimensionSize).reduce(1, (a, b) -> a * b);
        assert (plaintextSize <= prod);
        int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteL;
        List<long[]> coeffsList = new ArrayList<>();
        for (int i = 0; i < plaintextSize; i++) {
            byte[] processByte = new byte[byteSizeOfPlaintext * 2];
            for (int j = 0; j < elementSizeOfPlaintext; j++) {
                byte[] bytes = new byte[partitionByteL];
                if (i * elementSizeOfPlaintext + j < n) {
                    System.arraycopy(partitionDatabase[i * elementSizeOfPlaintext + j],
                        0,
                        bytes,
                        0,
                        partitionByteL);
                } else {
                    secureRandom.nextBytes(bytes);
                }
                System.arraycopy(bytes, 0, processByte, j * partitionByteL, partitionByteL);
            }
            if (i < plaintextSize - 1) {
                for (int j = 0; j < elementSizeOfPlaintext; j++) {
                    byte[] bytes = new byte[partitionByteL];
                    if (elementSizeOfPlaintext + i * elementSizeOfPlaintext + j < n) {
                        System.arraycopy(partitionDatabase[elementSizeOfPlaintext + i * elementSizeOfPlaintext + j],
                            0,
                            bytes,
                            0,
                            partitionByteL);
                    } else {
                        secureRandom.nextBytes(bytes);
                    }
                    System.arraycopy(bytes,
                        0,
                        processByte,
                        byteSizeOfPlaintext + j * partitionByteL,
                        partitionByteL);
                }
            } else {
                for (int j = 0; j < elementSizeOfPlaintext; j++) {
                    byte[] bytes = new byte[partitionByteL];
                    secureRandom.nextBytes(bytes);
                    System.arraycopy(bytes,
                        0,
                        processByte,
                        byteSizeOfPlaintext + j * partitionByteL,
                        partitionByteL);
                }
            }
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffs = FemurSealPirNativeUtils.convertBytesToCoeffs(
                byteSizeOfPlaintext * 2, params.getPlainModulusBitLength(), processByte
            );
            assert (coeffs.length <= params.getPolyModulusDegree());
            long[] paddingCoeffsArray = new long[params.getPolyModulusDegree()];
            System.arraycopy(coeffs, 0, paddingCoeffsArray, 0, coeffs.length);
            // Pad the rest with 1s
            IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        assert (currentPlaintextSize <= plaintextSize);
        IntStream.range(0, (prod - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, params.getPolyModulusDegree()).mapToLong(j -> 1L).toArray())
            .forEach(coeffsList::add);
        return FemurSealPirNativeUtils.transformToNtt(params.getEncryptionParams(), coeffsList).toArray(new byte[0][]);
    }
}
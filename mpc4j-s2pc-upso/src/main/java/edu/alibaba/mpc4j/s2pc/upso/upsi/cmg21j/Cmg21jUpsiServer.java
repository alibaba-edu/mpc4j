package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.AbstractUpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiParams;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j.Cmg21jUpsiPtoDesc.PtoStep;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CMG21J UPSI server.
 *
 * @author Liqiang Peng
 * @date 2024/2/23
 */
public class Cmg21jUpsiServer<T> extends AbstractUpsiServer<T> {

    /**
     * MP-OPRF sender
     */
    private final MpOprfSender mpOprfSender;
    /**
     * UPSI params
     */
    private Cmg21jUpsiParams upsiParams;
    /**
     * relinearization keys
     */
    private RelinKeys relinKeys;
    /**
     * SEAL Context
     */
    private SealContext context;
    /**
     * batch encoder
     */
    private BatchEncoder encoder;
    /**
     * evaluator
     */
    private Evaluator evaluator;
    /**
     * zp64
     */
    private Zp64Poly zp64Poly;

    public Cmg21jUpsiServer(Rpc serverRpc, Party clientParty, Cmg21jUpsiConfig config) {
        super(Cmg21jUpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfSender);
    }

    @Override
    public void init(UpsiParams upsiParams) throws MpcAbortException {
        setInitInput(upsiParams.maxClientElementSize());
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        assert (upsiParams instanceof Cmg21jUpsiParams);
        this.upsiParams = (Cmg21jUpsiParams) upsiParams;
        mpOprfSender.init(this.upsiParams.maxClientElementSize());
        zp64Poly = Zp64PolyFactory.createInstance(envType, this.upsiParams.getPlainModulus());
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
        upsiParams = Cmg21jUpsiParams.SERVER_1M_CLIENT_MAX_5535;
        mpOprfSender.init(upsiParams.maxClientElementSize());
        zp64Poly = Zp64PolyFactory.createInstance(envType, this.upsiParams.getPlainModulus());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException, IOException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // MP-OPRF
        List<ByteBuffer> prfOutputList = oprf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, oprfTime, "OPRF");

        // receive hask keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            hashKeyPayload.size() == upsiParams.getCuckooHashNum(),
            "the size of hash keys " + "should be {}", upsiParams.getCuckooHashNum()
        );
        byte[][] hashKeys = hashKeyPayload.toArray(new byte[0][]);
        // receive encryption parameter and relinearization keys
        DataPacketHeader encryptionParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> encryptionParamsPayload = rpc.receive(encryptionParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            encryptionParamsPayload.size() == 2, "the size of encryption parameters should be 2"
        );
        EncryptionParameters encryptionParams = new EncryptionParameters(SchemeType.BFV);
        encryptionParams.load(null, encryptionParamsPayload.get(0));
        context = new SealContext(encryptionParams);
        relinKeys = new RelinKeys();
        relinKeys.load(context, encryptionParamsPayload.get(1));
        encoder = new BatchEncoder(context);
        evaluator = new Evaluator(context);
        // receive client query
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload =rpc.receive(queryHeader).getPayload();

        stopWatch.start();
        List<List<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(prfOutputList, hashKeys);
        int binSize = hashBins.get(0).size();
        List<long[][]> encodeDatabase = UpsoUtils.encodeDatabase(
            zp64Poly, hashBins, binSize, upsiParams.getPlainModulus(), upsiParams.getMaxPartitionSizePerBin(),
            upsiParams.getItemEncodedSlotSize(), upsiParams.getItemPerCiphertext(), upsiParams.getBinNum(),
            upsiParams.getCiphertextNum(), upsiParams.getPolyModulusDegree(), parallel
        );
        hashBins.clear();
        stopWatch.stop();
        long encodedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, encodedTime, "Server encodes database");

        stopWatch.start();
        List<Ciphertext[]> responsePayload = computeResponse(encodeDatabase, queryPayload, binSize);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> response = new ArrayList<>();
        for (Ciphertext[] ciphertexts : responsePayload) {
            for (Ciphertext ciphertext : ciphertexts) {
                response.add(ciphertext.save());
            }
        }
        rpc.send(DataPacket.fromByteArrayList(responseHeader, response));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate complete hash bin.
     *
     * @param itemList element list.
     * @param hashKeys hash keys.
     * @return complete hash bin.
     */
    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(List<ByteBuffer> itemList, byte[][] hashKeys) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(
            envType, upsiParams.getBinNum(), serverElementSize, hashKeys
        );
        completeHash.insertItems(itemList);
        int maxBinSize = IntStream.range(0, upsiParams.getBinNum()).map(completeHash::binSize).max().orElse(0);
        List<List<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        for (int i = 0; i < completeHash.binNum(); i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        itemList.clear();
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
     * @param queryPayload        query list.
     * @param binSize          bin size.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     * @throws IOException if I/O operations failed.
     */
    private List<Ciphertext[]> computeResponse(List<long[][]> database, List<byte[]> queryPayload, int binSize)
        throws MpcAbortException, IOException {
        int partitionCount = CommonUtils.getUnitNum(binSize, upsiParams.getMaxPartitionSizePerBin());
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == upsiParams.getCiphertextNum() * upsiParams.getQueryPowers().length,
            "The size of query is incorrect"
        );
        List<Ciphertext> query = new ArrayList<>();
        for (byte[] bytes : queryPayload) {
            Ciphertext temp = new Ciphertext();
            temp.load(context, bytes);
            query.add(temp);
        }
        queryPayload.clear();
        int[][] powerDegree = UpsoUtils.computePowerDegree(
            upsiParams.getPsLowDegree(), upsiParams.getQueryPowers(), upsiParams.getMaxPartitionSizePerBin()
        );
        int length = upsiParams.getQueryPowers().length;
        IntStream intStream = IntStream.range(0, upsiParams.getCiphertextNum());
        intStream = parallel ? intStream.parallel() : intStream;
        List<Ciphertext[]> queryPowers = intStream
            .mapToObj(i -> {
                    try {
                        return computeQueryPowers(query.subList(i * length, (i + 1) * length), powerDegree);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return new Ciphertext[0];
                }
            )
            .collect(Collectors.toList());
        if (upsiParams.getPsLowDegree() > 0) {
            return IntStream.range(0, upsiParams.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> optComputeMatches(queryPowers.get(i), database.get(i * partitionCount + j)))
                        .toArray(Ciphertext[]::new))
                .collect(Collectors.toList());
        } else if (upsiParams.getPsLowDegree() == 0) {
            return IntStream.range(0, upsiParams.getCiphertextNum())
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> naiveComputeMatches(queryPowers.get(i), database.get(i * partitionCount + j)))
                        .toArray(Ciphertext[]::new))
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree is incorrect");
        }
    }

    /**
     * compute query powers.
     *
     * @param query        client query.
     * @param parentPowers parent powers.
     * @return query powers.
     * @throws IOException if I/O operations failed.
     */
    private Ciphertext[] computeQueryPowers(List<Ciphertext> query, int[][] parentPowers) throws IOException {
        // compute all the powers of the receiver's input.
        int targetPowerSize = parentPowers.length;
        ParmsId highPowersParmsId = UpsoUtils.getParmsIdForChainIdx(context, 1);
        ParmsId lowPowersParmsId = UpsoUtils.getParmsIdForChainIdx(context, 2);
        Ciphertext[] encryptedPowers = IntStream.range(0, targetPowerSize)
            .mapToObj(i -> new Ciphertext(context))
            .toArray(Ciphertext[]::new);
        if (upsiParams.getPsLowDegree() > 0) {
            // Paterson-Stockmeyer algorithm
            int psHighDegree = upsiParams.getPsLowDegree() + 1;
            for (int i = 0; i < query.size(); i++) {
                if (upsiParams.getQueryPowers()[i] <= upsiParams.getPsLowDegree()) {
                    encryptedPowers[upsiParams.getQueryPowers()[i] - 1].copyFrom(query.get(i));
                } else {
                    encryptedPowers[upsiParams.getPsLowDegree() + (upsiParams.getQueryPowers()[i] / psHighDegree) - 1].copyFrom(query.get(i));
                }
            }
            for (int i = 0; i < upsiParams.getPsLowDegree(); i++) {
                if (parentPowers[i][1] != 0) {
                    if (parentPowers[i][0] - 1 == parentPowers[i][1] - 1) {
                        evaluator.square(encryptedPowers[parentPowers[i][0] - 1], encryptedPowers[i]);
                    } else {
                        evaluator.multiply(encryptedPowers[parentPowers[i][0] - 1],
                            encryptedPowers[parentPowers[i][1] - 1], encryptedPowers[i]);
                    }
                    evaluator.relinearizeInplace(encryptedPowers[i], relinKeys);
                }
            }
            for (int i = upsiParams.getPsLowDegree(); i < targetPowerSize; i++) {
                if (parentPowers[i][1] != 0) {
                    if (parentPowers[i][0] - 1 == parentPowers[i][1] - 1) {
                        evaluator.square(
                            encryptedPowers[parentPowers[i][0] - 1 + upsiParams.getPsLowDegree()], encryptedPowers[i]
                        );
                    } else {
                        evaluator.multiply(encryptedPowers[parentPowers[i][0] - 1 + upsiParams.getPsLowDegree()],
                            encryptedPowers[parentPowers[i][1] - 1 + upsiParams.getPsLowDegree()], encryptedPowers[i]);
                    }
                    evaluator.relinearizeInplace(encryptedPowers[i], relinKeys);
                }
            }
            for (int i = 0; i < upsiParams.getPsLowDegree(); i++) {
                // Low powers must be at a higher level than high powers
                evaluator.modSwitchToInplace(encryptedPowers[i], lowPowersParmsId);
                // Low powers must be in NTT form
                evaluator.transformToNttInplace(encryptedPowers[i]);
            }
            for (int i = upsiParams.getPsLowDegree(); i < targetPowerSize; i++) {
                // High powers are only modulus switched
                evaluator.modSwitchToInplace(encryptedPowers[i], highPowersParmsId);
            }
        } else {
            // naive algorithm
            for (int i = 0; i < query.size(); i++) {
                encryptedPowers[upsiParams.getQueryPowers()[i] - 1].copyFrom(query.get(i));
            }
            for (int i = 0; i < targetPowerSize; i++) {
                if (parentPowers[i][1] != 0) {
                    if (parentPowers[i][0] - 1 == parentPowers[i][1] - 1) {
                        evaluator.square(encryptedPowers[parentPowers[i][0] - 1], encryptedPowers[i]);
                    } else {
                        evaluator.multiply(encryptedPowers[parentPowers[i][0] - 1],
                            encryptedPowers[parentPowers[i][1] - 1], encryptedPowers[i]);
                    }
                    evaluator.relinearizeInplace(encryptedPowers[i], relinKeys);
                }
            }
            for (Ciphertext encryptedPower : encryptedPowers) {
                // Only one ciphertext-plaintext multiplication is needed after this
                evaluator.modSwitchToInplace(encryptedPower, highPowersParmsId);
                // All powers must be in NTT form
                evaluator.transformToNttInplace(encryptedPower);
            }
        }
        return encryptedPowers;
    }

    /**
     * optimal compute matches.
     *
     * @param powers powers in form of ciphertext.
     * @param coeffs coeffs in form of plaintext.
     * @return result.
     */
    private Ciphertext optComputeMatches(Ciphertext[] powers, long[][] coeffs) {
        ParmsId lowPowersParmsId = UpsoUtils.getParmsIdForChainIdx(context, 2);
        int psHighDegree = upsiParams.getPsLowDegree() + 1;
        Plaintext[] plaintexts = new Plaintext[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) {
            plaintexts[i] = new Plaintext();
            encoder.encode(coeffs[i], plaintexts[i]);
            if (i % psHighDegree != 0) {
                evaluator.transformToNttInplace(plaintexts[i], lowPowersParmsId);
            }
        }
        ParmsId parmsId = UpsoUtils.getParmsIdForChainIdx(context, 1);
        int degree = coeffs.length - 1;
        Ciphertext evaluated = new Ciphertext(), cipherTemp = new Ciphertext(), tempIn = new Ciphertext();
        evaluated.resize(context, parmsId, 3);
        evaluated.setIsNttForm(false);
        int psHighDegreePowers = degree / psHighDegree;
        // Calculate polynomial for i=1,...,ps_high_degree_powers-1
        for (int i = 1; i < psHighDegreePowers; i++) {
            // Evaluate inner polynomial. The free term is left out and added later on.
            // The evaluation result is stored in temp_in.
            for (int j = 1; j < psHighDegree; j++) {
                evaluator.multiplyPlain(powers[j - 1], plaintexts[j + i * psHighDegree], cipherTemp);
                if (j == 1) {
                    tempIn.copyFrom(cipherTemp);
                } else {
                    evaluator.addInplace(tempIn, cipherTemp);
                }
            }
            // Transform inner polynomial to coefficient form
            evaluator.transformFromNttInplace(tempIn);
            evaluator.modSwitchToInplace(tempIn, parmsId);
            // The high powers are already in coefficient form
            evaluator.multiplyInplace(tempIn, powers[i - 1 + upsiParams.getPsLowDegree()]);
            evaluator.addInplace(evaluated, tempIn);
        }
        // Calculate polynomial for i=ps_high_degree_powers.
        // Done separately because here the degree of the inner poly is degree % ps_high_degree.
        // Once again, the free term will only be added later on.
        if (degree % psHighDegree > 0 && psHighDegreePowers > 0) {
            for (int i = 1; i <= degree % psHighDegree; i++) {
                evaluator.multiplyPlain(powers[i - 1], plaintexts[psHighDegree * psHighDegreePowers + i], cipherTemp);
                if (i == 1) {
                    tempIn.copyFrom(cipherTemp);
                } else {
                    evaluator.addInplace(tempIn, cipherTemp);
                }
            }
            // Transform inner polynomial to coefficient form
            evaluator.transformFromNttInplace(tempIn);
            evaluator.modSwitchToInplace(tempIn, parmsId);
            // The high powers are already in coefficient form
            evaluator.multiplyInplace(tempIn, powers[psHighDegreePowers - 1 + upsiParams.getPsLowDegree()]);
            evaluator.addInplace(evaluated, tempIn);
        }
        // Relinearize sum of ciphertext-ciphertext products
        if (!evaluated.isTransparent()) {
            evaluator.relinearizeInplace(evaluated, relinKeys);
        }
        // Calculate inner polynomial for i=0.
        // Done separately since there is no multiplication with a power of high-degree
        int length = psHighDegreePowers == 0 ? degree : upsiParams.getPsLowDegree();
        for (int j = 1; j <= length; j++) {
            evaluator.multiplyPlain(powers[j - 1], plaintexts[j], cipherTemp);
            evaluator.transformFromNttInplace(cipherTemp);
            evaluator.modSwitchToInplace(cipherTemp, parmsId);
            evaluator.addInplace(evaluated, cipherTemp);
        }
        // Add the constant coefficients of the inner polynomials multiplied by the respective powers of high-degree
        for (int i = 1; i < psHighDegreePowers + 1; i++) {
            evaluator.multiplyPlain(powers[i - 1 + upsiParams.getPsLowDegree()], plaintexts[psHighDegree * i], cipherTemp);
            evaluator.modSwitchToInplace(cipherTemp, parmsId);
            evaluator.addInplace(evaluated, cipherTemp);
        }
        // Add the constant coefficient
        evaluator.addPlainInplace(evaluated, plaintexts[0]);
        while (!evaluated.parmsId().equals(context.lastParmsId())) {
            evaluator.modSwitchToNextInplace(evaluated);
        }
        return evaluated;
    }

    /**
     * naive compute matches.
     *
     * @param powers powers in form of ciphertext.
     * @param coeffs coeffs in form of plaintext.
     * @return result.
     */
    private Ciphertext naiveComputeMatches(Ciphertext[] powers, long[][] coeffs) {
        // encrypted query powers
        ParmsId parmsId = UpsoUtils.getParmsIdForChainIdx(context, 1);
        Plaintext[] plaintexts = new Plaintext[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) {
            plaintexts[i] = new Plaintext();
            encoder.encode(coeffs[i], plaintexts[i]);
            if (i > 0) {
                evaluator.transformToNttInplace(plaintexts[i], parmsId);
            }
        }
        int degree = coeffs.length - 1;
        Ciphertext evaluated = new Ciphertext(), cipherTemp = new Ciphertext(), tempIn = new Ciphertext();
        evaluated.resize(context, parmsId, 3);
        evaluated.setIsNttForm(false);
        for (int i = 1; i <= degree; i++) {
            evaluator.multiplyPlain(powers[i - 1], plaintexts[i], cipherTemp);
            if (i == 1) {
                tempIn.copyFrom(cipherTemp);
            } else {
                evaluator.addInplace(tempIn, cipherTemp);
            }
        }
        // Add the constant coefficient
        evaluator.transformFromNtt(tempIn, evaluated);
        evaluator.addPlainInplace(evaluated, plaintexts[0]);
        while (!evaluated.parmsId().equals(context.lastParmsId())) {
            evaluator.modSwitchToNextInplace(evaluated);
        }
        return evaluated;
    }
}

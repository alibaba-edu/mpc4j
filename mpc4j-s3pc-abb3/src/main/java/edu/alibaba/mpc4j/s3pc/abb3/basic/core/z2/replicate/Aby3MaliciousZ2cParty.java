package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.FileUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The party of Replicated z2 sharing with malicious security under honest-majority
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Aby3MaliciousZ2cParty extends AbstractAby3Z2cParty implements TripletZ2cParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3MaliciousZ2cParty.class);
    /**
     * the directory to buffer the unverified and tuples
     */
    private final String bufferPath;
    /**
     * the maximum number of bytes in each buffer vectors
     */
    protected final int bufferMaxByteLen;
    /**
     * the maximum number of memoryBuffer vectors
     */
    protected final int memoryBufferThreshold;
    /**
     * the maximum number of vectors can be verified at once
     */
    protected final int singleVerifyThreshold;
    /**
     * index for file buffer, the first one is the start index / the second one is the end index of unverified multiplication result in files
     */
    protected final int[] fileBufferIndexes;
    /**
     * storing unverified multiplication result
     */
    protected List<TripletRpZ2Vector[]> memoryBuffer;
    /**
     * valid number of bytes in the last memoryBuffer
     */
    protected int validByteNumOfLastBuffer;

    protected Aby3MaliciousZ2cParty(Rpc rpc, Aby3Z2cConfig config, TripletProvider tripletProvider) {
        super(rpc, config, tripletProvider);
        bufferPath = config.getBufferPath();
        File dir = new File(bufferPath);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new IllegalStateException("Dir " + dir.getName() + " doesn't exists and cannot create.");
            }
        }
        bufferMaxByteLen = config.getBufferMaxByteLen();
        memoryBufferThreshold = config.getMemoryBufferThreshold();
        singleVerifyThreshold = config.getSingleVerifyThreshold();
        fileBufferIndexes = new int[]{0, 0};
        memoryBuffer = new LinkedList<>();
    }

    @Override
    public TripletRpZ2Vector[] shareOwn(BitVector[] xiArray) throws MpcAbortException {
        // 1. generate a shared random vector, and reveal it to data owner
        int[] bitNums = Arrays.stream(xiArray).mapToInt(BitVector::bitNum).toArray();
        TripletRpZ2Vector[] rand = crProvider.randRpShareZ2Vector(bitNums);
        duringVerificationFlag = true;
        BitVector[] r = revealOwn(rand);
        duringVerificationFlag = false;
        // 2. compute w = v - r, and send it to two parties
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        BitVector[] w = intStream.mapToObj(i -> xiArray[i].xor(r[i])).toArray(BitVector[]::new);
        sendBitVectors(PtoStep.INPUT_SHARE.ordinal(), leftParty(), w);
        sendBitVectors(PtoStep.INPUT_SHARE.ordinal(), rightParty(), w);
        extraInfo++;
        // 3. send the received data to the other party, and compare
        compareView(w);
        // 4. get the result sharing  v = w + r
        IntStream.range(0, xiArray.length).forEach(i -> xori(rand[i], PlainZ2Vector.create(w[i])));
        return rand;
    }

    @Override
    public TripletRpZ2Vector[] shareOther(int[] bitNums, Party party) throws MpcAbortException {
        // 1. generate a shared random vector, and reveal it to data owner
        TripletRpZ2Vector[] rShare = crProvider.randRpShareZ2Vector(bitNums);
        duringVerificationFlag = true;
        revealOther(rShare, party);
        duringVerificationFlag = false;
        // 2. compute w = v - r, and send it to two parties
        BitVector[] w = receiveBitVectors(PtoStep.INPUT_SHARE.ordinal(), party, bitNums);
        extraInfo++;
        // 3. send the received data to the other party, and compare
        compareView(w);
        // 4. get the result sharing
        IntStream.range(0, bitNums.length).forEach(i -> xori(rShare[i], create(true, new BitVector[]{w[i]})));
        return rShare;
    }

    @Override
    public void revealOther(MpcZ2Vector[] xiArray, Party party) throws MpcAbortException {
        checkUnverified();
        int index = (selfId + 1) % 3 == party.getPartyId() ? 0 : 1;
        BitVector[] data = Arrays.stream(xiArray).map(x -> x.getBitVectors()[index]).toArray(BitVector[]::new);
        sendBitVectors(PtoStep.REVEAL_SHARE.ordinal(), party, data);
        extraInfo++;
    }

    @Override
    public BitVector[] revealOwn(MpcZ2Vector[] xiArray) throws MpcAbortException {
        checkUnverified();
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        BitVector[] data = receiveBitVectors(PtoStep.REVEAL_SHARE.ordinal(), leftParty(), bitNums);
        BitVector[] data2 = receiveBitVectors(PtoStep.REVEAL_SHARE.ordinal(), rightParty(), bitNums);
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            Preconditions.checkArgument(data[i].equals(data2[i]));
            data[i].xori(xiArray[i].getBitVectors()[0]);
            data[i].xori(xiArray[i].getBitVectors()[1]);
        });
        extraInfo++;
        return data;
    }

    @Override
    public BitVector[] open(TripletZ2Vector[] xiArray) throws MpcAbortException {
        checkUnverified();
        BitVector[] sendRightData = Arrays.stream(xiArray).map(x -> x.getBitVectors()[0]).toArray(BitVector[]::new);
        sendBitVectors(PtoStep.OPEN_SHARE.ordinal(), rightParty(), sendRightData);

        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        BitVector[] data = receiveBitVectors(PtoStep.OPEN_SHARE.ordinal(), leftParty(), bitNums);
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            data[i].xori(xiArray[i].getBitVectors()[0]);
            data[i].xori(xiArray[i].getBitVectors()[1]);
        });
        extraInfo++;
        compareView(data);
        return data;
    }

    @Override
    public void verifyMul() throws MpcAbortException {
        if (memoryBuffer.isEmpty() && fileBufferIndexes[0] == fileBufferIndexes[1]) {
            return;
        }
        duringVerificationFlag = true;
        logPhaseInfo(PtoState.PTO_BEGIN);

        // if the last one is not full
        if (bufferMaxByteLen > validByteNumOfLastBuffer) {
            if(validByteNumOfLastBuffer == 0){
                memoryBuffer.remove(memoryBuffer.size() - 1);
            }else{
                TripletRpZ2Vector[] tmp = memoryBuffer.get(memoryBuffer.size() - 1);
                for (int i = 0; i < 3; i++) {
                    tmp[i].reduce(validByteNumOfLastBuffer << 3);
                }
            }
        }
        int totalNum = fileBufferIndexes[1] - fileBufferIndexes[0] + memoryBuffer.size();
        int groupNum = totalNum / singleVerifyThreshold + (totalNum % singleVerifyThreshold > 0 ? 1 : 0);
        LOGGER.info("BC to be verified multiplication gate totalNum:{} X {} bytes", totalNum, bufferMaxByteLen);
        // verify all in batch
        int currentBatchIndex = 1;
        while (totalNum > 0) {
            StopWatch stopWatch1 = new StopWatch();
            stopWatch1.start();
            int currentLen = Math.min(singleVerifyThreshold, totalNum);
            // verify data in buffer first
            int fromBuffer = Math.min(singleVerifyThreshold, memoryBuffer.size());
            int fromFile = Math.min(fileBufferIndexes[1] - fileBufferIndexes[0], currentLen - fromBuffer);
            TripletRpZ2Vector[][] toVer = new TripletRpZ2Vector[currentLen][];
            if (fromFile > 0) {
                System.arraycopy(readBufferFile(fromFile), 0, toVer, 0, fromFile);
            }
            for (int i = 0; i < fromBuffer; i++) {
                toVer[fromFile++] = memoryBuffer.remove(0);
            }
            TripletRpZ2Vector[][] trans = MatrixUtils.transposeDim(toVer);
            verifyMultipleGroup(MatrixUtils.transposeDim(toVer),
                z2MtProvider.getTuple(Arrays.stream(trans[0]).mapToInt(MpcZ2Vector::bitNum).toArray()));
            totalNum -= currentLen;
            stopWatch1.stop();
            logStepInfo(PtoState.PTO_STEP, "BC verifyMul", currentBatchIndex++, groupNum, stopWatch1.getTime(TimeUnit.MILLISECONDS), "verify with tuples");
            stopWatch1.reset();
        }
        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    protected void intoBuffer(TripletRpZ2Vector[][] unverifiedData) {
        assert unverifiedData.length == 3;
        // if buffer is empty or the last one is full
        if (memoryBuffer.isEmpty() || validByteNumOfLastBuffer == bufferMaxByteLen) {
            this.addEmptyBuffer();
        }
        for(int index = 0; index < unverifiedData[0].length; index++){
            TripletRpZ2Vector[] unverifiedDatum = new TripletRpZ2Vector[3];
            for(int each = 0; each < 3; each++){
                unverifiedDatum[each] = unverifiedData[each][index];
            }
            // the following code essentially copy the unverified multiplication result into new fixed-length vectors
            int singleInputArrayLen = unverifiedDatum[0].byteNum();
            while (singleInputArrayLen > 0) {
                TripletRpZ2Vector[] currentBuffer = memoryBuffer.get(memoryBuffer.size() - 1);
                int lastGroupCapLen = bufferMaxByteLen - validByteNumOfLastBuffer;
                int copyLen = Math.min(lastGroupCapLen, singleInputArrayLen);
                int sourceCopyStartPos = singleInputArrayLen - copyLen;
                int targetCopyStartPos = lastGroupCapLen - copyLen;
                for (int j = 0; j < 3; j++) {
                    currentBuffer[j].setBytes(unverifiedDatum[j], sourceCopyStartPos, targetCopyStartPos, copyLen);
                }
                validByteNumOfLastBuffer += copyLen;
                singleInputArrayLen = sourceCopyStartPos;
                if (copyLen == lastGroupCapLen) {
                    this.addEmptyBuffer();
                }
            }
        }
        if (memoryBufferThreshold < memoryBuffer.size()) {
            int saveNum = memoryBuffer.size() - memoryBufferThreshold;
            TripletRpZ2Vector[][] saveData = IntStream.range(0, saveNum).mapToObj(i ->
                memoryBuffer.remove(0)).toArray(TripletRpZ2Vector[][]::new);
            writeIntoFile(saveData);
        }
    }

    @Override
    public void compareView4Zero(TripletZ2Vector... data) throws MpcAbortException {
        // 1. generate hash for x1^x2, and send it to right party
        Stream<TripletZ2Vector> stream = parallel ? Arrays.stream(data).parallel() : Arrays.stream(data);
        BitVector[] xorRes = stream.map(x -> x.getBitVectors()[0].xor(x.getBitVectors()[1])).toArray(BitVector[]::new);
        byte[] xorHash = crProvider.genHash(xorRes);
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), Collections.singletonList(xorHash));
        // 2. generate hash for x2, and compare it with the data from the left party
        byte[] x2Hash = crProvider.genHash(Arrays.stream(data).map(x -> x.getBitVectors()[1]).toArray(BitVector[]::new));
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(x2Hash, recData)) {
            throw new MpcAbortException("data is not consistent");
        }
    }

    /**
     * verify the data is the same by sending data to the right party
     *
     * @param data to be checked
     * @throws MpcAbortException if the protocol is abort.
     */
    private void compareView(BitVector[] data) throws MpcAbortException {
        List<byte[]> hash = Collections.singletonList(crProvider.genHash(data));
        send(PtoStep.COMPARE_VIEW.ordinal(), rightParty(), hash);
        byte[] recData = receive(PtoStep.COMPARE_VIEW.ordinal(), leftParty()).get(0);
        if (!Arrays.equals(hash.get(0), recData)) {
            throw new MpcAbortException("data is not consistent");
        }
    }

    /**
     * verify the multiplication result is correct with tuples
     *
     * @param toBeVerified data to be verified in the form of [[x1, x2, ...], [y1, y2, ...], [z1, z2, ...]]
     * @param tuple        multiplication tuples in the form of [[a1, a2, ...], [b1, b2, ...], [c1, c2, ...]]
     * @throws MpcAbortException if the protocol is abort.
     */
    private void verifyMultipleGroup(TripletRpZ2Vector[][] toBeVerified, TripletRpZ2Vector[][] tuple) throws MpcAbortException {
        if (toBeVerified.length == 0) {
            return;
        }
        int arrayLen = toBeVerified[0].length;
        logPhaseInfo(PtoState.PTO_BEGIN);
        // 1. compute and open rho, sigma
        stopWatch.start();
        TripletRpZ2Vector[] rho = xor(toBeVerified[0], tuple[0]);
        TripletRpZ2Vector[] sigma = xor(toBeVerified[1], tuple[1]);
        BitVector[] openRes = open(MatrixUtils.flat(new TripletRpZ2Vector[][]{rho, sigma}));
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 1, 3, resetAndGetTime(), "compute and open rho, sigma");
        // 2. [delta] = [z] ^ [c] ^ sigma & [a] ^ rho & [b] ^ rho & sigma
        stopWatch.start();
        PlainZ2Vector[] openRho = Arrays.stream(Arrays.copyOf(openRes, arrayLen))
            .map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        PlainZ2Vector[] openSigma = Arrays.stream(Arrays.copyOfRange(openRes, arrayLen, openRes.length))
            .map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        andi(tuple[1], openRho);
        andi(tuple[0], openSigma);
        xori(tuple[2], toBeVerified[2]);
        xori(tuple[2], tuple[0]);
        xori(tuple[2], tuple[1]);
        IntStream intStream = parallel ? IntStream.range(0, arrayLen).parallel() : IntStream.range(0, arrayLen);
        intStream.forEach(i -> openRho[i].andi(openSigma[i]));
        xori(tuple[2], openRho);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 2, 3, resetAndGetTime(), "locally computation");
        // 3. verify [delta] = 0
        stopWatch.start();
        compareView4Zero(tuple[2]);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 3, 3, resetAndGetTime(), "compare view for zero");
        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * add an empty memory buffer, and reset the validByteNumOfLastBuffer
     */
    private void addEmptyBuffer() {
        memoryBuffer.add(IntStream.range(0, 3).mapToObj(i ->
                TripletRpZ2Vector.createEmpty(bufferMaxByteLen << 3))
            .toArray(TripletRpZ2Vector[]::new));
        validByteNumOfLastBuffer = 0;
    }

    /**
     * write the memory buffer into files
     *
     * @param data to be stored in files
     */
    private void writeIntoFile(TripletRpZ2Vector[][] data) {
        IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
        intStream.forEach(each -> {
            int index = each + fileBufferIndexes[1];
            String filePath = bufferPath + File.separator + index + "_" + selfId + "_z2.txt";
            BitVector[] writeData = new BitVector[data[each].length << 1];
            for (int i = 0; i < data[each].length; i++) {
                System.arraycopy(data[each][i].getBitVectors(), 0, writeData, i << 1, 2);
            }
            FileUtils.writeFile(writeData, filePath);
        });
        fileBufferIndexes[1] += data.length;
    }

    /**
     * read the files into the memory buffer into bitVectors
     *
     * @param batchNum how many files should be read and deleted
     */
    private TripletRpZ2Vector[][] readBufferFile(int batchNum) {
        IntStream intStream = parallel ? IntStream.range(0, batchNum).parallel() : IntStream.range(0, batchNum);
        TripletRpZ2Vector[][] res = intStream.mapToObj(i -> {
            int index = i + fileBufferIndexes[0];
            BitVector[] tmp = FileUtils.readFileIntoBitVectors(bufferPath + File.separator + index + "_" + selfId +"_z2.txt", true);
            assert (tmp.length & 1) == 0;
            return IntStream.range(0, tmp.length >> 1).mapToObj(each ->
                    TripletRpZ2Vector.create(Arrays.copyOfRange(tmp, each << 1, (each << 1) + 2)))
                .toArray(TripletRpZ2Vector[]::new);
        }).toArray(TripletRpZ2Vector[][]::new);
        fileBufferIndexes[0] += batchNum;
        return res;
    }
}

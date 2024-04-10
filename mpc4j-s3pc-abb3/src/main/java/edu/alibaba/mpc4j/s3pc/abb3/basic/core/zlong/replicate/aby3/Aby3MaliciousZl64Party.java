package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongCpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.FileUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The party of Replicated zl64 sharing with malicious security under honest-majority
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Aby3MaliciousZl64Party extends AbstractAby3LongParty implements TripletLongParty {
    /**
     * the directory to buffer the unverified and tuples
     */
    private final String bufferPath;
    /**
     * the maximum number of data in each buffer vectors
     */
    protected final int maxBufferElementLen;
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
    protected List<TripletRpLongVector[]> memoryBuffer;
    /**
     * valid number of data in the last memoryBuffer
     */
    protected int validNumOfLastBuffer;

    public Aby3MaliciousZl64Party(Rpc rpc, Aby3LongConfig config, TripletProvider tripletProvider) {
        super(rpc, config, tripletProvider);
        bufferPath = config.getBufferPath();
        File dir = new File(bufferPath);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new IllegalStateException("Dir " + dir.getName() + " doesn't exists and cannot create.");
            }
        }
        maxBufferElementLen = config.getMaxBufferElementLen();
        memoryBufferThreshold = config.getMemoryBufferThreshold();
        singleVerifyThreshold = config.getSingleVerifyThreshold();
        fileBufferIndexes = new int[]{0, 0};
        memoryBuffer = new LinkedList<>();
    }



    @Override
    public TripletRpLongVector[] shareOwn(LongVector[] xiArray) throws MpcAbortException {
        // 1. generate a shared random vector, and reveal it to data owner
        int[] lens = Arrays.stream(xiArray).mapToInt(LongVector::getNum).toArray();
        TripletRpLongVector[] rShare = crProvider.randRpShareZl64Vector(lens);
        duringVerificationFlag = true;
        LongVector[] r = revealOwn(rShare);
        duringVerificationFlag = false;
        // 2. compute w = v - r, and send it to two parties
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        LongVector[] w = intStream.mapToObj(i -> xiArray[i].sub(r[i])).toArray(LongVector[]::new);
        sendLongVectors(PtoStep.INPUT_SHARE.ordinal(), leftParty(), w);
        sendLongVectors(PtoStep.INPUT_SHARE.ordinal(), rightParty(), w);
        extraInfo++;
        // 3. send the received data to the other party, and compare
        compareView(w);
        // 4. get the result sharing  v = w + r
        IntStream.range(0, xiArray.length).forEach(i -> addi(rShare[i], PlainLongVector.create(w[i])));
        return rShare;
    }

    @Override
    public TripletRpLongVector[] shareOther(int[] nums, Party party) throws MpcAbortException {
        // 1. generate a shared random vector, and reveal it to data owner
        TripletRpLongVector[] rShare = crProvider.randRpShareZl64Vector(nums);
        duringVerificationFlag = true;
        revealOther(party, rShare);
        duringVerificationFlag = false;
        // 2. compute w = v - r, and send it to two parties
        LongVector[] w = receiveLongVectors(PtoStep.INPUT_SHARE.ordinal(), party);
        extraInfo++;
        // 3. send the received data to the other party, and compare
        compareView(w);
        // 4. get the result sharing
        IntStream.range(0, nums.length).forEach(i -> addi(rShare[i], PlainLongVector.create(w[i])));
        return rShare;
    }

    @Override
    public LongVector[] revealOwn(int validBitLen, MpcLongVector... xiArray) throws MpcAbortException {
        checkUnverified();
        LongVector[] data = receiveLongVectors(PtoStep.REVEAL_SHARE.ordinal(), leftParty());
        LongVector[] data2 = receiveLongVectors(PtoStep.REVEAL_SHARE.ordinal(), rightParty());
        extraInfo++;
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            assert data[i].equals(data2[i]);
            data[i].addi(xiArray[i].getVectors()[0]);
            data[i].addi(xiArray[i].getVectors()[1]);
            data[i].format(validBitLen);
        });
        return data;
    }

    @Override
    public void revealOther(Party party, MpcLongVector... xiArray) throws MpcAbortException {
        checkUnverified();
        int index = (selfId + 1) % 3 == party.getPartyId() ? 0 : 1;
        LongVector[] data = Arrays.stream(xiArray).map(x -> x.getVectors()[index]).toArray(LongVector[]::new);
        sendLongVectors(PtoStep.REVEAL_SHARE.ordinal(), party, data);
        extraInfo++;
    }

    @Override
    public LongVector[] open(int validBitLen, MpcLongVector... xiArray) throws MpcAbortException {
        checkUnverified();

        LongVector[] sendData = Arrays.stream(xiArray).map(x -> x.getVectors()[0]).toArray(LongVector[]::new);
        sendLongVectors(PtoStep.OPEN_SHARE.ordinal(), rightParty(), sendData);

        LongVector[] data = receiveLongVectors(PtoStep.OPEN_SHARE.ordinal(), leftParty());
        IntStream intStream = parallel ? IntStream.range(0, xiArray.length).parallel() : IntStream.range(0, xiArray.length);
        intStream.forEach(i -> {
            data[i].addi(xiArray[i].getVectors()[0]);
            data[i].addi(xiArray[i].getVectors()[1]);
            data[i].format(validBitLen);
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

        // if the last one is not full
        if (maxBufferElementLen > validNumOfLastBuffer) {
            if(validNumOfLastBuffer == 0){
                memoryBuffer.remove(memoryBuffer.size() - 1);
            }else{
                TripletRpLongVector[] tmp = memoryBuffer.get(memoryBuffer.size() - 1);
                for (int i = 0; i < 3; i++) {
                    tmp[i].reduce(validNumOfLastBuffer);
                }
            }
        }
        // verify all in batch
        int totalNum = fileBufferIndexes[1] - fileBufferIndexes[0] + memoryBuffer.size();
        while (totalNum > 0) {
            int currentLen = Math.min(singleVerifyThreshold, totalNum);
            // verify data in buffer first
            int fromBuffer = Math.min(singleVerifyThreshold, memoryBuffer.size());
            int fromFile = Math.min(fileBufferIndexes[1] - fileBufferIndexes[0], currentLen - fromBuffer);
            TripletRpLongVector[][] toVer = new TripletRpLongVector[currentLen][];
            if (fromFile > 0) {
                System.arraycopy(readBufferFile(fromFile), 0, toVer, 0, fromFile);
            }
            for (int i = 0; i < fromBuffer; i++) {
                toVer[fromFile++] = memoryBuffer.remove(0);
            }
            verifyMultipleGroup(MatrixUtils.transposeDim(toVer),
                zl64MtProvider.getTuple(Arrays.stream(toVer).mapToInt(x -> x[0].getNum()).toArray()));
            totalNum -= currentLen;
        }
    }

    @Override
    protected void intoBuffer(TripletRpLongVector[][] unverifiedData) {
        // if buffer is empty or the last one is full
        if (memoryBuffer.isEmpty() || validNumOfLastBuffer == maxBufferElementLen) {
            this.addEmptyBuffer();
        }
        for(int index = 0; index < unverifiedData[0].length; index++){
            TripletRpLongVector[] unverifiedDatum = new TripletRpLongVector[3];
            for(int each = 0; each < 3; each++){
                unverifiedDatum[each] = unverifiedData[each][index];
            }
            // the following code essentially copy the unverified multiplication result into new fixed-length vectors
            // set values of buffer from start to end, opposite to the z2
            int singleInputArrayLen = unverifiedDatum[0].getNum();
            while (singleInputArrayLen > 0) {
                TripletRpLongVector[] currentBuffer = memoryBuffer.get(memoryBuffer.size() - 1);
                int lastGroupCapLen = maxBufferElementLen - validNumOfLastBuffer;
                int copyLen = Math.min(lastGroupCapLen, singleInputArrayLen);
                int sourceCopyStartPos = singleInputArrayLen - copyLen;
                for (int j = 0; j < 3; j++) {
                    currentBuffer[j].setElements(unverifiedDatum[j], sourceCopyStartPos, validNumOfLastBuffer, copyLen);
                }
                validNumOfLastBuffer += copyLen;
                singleInputArrayLen = sourceCopyStartPos;
                if (copyLen == lastGroupCapLen) {
                    this.addEmptyBuffer();
                }
            }
        }
        if (memoryBufferThreshold < memoryBuffer.size()) {
            int saveNum = memoryBuffer.size() - memoryBufferThreshold;
            TripletRpLongVector[][] saveData = IntStream.range(0, saveNum).mapToObj(i ->
                memoryBuffer.remove(0)).toArray(TripletRpLongVector[][]::new);
            writeIntoFile(saveData);
        }
    }

    /**
     * verify the multiplication result is correct with tuples
     *
     * @param toBeVerified data to be verified in the form of [[x1, x2, ...], [y1, y2, ...], [z1, z2, ...]]
     * @param tuple        multiplication tuples in the form of [[a1, a2, ...], [b1, b2, ...], [c1, c2, ...]]
     * @throws MpcAbortException if the protocol is abort.
     */
    private void verifyMultipleGroup(TripletLongVector[][] toBeVerified, TripletLongVector[][] tuple) throws MpcAbortException {
        if (toBeVerified.length == 0) {
            return;
        }
        logPhaseInfo(PtoState.PTO_BEGIN);
        int arrayLen = toBeVerified[0].length;
        // 1. compute and open: rho = x - a, sigma = y - b; since the data won't be used again, we use in-place operation
        stopWatch.start();
        TripletLongVector[] rho = sub(toBeVerified[0], tuple[0]);
        TripletLongVector[] sigma = sub(toBeVerified[1], tuple[1]);
        LongVector[] openRes = open(MatrixUtils.flat(new TripletLongVector[][]{rho, sigma}));
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 1, 3, resetAndGetTime(), "compute and open rho, sigma");
        // 2. [delta] = [z] - [c] - sigma * [a] - rho * [b] - rho * sigma
        stopWatch.start();
        TripletLongVector[] tmpRes = sub(toBeVerified[2], tuple[2]);
        PlainLongVector[] openRho = IntStream.range(0, arrayLen).mapToObj(i ->
                PlainLongVector.create(openRes[i]))
            .toArray(PlainLongVector[]::new);
        PlainLongVector[] openSigma = IntStream.range(0, arrayLen).mapToObj(i ->
                PlainLongVector.create(openRes[i + arrayLen]))
            .toArray(PlainLongVector[]::new);
        muli(tuple[1], openRho);
        muli(tuple[0], openSigma);
        subi(tmpRes, tuple[0]);
        subi(tmpRes, tuple[1]);
        IntStream intStream = parallel ? IntStream.range(0, arrayLen).parallel() : IntStream.range(0, arrayLen);
        intStream.forEach(i -> openRho[i].getVectors()[0].muli(openSigma[i].getVectors()[0]));
        subi(tmpRes, openRho);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 1, 3, resetAndGetTime(), "locally computation");
        // 3. format the values and verify [delta] = 0
        stopWatch.start();
        compareView4Zero(64, tmpRes);
        logStepInfo(PtoState.PTO_STEP, "verifyMultipleGroup", 3, 3, resetAndGetTime(), "compare view for zero");
        logPhaseInfo(PtoState.PTO_END);
    }



    /**
     * add an empty memory buffer, and reset the validByteNumOfLastBuffer
     */
    private void addEmptyBuffer() {
        memoryBuffer.add(IntStream.range(0, 3).mapToObj(i ->
                TripletRpLongVector.createZeros(maxBufferElementLen))
            .toArray(TripletRpLongVector[]::new));
        validNumOfLastBuffer = 0;
    }

    /**
     * write the memory buffer into files
     *
     * @param data to be stored in files
     */
    private void writeIntoFile(TripletRpLongVector[][] data) {
        IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
        intStream.forEach(each -> {
            int index = each + fileBufferIndexes[1];
            String filePath = bufferPath + File.separator + index + "_" + selfId + "_zl64.txt";
            long[][] writeData = new long[data[each].length << 1][];
            for (int i = 0, j = 0; i < data[each].length; i++) {
                writeData[j++] = data[each][i].getVectors()[0].getElements();
                writeData[j++] = data[each][i].getVectors()[1].getElements();
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
    private TripletRpLongVector[][] readBufferFile(int batchNum) {
        IntStream intStream = parallel ? IntStream.range(0, batchNum).parallel() : IntStream.range(0, batchNum);
        TripletRpLongVector[][] res = intStream.mapToObj(i -> {
            int index = i + fileBufferIndexes[0];
            long[][] tmp = FileUtils.readFileIntoLongMatrix(bufferPath + File.separator + index + "_" + selfId + "_zl64.txt", true);
            assert (tmp.length & 1) == 0;
            return IntStream.range(0, tmp.length >> 1).mapToObj(each ->
                    TripletRpLongVector.create(Arrays.copyOfRange(tmp, each << 1, (each << 1) + 2)))
                .toArray(TripletRpLongVector[]::new);
        }).toArray(TripletRpLongVector[][]::new);
        fileBufferIndexes[0] += batchNum;
        return res;
    }
}

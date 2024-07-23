package edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.edit.AbstractEditDistSender;
import edu.alibaba.mpc4j.s2pc.aby.edit.EditUtils;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.ZlExtensionParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.ZlMin2Factory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.ZlMin2Party;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Edit distance sender.
 *
 * @author Li Peng
 * @date 2024/4/8
 */
public class S2pcDiagEditDistSender extends AbstractEditDistSender {
    /**
     * private equality test sender
     */
    private final PeqtParty peqtSender;
    /**
     * zl min2 sender
     */
    private final ZlMin2Party zlMin2Sender;
    /**
     * zl mux sender
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * zl circuit sender.
     */
    private final ZlcParty zlcSender;
    /**
     * z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * zl extension party.
     */
    private final ZlExtensionParty zlExtensionSender;
    /**
     * receiver string length.
     */
    private int[] receiverStrLen;
    /**
     * sender string length.
     */
    private int[] senderStrLen;
    /**
     * max receiver string length.
     */
    private int receiverMaxLen;
    /**
     * max sender string length.
     */
    private int senderMaxLen;
    /**
     * matrix
     */
    private BigInteger[][][] matrix;
    /**
     * equality vector.
     */
    private SquareZ2Vector eq;
    /**
     * char bit length.
     */
    private final int CHAR_LEN = 8;
    /**
     * max batch size.
     */
    private final int maxBatchSize;
    /**
     * need extend.
     */
    private final boolean needExtend;
    /**
     * the number of increment of zl length in a single extend step.
     */
    private final int increment;
    /**
     * need to prune unneeded cells.
     */
    private final boolean needPrune;

    public S2pcDiagEditDistSender(Z2cParty z2cSender, Party otherParty, S2pcDiagEditDistConfig config) {
        super(S2pcDiagEditDistPtoDesc.getInstance(), z2cSender.getRpc(), otherParty, config);
        this.z2cSender = z2cSender;
        addSubPto(z2cSender);
        peqtSender = PeqtFactory.createSender(z2cSender.getRpc(), otherParty, config.getPeqtConfig());
        addSubPto(peqtSender);
        zlMin2Sender = ZlMin2Factory.createSender(z2cSender, otherParty, config.getZlMin2Config());
        addSubPto(zlMin2Sender);
        zlMuxSender = ZlMuxFactory.createSender(z2cSender.getRpc(), otherParty, config.getZlMuxConfig());
        addSubPto(zlMuxSender);
        zlcSender = ZlcFactory.createSender(z2cSender.getRpc(), otherParty, config.getZlcConfig());
        addSubPto(zlcSender);
        zlExtensionSender = ZlExtensionFactory.createSender(z2cSender, otherParty, config.getZlExtensionConfig());
        addSubPto(zlExtensionSender);
        maxBatchSize = config.getMaxBatchSize();
        needExtend = config.isNeedExtend();
        increment = config.getIncrement();
        needPrune = config.isNeedPrune();
    }

    @Override
    public void init(int maxLength) throws MpcAbortException {
        int maxLogLength = maxLength == 0 ? 1 : EditUtils.getBitRequired(2 * maxLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        stopWatch.start();
        z2cSender.init(maxBatchSize);
        peqtSender.init(CHAR_LEN, maxBatchSize);
        zlcSender.init(maxLogLength + 1, maxBatchSize);
        zlMin2Sender.init(maxLogLength + 1, maxBatchSize);
        zlMuxSender.init(maxBatchSize);
        zlExtensionSender.init(maxLogLength, maxLogLength + 1, maxBatchSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public int[] editDist(String[] input) throws MpcAbortException {

        logPhaseInfo(PtoState.PTO_BEGIN);
        List<BigInteger> result = new ArrayList<>();
        stopWatch.start();
        // 1. exchange data length
        exchangeDataLength(input);
        stopWatch.stop();
        long exchangeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, exchangeTime);
        // 2. compare and compute in batch
        stopWatch.start();
        int[][] indicator = EditUtils.getSepIndexAndNum(input, receiverStrLen, maxBatchSize);
        int totalCharNum = 0;
        // zls
        Zl[] zls = needExtend ? IntStream.range(0, senderMaxLen + receiverMaxLen - 1).mapToObj(i ->
            ZlFactory.createInstance(EnvType.INLAND_JDK, EditUtils.getBitRequired(i + 1))).toArray(Zl[]::new) :
            IntStream.range(0, senderMaxLen + receiverMaxLen - 1).mapToObj(i ->
                ZlFactory.createInstance(EnvType.INLAND_JDK, EditUtils.getBitRequired(senderMaxLen + receiverMaxLen - 1))).toArray(Zl[]::new);
        // max extend count.
        int globalExtendCount = 0;
        for (int batchIndex = 0; batchIndex < indicator[0].length; batchIndex++) {
            // 1) peqt
            int strStartIndex = batchIndex == 0 ? 0 : indicator[0][batchIndex - 1];
            int strStopIndex = indicator[0][batchIndex];
            int strNum = strStopIndex - strStartIndex;
            // encode input into byte array.
            byte[][] equTestInput = EditUtils.getSingleBatchBytesSimple(
                input, receiverStrLen, indicator[1][batchIndex], strStartIndex, strStopIndex, false);
            // peqt protocol
            if (equTestInput.length == 0) {
                eq = SquareZ2Vector.createEmpty(false);
            } else {
                eq = peqtSender.peqt(CHAR_LEN, equTestInput);
            }
            // init matrix
            initMatrix(strNum, strStartIndex, needPrune);
            // 2) diagonal computing. extract and combine every elements in every iterations of diagonal computing
            int count = 0;
            for (int computeTime = 0; computeTime < senderMaxLen + receiverMaxLen - 1; computeTime++) {
                // record coordinates with arrays.
                int[][][] coordi = new int[strNum][][];
                for (int strIndex = 0; strIndex < strNum; strIndex++) {
                    coordi[strIndex] = EditUtils.getCoordi(computeTime, receiverStrLen[strStartIndex + strIndex], senderStrLen[strStartIndex + strIndex], needPrune);
                }
                // eq
                SquareZ2Vector tempEqVector = getTempEq(strStartIndex, strNum, coordi, totalCharNum);
                if (tempEqVector.bitNum() == 0) {
                    continue;
                }
                if (needExtend) {
                    // check
                    if (computeTime >= 1 && zls[computeTime - 1].getL() < zls[computeTime].getL()) {
                        //  extend zl and update
                        extendAndUpdate(zls, computeTime - 1, computeTime, increment, strNum, strStartIndex);
                        count++;
                    }
                    // check
                    if (computeTime >= 2 && zls[computeTime - 2].getL() < zls[computeTime].getL()) {
                        // extend zl and update
                        extendAndUpdate(zls, computeTime - 2, computeTime, increment, strNum, strStartIndex);
                    }
                }
                // matrix[j-1][k-1]
                SquareZlVector matrixChar1Vector = getChar1Vector(coordi, strNum, zls[computeTime]);
                // matrix[j-1][k]
                SquareZlVector matrixChar2Vector = getChar2Vector(coordi, strNum, zls[computeTime]);
                // matrix[j][k-1]
                SquareZlVector matrixChar3Vector = getChar3Vector(coordi, strNum, zls[computeTime]);
                // matrix[j][k] = eq * matrix[j-1][k-1] + !eq * minPlusOne
                SquareZlVector one = SquareZlVector.createOnes(zls[computeTime], tempEqVector.getNum());
                SquareZlVector t1 = zlMin2Sender.min2(matrixChar1Vector, zlMin2Sender.min2(matrixChar2Vector, matrixChar3Vector));
                SquareZlVector minPlusOne = zlcSender.add(t1, one);
                SquareZlVector matrixCharResult = zlcSender.add(zlMuxSender.mux(tempEqVector, matrixChar1Vector),
                    zlMuxSender.mux(z2cSender.not(tempEqVector), minPlusOne));
                // update matrix
                EditUtils.updateMatrix(matrix, matrixCharResult.getZlVector().getElements(), coordi);
            }
            globalExtendCount = Math.max(globalExtendCount, count);
            // add result
            for (int strIndex = 0; strIndex < strNum; strIndex++) {
                result.add(matrix[strIndex][receiverStrLen[strStartIndex + strIndex]][senderStrLen[strStartIndex + strIndex]]);
            }
            totalCharNum += indicator[1][batchIndex];
        }
        System.out.println("count:" + globalExtendCount);
        stopWatch.stop();
        long computeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, computeTime);

        stopWatch.start();
        int[] r = receiveResult(result.toArray(new BigInteger[0]));
        stopWatch.stop();
        long finalTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, finalTime);
        logPhaseInfo(PtoState.PTO_END);
        return r;
    }

    /**
     * Exchange data length.
     *
     * @param input input data.
     */
    private void exchangeDataLength(String[] input) {
        senderStrLen = Arrays.stream(input).mapToInt(String::length).toArray();
        senderMaxLen = Arrays.stream(senderStrLen).boxed().max(Integer::compare).orElseThrow(NoSuchElementException::new);
        List<byte[]> sendDataLen = Arrays.stream(senderStrLen).mapToObj(IntUtils::intToByteArray).collect(Collectors.toList());
        DataPacketHeader headerSendDataLen = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), S2pcDiagEditDistPtoDesc.PtoStep.SEND_CHAR_LEN.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(headerSendDataLen, sendDataLen));
        DataPacketHeader headerReceiveDataLen = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), S2pcDiagEditDistPtoDesc.PtoStep.SEND_CHAR_LEN.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        receiverStrLen = rpc.receive(headerReceiveDataLen).getPayload().stream().mapToInt(IntUtils::byteArrayToInt).toArray();
        receiverMaxLen = Arrays.stream(receiverStrLen).boxed().max(Integer::compare).orElseThrow(NoSuchElementException::new);
    }

    /**
     * Initiate edit distance matrix.
     *
     * @param strNum        Number of strings.
     * @param strStartIndex start index of string in current iteration
     */
    private void initMatrix(int strNum, int strStartIndex, boolean needPrune) {
        // format：BigInteger[][][]，index、row、column.
        matrix = new BigInteger[strNum][][];
        // init first row and column with plain values.
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            matrix[strIndex] = new BigInteger[receiverStrLen[strStartIndex + strIndex] + 1][senderStrLen[strStartIndex + strIndex] + 1];
            // first column
            for (int i = 0; i < receiverStrLen[strStartIndex + strIndex] + 1; i++) {
                matrix[strIndex][i][0] = BigInteger.ZERO;
            }
            // first row
            for (int j = 0; j < senderStrLen[strStartIndex + strIndex] + 1; j++) {
                matrix[strIndex][0][j] = BigInteger.ZERO;
            }
            // init pruned cells
            if (needPrune) {
                for (int i = 1; i < receiverStrLen[strStartIndex + strIndex] + 1; i++) {
                    for (int j = 1; j < senderStrLen[strStartIndex + strIndex] + 1; j++) {
                        if (EditUtils.isPrunedIndex(i, j, receiverStrLen[strStartIndex + strIndex] + 1, senderStrLen[strStartIndex + strIndex] + 1)) {
                            matrix[strIndex][i][j] = BigInteger.ZERO;
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the equality-test vector in current iteration.
     *
     * @param strStartIndex the start index of string in current iteration.
     * @param strNum        the number of strings.
     * @param coordi        arrays of coordinates.
     * @param totalCharNum  total char num in previous iterations.
     * @return the equality-test vector
     */
    private SquareZ2Vector getTempEq(int strStartIndex, int strNum, int[][][] coordi, int totalCharNum) {
        int charOffsetSum = IntStream.range(0, strStartIndex).map(i -> receiverStrLen[i] * senderStrLen[i]).sum();
        // global index of extracted char
        List<Integer> charIndexes = new ArrayList<>();
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            int coordiNum = coordi[strIndex][0] == null ? 0 : coordi[strIndex][0].length;
            if (coordiNum == 0) {
                charOffsetSum += receiverStrLen[strStartIndex + strIndex] * senderStrLen[strStartIndex + strIndex];
                continue;
            }
            for (int coordiIndex = 0; coordiIndex < coordiNum; coordiIndex++) {
                int currentCharOffSet = EditUtils.getOffsetFromCoordi(senderStrLen[strIndex + strStartIndex], coordi[strIndex][0][coordiIndex], coordi[strIndex][1][coordiIndex]);
                charIndexes.add(charOffsetSum + currentCharOffSet - totalCharNum);
            }
            charOffsetSum += receiverStrLen[strStartIndex + strIndex] * senderStrLen[strStartIndex + strIndex];
        }
        int validCharNum = charIndexes.size();
        if (validCharNum == 0) {
            return SquareZ2Vector.createEmpty(false);
        }
        BitVector tempEq = BitVectorFactory.createZeros(validCharNum);
        for (int charInnerIndex = 0; charInnerIndex < validCharNum; charInnerIndex++) {
            tempEq.set(charInnerIndex, eq.getBitVector().get(charIndexes.get(charInnerIndex)));
        }
        return SquareZ2Vector.create(tempEq, false);
    }

    /**
     * Compute the matrix[j-1][k-1].
     *
     * @param coordi arrays of coordinates.
     * @param strNum the number of strings.
     * @return matrix[j-1][k-1].
     */
    private SquareZlVector getChar1Vector(int[][][] coordi, int strNum, Zl zl) {
        // matrix[j-1][k-1]
        List<BigInteger> matrixChar1 = new ArrayList<>();
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            int coordiNum = coordi[strIndex][0] == null ? 0 : coordi[strIndex][0].length;
            if (coordiNum == 0) {
                continue;
            }
            for (int coordiIndex = 0; coordiIndex < coordiNum; coordiIndex++) {
                matrixChar1.add(matrix[strIndex][coordi[strIndex][0][coordiIndex]][coordi[strIndex][1][coordiIndex]]);
                assert matrix[strIndex][coordi[strIndex][0][coordiIndex]][coordi[strIndex][1][coordiIndex]] != null;
            }
        }
        return SquareZlVector.create(zl, matrixChar1.toArray(new BigInteger[0]), false);
    }

    /**
     * Compute the matrix[j-1][k].
     *
     * @param coordi arrays of coordinates.
     * @param strNum the number of strings.
     * @return matrix[j-1][k].
     */
    private SquareZlVector getChar2Vector(int[][][] coordi, int strNum, Zl zl) {
        // matrix[j-1][k]
        List<BigInteger> matrixChar2 = new ArrayList<>();
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            int coordiNum = coordi[strIndex][0] == null ? 0 : coordi[strIndex][0].length;
            if (coordiNum == 0) {
                continue;
            }
            for (int coordiIndex = 0; coordiIndex < coordiNum; coordiIndex++) {
                matrixChar2.add(matrix[strIndex][coordi[strIndex][0][coordiIndex]][coordi[strIndex][1][coordiIndex] + 1]);
                assert matrix[strIndex][coordi[strIndex][0][coordiIndex]][coordi[strIndex][1][coordiIndex]] != null;
            }
        }
        return SquareZlVector.create(zl, matrixChar2.toArray(new BigInteger[0]), false);
    }

    /**
     * Compute the matrix[j][k-1].
     *
     * @param coordi arrays of coordinates.
     * @param strNum the number of strings.
     * @return matrix[j][k-1].
     */
    private SquareZlVector getChar3Vector(int[][][] coordi, int strNum, Zl zl) {
        // matrix[j][k-1]
        List<BigInteger> matrixChar3 = new ArrayList<>();
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            int coordiNum = coordi[strIndex][0] == null ? 0 : coordi[strIndex][0].length;
            if (coordiNum == 0) {
                continue;
            }
            for (int coordiIndex = 0; coordiIndex < coordiNum; coordiIndex++) {
                matrixChar3.add(matrix[strIndex][coordi[strIndex][0][coordiIndex] + 1][coordi[strIndex][1][coordiIndex]]);
                assert matrix[strIndex][coordi[strIndex][0][coordiIndex]][coordi[strIndex][1][coordiIndex]] != null;
            }
        }
        return SquareZlVector.create(zl, matrixChar3.toArray(new BigInteger[0]), false);
    }

    /**
     * Extend zl and update matrix.
     *
     * @param zls           zls.
     * @param oldZlIndex    the index of old zl.
     * @param newZlIndex    the index of new zl.
     * @param increment     the number of increment of zl length in a single extend step.
     * @param strNum        number of strings.
     * @param strStartIndex start index of string.
     */
    private void extendAndUpdate(Zl[] zls, int oldZlIndex, int newZlIndex, int increment, int strNum, int strStartIndex) throws MpcAbortException {
        // get coordinates
        int[][][] coordi = new int[strNum][][];
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            coordi[strIndex] = EditUtils.getCoordi(oldZlIndex, receiverStrLen[strStartIndex + strIndex], senderStrLen[strStartIndex + strIndex], needPrune);
        }
        // extend matrix[j][k]
        List<BigInteger> matrixCharAll = new ArrayList<>();
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            int coordiNum = coordi[strIndex][0] == null ? 0 : coordi[strIndex][0].length;
            if (coordiNum == 0) {
                continue;
            }
            for (int coordiIndex = 0; coordiIndex < coordiNum; coordiIndex++) {
                matrixCharAll.add(matrix[strIndex][coordi[strIndex][0][coordiIndex] + 1][coordi[strIndex][1][coordiIndex] + 1]);
                assert matrix[strIndex][coordi[strIndex][0][coordiIndex] + 1][coordi[strIndex][1][coordiIndex] + 1] != null;
            }
        }
        Zl oldZl = zls[oldZlIndex];
        Zl newZl = ZlFactory.createInstance(EnvType.STANDARD, oldZl.getL() + increment);
        SquareZlVector v = SquareZlVector.create(oldZl, matrixCharAll.toArray(new BigInteger[0]), false);
        v = zlExtensionSender.zExtend(v, newZl.getL(), false);
        // update
        EditUtils.updateMatrix(matrix, v.getZlVector().getElements(), coordi);
        zls[oldZlIndex] = newZl;
        Arrays.stream(IntStream.range(0, zls.length).filter(i -> zls[i].getL() == zls[newZlIndex].getL()).toArray()).forEach(i -> zls[i] = newZl);
    }

    /**
     * Exchange data length.
     *
     * @param input input data.
     */
    private int[] receiveResult(BigInteger[] input) {
        DataPacketHeader headerReceiveDataLen = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SEND_RESULT.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        long[] receiverResult = rpc.receive(headerReceiveDataLen).getPayload().stream().mapToLong(LongUtils::byteArrayToLong).toArray();
        assert receiverResult.length == input.length;
        return IntStream.range(0, input.length).map(i -> {
            // get the zl of str of index i.
            int l = EditUtils.getBitRequired(senderStrLen[i] + receiverStrLen[i]);
            int r = (int) (input[i].longValue() + receiverResult[i]) % (1 << l);
            r = r < 0 ? r + (1 << l) : r;
            return r;
        }).toArray();
    }
}

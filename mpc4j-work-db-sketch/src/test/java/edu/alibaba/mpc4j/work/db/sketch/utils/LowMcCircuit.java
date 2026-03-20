package edu.alibaba.mpc4j.work.db.sketch.utils;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.FileUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcSoprpPtoDesc.PrpSteps;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.MatrixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * lowMc circuit
 */
public class LowMcCircuit {
    private static final Logger LOGGER = LoggerFactory.getLogger(LowMcCircuit.class);
    /**
     * secure random
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * LowMC file path
     */
    private static final String LOW_MC_RESOURCE_FILE_PATH = "low_mc/";
    /**
     * LowMC parameter file suffix
     */
    private static final String LOW_MC_FILE_SUFFIX = "LowMc_Matrix.txt";
    /**
     * party
     */
    public MpcZ2cParty env;
    /**
     * RPC
     */
    public Rpc rpc;
    /**
     * lowMC num. of box
     */
    private final int numOfBoxes;
    /**
     * lowMC block size
     */
    private final int blockSize;
    /**
     * lowMC round
     */
    private final int rounds;
    /**
     * key
     */
    protected MpcZ2Vector key;
    /**
     * current data to be filled into the circuit
     */
    protected MpcZ2Vector[] state;
    /**
     * lowMC matrix
     */
    public boolean[][][] linMatrices;
    public boolean[][][] invLinMatrices;
    public boolean[][] roundConstants;
    public boolean[][][] keyMatrices;
    public MpcZ2Vector[] roundKeys;

    public LowMcCircuit(MpcZ2cParty z2cParty, Rpc rpc, LowMcParam param) {
        this.env = z2cParty;
        this.rpc = rpc;
        numOfBoxes = param.numOfBoxes;
        blockSize = param.blockSize;
        rounds = param.rounds;
    }

    public void init() throws MpcAbortException {
        env.init();
        this.key = env.createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        this.extendKey(key);
        this.initMatrix();
    }

    public void init(MpcZ2Vector key) throws MpcAbortException {
        env.init();
        this.extendKey(key);
        this.initMatrix();
    }

    public int getInputDim() {
        return blockSize;
    }

    public void setKey(MpcZ2Vector key) throws MpcAbortException {
        this.extendKey(key);
        this.keySchedule();
    }

    /**
     * enc
     *
     * @param plainText (length, num)
     */
    public MpcZ2Vector[] enc(MpcZ2Vector[] plainText) throws MpcAbortException {
        checkEncInput(plainText, blockSize);
        int numOfInput = plainText[0].getNum();
        BitVector allOneVec = BitVectorFactory.createOnes(numOfInput);
        BitVector allZeroVec = BitVectorFactory.createZeros(numOfInput);

        // initial whitening
        // state = plaintext + MultiplyWithGF2Matrix(KMatrix(0),key)
        for (int dim = 0; dim < blockSize; dim++) {
            int targetIndex = blockSize - dim - 1;
            MpcZ2Vector tmpRoundKey = env.create(plainText[0].isPlain(),
                Arrays.stream(roundKeys[0].getBitVectors()).map(each ->
                    BinaryUtils.getBoolean(each.getBytes(), targetIndex) ? allOneVec : allZeroVec).toArray(BitVector[]::new));
            env.xori(state[dim], tmpRoundKey);
        }
        for (int r = 0; r < rounds; r++) {
            // m computations of 3-bit sBox,
            // remaining n-3m bits remain the same
            this.sBox1Round(state);
            // affine layer
            // state = MultiplyWithGF2Matrix(LMatrix(i),state)
            state = this.batchMultiplyWithF2Matrix(state, this.linMatrices[r]);
            // state = state + Constants(i)
            MpcZ2Vector[] finalState = state;
            for (int i = 0; i < blockSize; i++) {
                if (this.roundConstants[r][i]) {
                    env.noti(finalState[i]);
                }
                // generate round key and add to the state
                // state = state + MultiplyWithGF2Matrix(KMatrix(i + 1),key)
                int targetIndex = blockSize - i - 1;
                MpcZ2Vector tmpRoundKey = env.create(plainText[0].isPlain(),
                    Arrays.stream(roundKeys[r + 1].getBitVectors()).map(each ->
                        BinaryUtils.getBoolean(each.getBytes(), targetIndex) ? allOneVec : allZeroVec).toArray(BitVector[]::new));
                env.xori(finalState[i], tmpRoundKey);
            }
        }
        return state;
    }

    /**
     * dec
     *
     * @param ciphertext (blockSize, num)
     * @param bitLength  the length of the required plaintext
     */
    public MpcZ2Vector[] dec(MpcZ2Vector[] ciphertext, int bitLength) throws MpcAbortException {
        checkDecInput(ciphertext, blockSize);
        int numOfInput = ciphertext[0].getNum();
        BitVector allOneVec = BitVectorFactory.createOnes(numOfInput);
        BitVector allZeroVec = BitVectorFactory.createZeros(numOfInput);

        for (int r = rounds; r > 0; r--) {
            for (int i = 0; i < blockSize; i++) {
                // generate round key and add to the state
                // state = state + MultiplyWithGF2Matrix(KMatrix(i + 1),key)
                int targetIndex = blockSize - i - 1;
                MpcZ2Vector tmpRoundKey = env.create(ciphertext[0].isPlain(),
                    Arrays.stream(roundKeys[r].getBitVectors()).map(each ->
                        BinaryUtils.getBoolean(each.getBytes(), targetIndex) ? allOneVec : allZeroVec).toArray(BitVector[]::new));
                env.xori(state[i], tmpRoundKey);
                // state = state + Constants(i)
                if (this.roundConstants[r - 1][i]) {
                    env.noti(state[i]);
                }
            }
            // state = MultiplyWithGF2Matrix(LMatrix(i),state)
            state = this.batchMultiplyWithF2Matrix(state, this.invLinMatrices[r - 1]);
            // m computations of 3-bit sBox,
            // remaining n-3m bits remain the same
            this.invBox1Round(state);
        }
        // final whitening
        // state = plaintext + MultiplyWithGF2Matrix(KMatrix(0),key)
        for (int dim = 0; dim < blockSize; dim++) {
            int targetIndex = blockSize - dim - 1;
            MpcZ2Vector tmpRoundKey = env.create(ciphertext[0].isPlain(),
                Arrays.stream(roundKeys[0].getBitVectors()).map(each ->
                    BinaryUtils.getBoolean(each.getBytes(), targetIndex) ? allOneVec : allZeroVec).toArray(BitVector[]::new));
            env.xori(state[dim], tmpRoundKey);
        }
        // 输出
        MpcZ2Vector[] output;
        if (bitLength == blockSize) {
            output = state;
        } else if (bitLength < blockSize) {
            output = new MpcZ2Vector[bitLength];
            System.arraycopy(state, blockSize - bitLength, output, 0, bitLength);
        } else {
            output = new MpcZ2Vector[bitLength];
            for (int i = 0; i < bitLength - blockSize; i++) {
                output[i] = env.createShareZeros(numOfInput);
            }
            System.arraycopy(state, 0, output, bitLength - blockSize, blockSize);
        }
        return output;
    }

    protected void checkEncInput(MpcZ2Vector[] plainText, int blockSize) throws MpcAbortException {
        int numOfInput = plainText[0].getNum();
        int originLength = plainText.length;
        state = new MpcZ2Vector[blockSize];
        if (originLength <= blockSize) {
            for (int i = 0; i < blockSize - originLength; i++) {
                state[i] = env.createShareZeros(numOfInput);
            }
            System.arraycopy(plainText, 0, state, blockSize - originLength, originLength);
        } else {
            throw new MpcAbortException("the bit length of prp input is too long: " + plainText.length + ">" + blockSize);
        }
    }

    protected void checkDecInput(MpcZ2Vector[] ciphertext, int blockSize) {
        MathPreconditions.checkEqual("ciphertext.length", "blockSize", ciphertext.length, blockSize);
        state = new MpcZ2Vector[blockSize];
        System.arraycopy(ciphertext, 0, state, 0, blockSize);
    }


    /**
     * init key
     */
    private void extendKey(MpcZ2Vector key) throws MpcAbortException {
        if (key.getNum() >= CommonConstants.BLOCK_BIT_LENGTH) {
            this.key = key;
            key.reduce(CommonConstants.BLOCK_BIT_LENGTH);
        } else {
            throw new MpcAbortException("the length of key is too short: " + key.getNum());
        }
    }

    /**
     * encode to save
     */
    public byte[][] encodeMatrix() {
        byte[][] savaArray = new byte[(3 * rounds + 1) * blockSize + rounds][];
        for (int r = 0; r < rounds + 1; r++) {
            for (int i = 0; i < blockSize; i++) {
                if (r < rounds) {
                    savaArray[r * blockSize + i] = BinaryUtils.binaryToRoundByteArray(linMatrices[r][i]);
                    savaArray[rounds * blockSize + r * blockSize + i] = BinaryUtils.binaryToRoundByteArray(invLinMatrices[r][i]);
                }
                savaArray[2 * rounds * blockSize + r * blockSize + i] = BinaryUtils.binaryToRoundByteArray(keyMatrices[r][i]);
            }
            if (r < rounds) {
                savaArray[(3 * rounds + 1) * blockSize + r] = BinaryUtils.binaryToRoundByteArray(roundConstants[r]);
            }
        }
        return savaArray;
    }

    /**
     * decode
     */
    public void decode2Matrix(byte[][] matrix) throws MpcAbortException {
        MathPreconditions.checkEqual("matrix.length", "(3 * rounds + 1) * blockSize + rounds",
            matrix.length, (3 * rounds + 1) * blockSize + rounds);
        for (int r = 0; r < rounds + 1; r++) {
            for (int j = 0; j < blockSize; j++) {
                if (r < rounds) {
                    this.linMatrices[r][j] = BinaryUtils.uncheckByteArrayToBinary(matrix[r * blockSize + j], blockSize);
                    this.invLinMatrices[r][j] = BinaryUtils.uncheckByteArrayToBinary(matrix[rounds * blockSize + r * blockSize + j], blockSize);
                }
                this.keyMatrices[r][j] = BinaryUtils.uncheckByteArrayToBinary(matrix[2 * rounds * blockSize + r * blockSize + j], CommonConstants.BLOCK_BIT_LENGTH);
            }
            if (r < rounds) {
                this.roundConstants[r] = BinaryUtils.uncheckByteArrayToBinary(matrix[(3 * rounds + 1) * blockSize + r], blockSize);
            }
        }
        this.verifyLegal();
    }

    /**
     * generate parameters
     */
    public void genMatrix() {
        this.linMatrices = new boolean[rounds][blockSize][blockSize];
        this.invLinMatrices = new boolean[rounds][blockSize][blockSize];
        this.roundConstants = new boolean[rounds][blockSize];
        this.keyMatrices = new boolean[rounds + 1][blockSize][CommonConstants.BLOCK_BIT_LENGTH];
        // linMatrices, invLinMatrices, keyMatrices
        for (int r = 0; r < rounds; r++) {
            // linMatrices and invLinMatrices
            boolean[][] mat = new boolean[blockSize][blockSize];
            boolean[][] keyMat = new boolean[blockSize][CommonConstants.BLOCK_BIT_LENGTH];
            int rank;
            do {
                for (int i = 0; i < blockSize; i++) {
                    mat[i] = this.randomBits(blockSize);
                }
                rank = MatrixUtils.rankOfMatrix(mat);
            } while (rank != blockSize);
            boolean[][] invMat = MatrixUtils.invertMatrix(mat);
            // keyMatrices
            do {
                for (int i = 0; i < blockSize; i++) {
                    keyMat[i] = this.randomBits(CommonConstants.BLOCK_BIT_LENGTH);
                }
            } while (MatrixUtils.rankOfMatrix(keyMat) < Math.min(blockSize, CommonConstants.BLOCK_BIT_LENGTH));
            for (int i = 0; i < blockSize; i++) {
                this.linMatrices[r][i] = Arrays.copyOf(mat[i], blockSize);
                this.invLinMatrices[r][i] = Arrays.copyOf(invMat[i], blockSize);
                this.keyMatrices[r][i] = Arrays.copyOf(keyMat[i], CommonConstants.BLOCK_BIT_LENGTH);
            }
            if (r == rounds - 1) {
                do {
                    for (int i = 0; i < blockSize; i++) {
                        keyMat[i] = this.randomBits(CommonConstants.BLOCK_BIT_LENGTH);
                    }
                } while (MatrixUtils.rankOfMatrix(keyMat) < Math.min(blockSize, CommonConstants.BLOCK_BIT_LENGTH));
                for (int i = 0; i < blockSize; i++) {
                    this.keyMatrices[rounds][i] = Arrays.copyOf(keyMat[i], CommonConstants.BLOCK_BIT_LENGTH);
                }
            }
            // roundConstants
            this.roundConstants[r] = this.randomBits(blockSize);
        }
    }

    /**
     * verify
     */
    public void verifyLegal() throws MpcAbortException {
        for (int r = 0; r < rounds + 1; r++) {
            if (r < rounds) {
                MathPreconditions.checkEqual("rankOfMatrix", "blockSize", MatrixUtils.rankOfMatrix(this.linMatrices[r]), blockSize);
                MatrixUtils.testInvMatrix(this.invLinMatrices[r], this.linMatrices[r]);
            }
            MathPreconditions.checkEqual("rank of keyMatrices", "Math.min(blockSize, CommonConstants.BLOCK_BIT_LENGTH)", MatrixUtils.rankOfMatrix(this.keyMatrices[r]), Math.min(blockSize, CommonConstants.BLOCK_BIT_LENGTH));
        }
    }

    /**
     * init parameters
     */
    public void initMatrix() throws MpcAbortException {
        LOGGER.info("initMatrix");
        this.linMatrices = new boolean[rounds][blockSize][blockSize];
        this.invLinMatrices = new boolean[rounds][blockSize][blockSize];
        this.roundConstants = new boolean[rounds][blockSize];
        this.keyMatrices = new boolean[rounds + 1][blockSize][CommonConstants.BLOCK_BIT_LENGTH];
        String fileDir = "./" + LOW_MC_RESOURCE_FILE_PATH;
        File dir = new File(fileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileNameWithParam = fileDir + this.numOfBoxes + "_" + blockSize + "_" + rounds + LOW_MC_FILE_SUFFIX;
        File matrixFile = new File(fileNameWithParam);
        if (matrixFile.exists()) {
            LOGGER.info("lowMc file exist");
            BitVector[] data = FileUtils.readFileIntoBitVectors(fileNameWithParam, false);
            this.decode2Matrix(Arrays.stream(data).map(BitVector::getBytes).toArray(byte[][]::new));
        } else {
            LOGGER.info("no lowMc file exist");
            if (rpc == null) {
                LOGGER.info("start generate matrix");
                this.genMatrix();
                byte[][] savaArray = encodeMatrix();
                FileUtils.writeFile(Arrays.stream(savaArray).map(each -> BitVectorFactory.create(each.length << 3, each)).toArray(BitVector[]::new), fileNameWithParam);
                LOGGER.info("server finish save matrix");
            } else {
                if (rpc.ownParty().getPartyId() == 0) {
                    LOGGER.info("server start generate matrix");
                    this.genMatrix();
                    byte[][] savaArray = encodeMatrix();
                    LOGGER.info("server start public matrix");
                    List<byte[]> sendData = Arrays.stream(savaArray).collect(Collectors.toList());
                    for (int i = 1; i < rpc.getPartySet().size(); i++) {
                        DataPacketHeader header = new DataPacketHeader(
                            0, 0, PrpSteps.INIT_MATRIX.ordinal(), 0,
                            0, i
                        );
                        rpc.send(DataPacket.fromByteArrayList(header, sendData));
                    }
                    FileUtils.writeFile(Arrays.stream(savaArray).map(each -> BitVectorFactory.create(each.length << 3, each)).toArray(BitVector[]::new), fileNameWithParam);
                    LOGGER.info("server finish public matrix");
                } else {
                    LOGGER.info("start receive matrix");
                    DataPacketHeader header = new DataPacketHeader(
                        0, 0, PrpSteps.INIT_MATRIX.ordinal(), 0,
                        0, rpc.ownParty().getPartyId()
                    );
                    byte[][] matrix = rpc.receive(header).getPayload().toArray(new byte[0][]);
                    this.decode2Matrix(matrix);
                    if (!matrixFile.exists()) {
                        FileUtils.writeFile(Arrays.stream(matrix).map(each -> BitVectorFactory.create(each.length << 3, each)).toArray(BitVector[]::new), fileNameWithParam);
                    }
                }
            }
        }
        // roundKeys
        this.keySchedule();
    }

    /**
     * generate roundsKey
     */
    private void keySchedule() {
        roundKeys = new MpcZ2Vector[rounds + 1];
        for (int r = 0; r < rounds + 1; r++) {
            roundKeys[r] = this.multiplyWithF2Matrix(this.key, this.keyMatrices[r]);
        }
    }

    /**
     * generate roundKey
     */
    private MpcZ2Vector multiplyWithF2Matrix(MpcZ2Vector wireInput, boolean[][] matrix) {
        MathPreconditions.checkEqual("wireInput.getNum()", "matrix[0].length", wireInput.getNum(), matrix[0].length);
        boolean[][] wireInputBinary = new boolean[wireInput.getBitVectors().length][];
        boolean[][] res = new boolean[wireInputBinary.length][matrix.length];
        for (int i = 0; i < wireInputBinary.length; i++) {
            wireInputBinary[i] = BinaryUtils.uncheckByteArrayToBinary(wireInput.getBitVectors()[i].getBytes(), wireInput.getNum());
        }
        for (int row = 0; row < matrix.length; row++) {
            boolean[] temp = new boolean[wireInputBinary.length];
            for (int i = 0; i < wireInputBinary.length; i++) {
                for (int col = 0; col < matrix[0].length; col++) {
                    if (matrix[row][col]) {
                        temp[i] = temp[i] ^ wireInputBinary[i][col];
                    }
                }
                res[i][row] = temp[i];
            }
        }
        BitVector[] resVec = new BitVector[wireInputBinary.length];
        for (int i = 0; i < wireInputBinary.length; i++) {
            resVec[i] = BitVectorFactory.create(matrix.length, BinaryUtils.binaryToRoundByteArray(res[i]));
        }
        return env.create(wireInput.isPlain(), resVec);
    }

    /**
     * when the dimension of input is changed, we use row to compute
     *
     * @param wireInput input is (bitLen, num)
     * @param matrix    (bitLen, bitLen)
     */
    private MpcZ2Vector[] batchMultiplyWithF2Matrix(MpcZ2Vector[] wireInput, boolean[][] matrix) {
        MathPreconditions.checkEqual("wireInput.length", "matrix[0].length", wireInput.length, matrix[0].length);
        MpcZ2Vector[] output = IntStream.range(0, matrix.length).mapToObj(i ->
            env.createShareZeros(wireInput[0].getNum())).toArray(MpcZ2Vector[]::new);
        IntStream wIntStream = env.getParallel() ? IntStream.range(0, matrix.length).parallel() : IntStream.range(0, matrix.length);
        wIntStream.forEach(row -> {
            for (int col = 0; col < matrix[0].length; col++) {
                if (matrix[row][col]) {
                    env.xori(output[row], wireInput[col]);
                }
            }
        });
        return output;
    }

    /**
     * SBox
     * (a,b,c) -> (a^bc, a^b^ac, a^b^c^ab)
     *
     * @param input (blockSize, num)
     */
    private void sBox1Round(MpcZ2Vector[] input) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "blockSize", input.length, blockSize);
        int startPoint = blockSize - 3 * this.numOfBoxes;
        MpcZ2Vector[] leftAndInput = new MpcZ2Vector[3 * this.numOfBoxes];
        MpcZ2Vector[] rightAndInput = new MpcZ2Vector[3 * this.numOfBoxes];

        for (int i = 0; i < this.numOfBoxes; i++) {
            leftAndInput[3 * i] = input[startPoint + 3 * i + 1];
            leftAndInput[3 * i + 1] = input[startPoint + 3 * i];
            leftAndInput[3 * i + 2] = input[startPoint + 3 * i];
            rightAndInput[3 * i] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 1] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 2] = input[startPoint + 3 * i + 1];
        }
        MpcZ2Vector[] andResult = this.env.and(leftAndInput, rightAndInput);
        for (int i = 0; i < this.numOfBoxes; i++) {
            MpcZ2Vector a = input[startPoint + 3 * i];
            MpcZ2Vector ab = this.env.xor(a, input[startPoint + 3 * i + 1]);
            MpcZ2Vector abc = this.env.xor(ab, input[startPoint + 3 * i + 2]);
            input[startPoint + 3 * i] = this.env.xor(a, andResult[3 * i]);
            input[startPoint + 3 * i + 1] = this.env.xor(ab, andResult[3 * i + 1]);
            input[startPoint + 3 * i + 2] = this.env.xor(abc, andResult[3 * i + 2]);
        }
    }

    /**
     * invBox
     * (x,y,z) -> (x^y^yz, y^xz, x^y^z^xy)
     *
     * @param input (blockSize, num)
     */
    private void invBox1Round(MpcZ2Vector[] input) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "blockSize", input.length, blockSize);
        int startPoint = blockSize - 3 * this.numOfBoxes;
        MpcZ2Vector[] leftAndInput = new MpcZ2Vector[3 * this.numOfBoxes];
        MpcZ2Vector[] rightAndInput = new MpcZ2Vector[3 * this.numOfBoxes];
        IntStream wIntStream = env.getParallel() ? IntStream.range(0, this.numOfBoxes).parallel() : IntStream.range(0, this.numOfBoxes);
        wIntStream.forEach(i -> {
            leftAndInput[3 * i] = input[startPoint + 3 * i + 1];
            leftAndInput[3 * i + 1] = input[startPoint + 3 * i];
            leftAndInput[3 * i + 2] = input[startPoint + 3 * i];
            rightAndInput[3 * i] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 1] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 2] = input[startPoint + 3 * i + 1];
        });
        MpcZ2Vector[] andResult = this.env.and(leftAndInput, rightAndInput);
        wIntStream = env.getParallel() ? IntStream.range(0, this.numOfBoxes).parallel() : IntStream.range(0, this.numOfBoxes);
        wIntStream.forEach(i -> {
            MpcZ2Vector y = input[startPoint + 3 * i + 1];
            MpcZ2Vector xy = this.env.xor(y, input[startPoint + 3 * i]);
            MpcZ2Vector xyz = this.env.xor(xy, input[startPoint + 3 * i + 2]);
            input[startPoint + 3 * i] = this.env.xor(xy, andResult[3 * i]);
            input[startPoint + 3 * i + 1] = this.env.xor(y, andResult[3 * i + 1]);
            input[startPoint + 3 * i + 2] = this.env.xor(xyz, andResult[3 * i + 2]);
        });
    }

    /**
     * generate random bits
     */
    private boolean[] randomBits(int length) {
        byte[] data = new byte[CommonUtils.getByteLength(length)];
        SECURE_RANDOM.nextBytes(data);
        return BinaryUtils.uncheckByteArrayToBinary(data, length);
    }
}

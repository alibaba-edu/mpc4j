package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.FileUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.AbstractSoprpParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations.PrpFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcSoprpPtoDesc.PrpSteps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 3p lowmc soprp party
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class LowMcSoprpParty extends AbstractSoprpParty implements SoprpParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(LowMcSoprpParty.class);
    /**
     * LowMC配置文件存储路径
     */
    private static final String LOW_MC_RESOURCE_FILE_PATH = "low_mc/";
    /**
     * LowMC配置参数后缀
     */
    private static final String LOW_MC_FILE_SUFFIX = "LowMc_Matrix.txt";
    /**
     * 用来执行协议的参与方
     */
    public TripletZ2cParty env;
    /**
     * lowMC所需的参数 box的数量
     */
    private final int numOfBoxes;
    /**
     * lowMC所需的参数 block的size
     */
    private final int blockSize;
    /**
     * lowMC所需的参数 加密轮数
     */
    private final int rounds;

    /**
     * lowMC所需的矩阵
     */
    public boolean[][][] linMatrices;
    public boolean[][][] invLinMatrices;
    public boolean[][] roundConstants;
    public boolean[][][] keyMatrices;
    public TripletZ2Vector[] roundKeys;

    public LowMcSoprpParty(Abb3Party abb3Party, LowMcSoprpConfig config) {
        super(LowMcSoprpPtoDesc.getInstance(), abb3Party, config);
        this.env = abb3Party.getZ2cParty();
        numOfBoxes = config.getParam().numOfBoxes;
        blockSize = config.getParam().blockSize;
        rounds = config.getParam().rounds;
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        getAbb3Party().init();
        this.key = env.getTripletProvider().getCrProvider().randRpShareZ2Vector(new int[]{CommonConstants.BLOCK_BIT_LENGTH})[0];
        this.extendKey(key);
        this.initMatrix();
        initState();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(TripletZ2Vector key) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        this.key = key;
        this.extendKey(key);
        this.initMatrix();
        initState();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public int getInputDim() {
        return blockSize;
    }

    @Override
    public long[] setUsage(PrpFnParam... params) {
        if (isMalicious) {
            long bitTupleNum = 0;
            for (PrpFnParam param : params) {
                int dataNum = CommonUtils.getByteLength(param.dataNum) << 3;
                bitTupleNum += 3L * numOfBoxes * rounds * dataNum;
            }
            abb3Party.updateNum(bitTupleNum, 0);
            return new long[]{bitTupleNum, 0};
        }else{
            return new long[]{0, 0};
        }
    }

    @Override
    public void setKey(TripletZ2Vector key) throws MpcAbortException {
        this.extendKey(key);
        this.keySchedule();
    }

    /**
     * 对输入的多个BinaryWire进行加密
     *
     * @param plainText (length, num)
     */
    @Override
    public TripletZ2Vector[] enc(TripletZ2Vector[] plainText) throws MpcAbortException {
        checkEncInput(plainText, blockSize);
        int numOfInput = plainText[0].getNum();
        BitVector allOneVec = BitVectorFactory.createOnes(numOfInput);
        BitVector allZeroVec = BitVectorFactory.createZeros(numOfInput);
        logPhaseInfo(PtoState.PTO_BEGIN, "enc lowmc");

        stopWatch.start();
        // initial whitening
        // state = plaintext + MultiplyWithGF2Matrix(KMatrix(0),key)
        for (int dim = 0; dim < blockSize; dim++) {
            int targetIndex = blockSize - dim - 1;
            TripletZ2Vector tmpRoundKey = (TripletZ2Vector) env.create(false,
                Arrays.stream(roundKeys[0].getBitVectors()).map(each ->
                    BinaryUtils.getBoolean(each.getBytes(), targetIndex) ? allOneVec : allZeroVec).toArray(BitVector[]::new));
            env.xori(state[dim], tmpRoundKey);
        }
        for (int r = 0; r < rounds; r++) {
            if (rpc.ownParty().getPartyId() == 0) {
                LOGGER.debug("encryption for {}-th round", r);
            }
            // m computations of 3-bit sBox,
            // remaining n-3m bits remain the same
            this.sBox1Round(state);
            // affine layer
            // state = MultiplyWithGF2Matrix(LMatrix(i),state)
            state = this.batchMultiplyWithF2Matrix(state, this.linMatrices[r]);
            // state = state + Constants(i)
            IntStream wIntStream = IntStream.range(0, blockSize);
            int finalR = r;
            TripletZ2Vector[] finalState = state;

            wIntStream.forEach(i -> {
                if (this.roundConstants[finalR][i]) {
                    env.noti(finalState[i]);
                }
                // generate round key and add to the state
                // state = state + MultiplyWithGF2Matrix(KMatrix(i + 1),key)
                int targetIndex = blockSize - i - 1;
                TripletZ2Vector tmpRoundKey = (TripletZ2Vector) env.create(false,
                    Arrays.stream(roundKeys[finalR + 1].getBitVectors()).map(each ->
                        BinaryUtils.getBoolean(each.getBytes(), targetIndex) ? allOneVec : allZeroVec).toArray(BitVector[]::new));
                env.xori(finalState[i], tmpRoundKey);
            });
        }
        stopWatch.stop();
        long computeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, computeTime);

        logPhaseInfo(PtoState.PTO_END, "enc lowmc");
        return state;
    }

    /**
     * 对输入的多个BinaryWire进行加密
     *
     * @param ciphertext (blockSize, num)
     * @param bitLength  返回的明文的长度
     */
    @Override
    public TripletZ2Vector[] dec(TripletZ2Vector[] ciphertext, int bitLength) {
        checkDecInput(ciphertext, blockSize);
        int numOfInput = ciphertext[0].getNum();
        BitVector allOneVec = BitVectorFactory.createOnes(numOfInput);
        BitVector allZeroVec = BitVectorFactory.createZeros(numOfInput);
        logPhaseInfo(PtoState.PTO_BEGIN, "dec lowmc");

        stopWatch.start();
        for (int r = rounds; r > 0; r--) {
            for (int i = 0; i < blockSize; i++) {
                // generate round key and add to the state
                // state = state + MultiplyWithGF2Matrix(KMatrix(i + 1),key)
                int targetIndex = blockSize - i - 1;
                TripletZ2Vector tmpRoundKey = (TripletZ2Vector) env.create(false,
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
            TripletZ2Vector tmpRoundKey = (TripletZ2Vector) env.create(false,
                Arrays.stream(roundKeys[0].getBitVectors()).map(each ->
                    BinaryUtils.getBoolean(each.getBytes(), targetIndex) ? allOneVec : allZeroVec).toArray(BitVector[]::new));
            env.xori(state[dim], tmpRoundKey);
        }
        stopWatch.stop();
        long computeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, computeTime);

        stopWatch.start();
        // 输出
        TripletZ2Vector[] output;
        if (bitLength == blockSize) {
            output = state;
        } else if (bitLength < blockSize) {
            // 如果所需的输入长度大于输出所需，则复制后面的几位
            output = new TripletZ2Vector[bitLength];
            System.arraycopy(state, blockSize - bitLength, output, 0, bitLength);
        } else {
            // 如果所需的输入长度大于输出所需，则将前面填充0
            output = new TripletZ2Vector[bitLength];
            for (int i = 0; i < bitLength - blockSize; i++) {
                output[i] = env.createShareZeros(numOfInput);
            }
            System.arraycopy(state, 0, output, bitLength - blockSize, blockSize);
        }
        stopWatch.stop();
        long postProcessTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, postProcessTime);

        return output;
    }


    /**
     * 初始化密钥，如果输入的位数不足，则在前面补0，否则取后面几位
     */
    private void extendKey(TripletZ2Vector key) throws MpcAbortException {
        if (key.getNum() >= CommonConstants.BLOCK_BIT_LENGTH) {
            this.key = key;
            key.reduce(CommonConstants.BLOCK_BIT_LENGTH);
        } else {
            throw new MpcAbortException("the length of key is too short: " + key.getNum());
        }
    }

    /**
     * 将现在的matrix编码为BigInteger便于存储和传输
     */
    public byte[][] encodeMatrix() {
        // 如果保存参数
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
     * 将得到的BigInteger数组解码出来
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
     * 生成所需的参数
     */
    public void genMatrix() {
        this.linMatrices = new boolean[rounds][blockSize][blockSize];
        this.invLinMatrices = new boolean[rounds][blockSize][blockSize];
        this.roundConstants = new boolean[rounds][blockSize];
        this.keyMatrices = new boolean[rounds + 1][blockSize][CommonConstants.BLOCK_BIT_LENGTH];
        // 生成linMatrices, invLinMatrices, keyMatrices
        for (int r = 0; r < rounds; r++) {
            // 生成 linMatrices 和 invLinMatrices
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
            // 生成keyMatrices
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
            // 重来一遍
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
            // 生成 roundConstants
            this.roundConstants[r] = this.randomBits(blockSize);
        }
    }

    /**
     * 验证当前参数是否合法，即在要求加密可逆时，确认相关的矩阵是否可逆
     */
    public void verifyLegal() throws MpcAbortException {
        for (int r = 0; r < rounds + 1; r++) {
            if (r < rounds) {
                MathPreconditions.checkEqual("rankOfMatrix", "blockSize", MatrixUtils.rankOfMatrix(this.linMatrices[r]), blockSize);
                // 需要的是这样相乘为单位矩阵
                MatrixUtils.testInvMatrix(this.invLinMatrices[r], this.linMatrices[r]);
            }
            MathPreconditions.checkEqual("rank of keyMatrices", "Math.min(blockSize, CommonConstants.BLOCK_BIT_LENGTH)", MatrixUtils.rankOfMatrix(this.keyMatrices[r]), Math.min(blockSize, CommonConstants.BLOCK_BIT_LENGTH));
        }

    }

    /**
     * 初始化所需的参数
     */
    public void initMatrix() throws MpcAbortException {
        LOGGER.info("initMatrix");
        this.linMatrices = new boolean[rounds][blockSize][blockSize];
        this.invLinMatrices = new boolean[rounds][blockSize][blockSize];
        this.roundConstants = new boolean[rounds][blockSize];
        this.keyMatrices = new boolean[rounds + 1][blockSize][CommonConstants.BLOCK_BIT_LENGTH];
        // matrix文件的名字
        String fileDir = "./" + LOW_MC_RESOURCE_FILE_PATH;
        File dir = new File(fileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileNameWithParam = fileDir + this.numOfBoxes + "_" + blockSize + "_" + rounds + LOW_MC_FILE_SUFFIX;
        File matrixFile = new File(fileNameWithParam);
        if (matrixFile.exists()) {
            LOGGER.info("已经存在密码文件了");
            BitVector[] data = FileUtils.readFileIntoBitVectors(fileNameWithParam, false);
            this.decode2Matrix(Arrays.stream(data).map(BitVector::getBytes).toArray(byte[][]::new));
        } else {
            LOGGER.info("no lowMc file exist");
            if (rpc.ownParty().getPartyId() == 0) {
                LOGGER.info("server start generate matrix");
                // 自己是server，承担生成并公开所有matrix的责任
                this.genMatrix();
                byte[][] savaArray = encodeMatrix();
                LOGGER.info("server start public matrix");
                List<byte[]> sendData = Arrays.stream(savaArray).collect(Collectors.toList());
                send(PrpSteps.INIT_MATRIX.ordinal(), rpc.getParty(1), sendData);
                send(PrpSteps.INIT_MATRIX.ordinal(), rpc.getParty(2), sendData);
                FileUtils.writeFile(Arrays.stream(savaArray).map(each -> BitVectorFactory.create(each.length << 3, each)).toArray(BitVector[]::new), fileNameWithParam);
                LOGGER.info("server finish public matrix");
            } else {
                // 自己是其他方，则从server那里接受数据，然后解码
                LOGGER.info("start receive matrix");
                byte[][] matrix = receive(PrpSteps.INIT_MATRIX.ordinal(), rpc.getParty(0)).toArray(new byte[0][]);
                this.decode2Matrix(matrix);
                if (!matrixFile.exists()) {
                    // 为了避免单机测试的时候，重复删除读取文件
                    FileUtils.writeFile(Arrays.stream(matrix).map(each -> BitVectorFactory.create(each.length << 3, each)).toArray(BitVector[]::new), fileNameWithParam);
                }
            }
        }
        // 直接算出 roundKeys
        this.keySchedule();
    }

    /**
     * 计算开始之前开始计算roundsKey
     */
    private void keySchedule() {
        roundKeys = new TripletZ2Vector[rounds + 1];
        for (int r = 0; r < rounds + 1; r++) {
            roundKeys[r] = this.multiplyWithF2Matrix(this.key, this.keyMatrices[r]);
        }
    }

    /**
     * 每一行之间进行and操作，结果组成新的数组
     * 用于生成roundKey
     */
    private TripletZ2Vector multiplyWithF2Matrix(TripletZ2Vector wireInput, boolean[][] matrix) {
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
        return (TripletZ2Vector) env.create(false, resVec);
    }

    /**
     * 当输入的维度改变时，可以用行来计算
     *
     * @param wireInput 输入是(bitLen, num)
     * @param matrix    需要基于的明文矩阵是(bitLen, bitLen)
     */
    private TripletZ2Vector[] batchMultiplyWithF2Matrix(TripletZ2Vector[] wireInput, boolean[][] matrix) {
        MathPreconditions.checkEqual("wireInput.length", "matrix[0].length", wireInput.length, matrix[0].length);
        TripletZ2Vector[] output = IntStream.range(0, matrix.length).mapToObj(i ->
            env.createShareZeros(wireInput[0].getNum())).toArray(TripletZ2Vector[]::new);
        IntStream wIntStream = parallel ? IntStream.range(0, matrix.length).parallel() : IntStream.range(0, matrix.length);
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
     * SBox的操作，需要注意的是：计算过程中已经改变了input
     * (a,b,c) -> (a^bc, a^b^ac, a^b^c^ab)
     *
     * @param input (blockSize, num)
     */
    private void sBox1Round(TripletZ2Vector[] input) {
        MathPreconditions.checkEqual("input.length", "blockSize", input.length, blockSize);
        int startPoint = blockSize - 3 * this.numOfBoxes;
        // 计算过程中已经改变了input
        TripletZ2Vector[] leftAndInput = new TripletZ2Vector[3 * this.numOfBoxes];
        TripletZ2Vector[] rightAndInput = new TripletZ2Vector[3 * this.numOfBoxes];

        for (int i = 0; i < this.numOfBoxes; i++) {
            leftAndInput[3 * i] = input[startPoint + 3 * i + 1];
            leftAndInput[3 * i + 1] = input[startPoint + 3 * i];
            leftAndInput[3 * i + 2] = input[startPoint + 3 * i];
            rightAndInput[3 * i] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 1] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 2] = input[startPoint + 3 * i + 1];
        }
        TripletZ2Vector[] andResult = this.env.and(leftAndInput, rightAndInput);
        for (int i = 0; i < this.numOfBoxes; i++) {
            TripletZ2Vector a = input[startPoint + 3 * i];
            TripletZ2Vector ab = this.env.xor(a, input[startPoint + 3 * i + 1]);
            TripletZ2Vector abc = this.env.xor(ab, input[startPoint + 3 * i + 2]);
            input[startPoint + 3 * i] = this.env.xor(a, andResult[3 * i]);
            input[startPoint + 3 * i + 1] = this.env.xor(ab, andResult[3 * i + 1]);
            input[startPoint + 3 * i + 2] = this.env.xor(abc, andResult[3 * i + 2]);
        }
    }

    /**
     * invBox的操作，需要注意的是：计算过程中已经改变了input
     * (x,y,z) -> (x^y^yz, y^xz, x^y^z^xy)
     *
     * @param input (blockSize, num)
     */
    private void invBox1Round(TripletZ2Vector[] input) {
        MathPreconditions.checkEqual("input.length", "blockSize", input.length, blockSize);
        int startPoint = blockSize - 3 * this.numOfBoxes;
        // 计算过程中已经改变了input
        TripletZ2Vector[] leftAndInput = new TripletZ2Vector[3 * this.numOfBoxes];
        TripletZ2Vector[] rightAndInput = new TripletZ2Vector[3 * this.numOfBoxes];
        IntStream wIntStream = parallel ? IntStream.range(0, this.numOfBoxes).parallel() : IntStream.range(0, this.numOfBoxes);
        wIntStream.forEach(i -> {
            leftAndInput[3 * i] = input[startPoint + 3 * i + 1];
            leftAndInput[3 * i + 1] = input[startPoint + 3 * i];
            leftAndInput[3 * i + 2] = input[startPoint + 3 * i];
            rightAndInput[3 * i] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 1] = input[startPoint + 3 * i + 2];
            rightAndInput[3 * i + 2] = input[startPoint + 3 * i + 1];
        });
        TripletZ2Vector[] andResult = this.env.and(leftAndInput, rightAndInput);
        wIntStream = parallel ? IntStream.range(0, this.numOfBoxes).parallel() : IntStream.range(0, this.numOfBoxes);
        wIntStream.forEach(i -> {
            TripletZ2Vector y = input[startPoint + 3 * i + 1];
            TripletZ2Vector xy = this.env.xor(y, input[startPoint + 3 * i]);
            TripletZ2Vector xyz = this.env.xor(xy, input[startPoint + 3 * i + 2]);
            input[startPoint + 3 * i] = this.env.xor(xy, andResult[3 * i]);
            input[startPoint + 3 * i + 1] = this.env.xor(y, andResult[3 * i + 1]);
            input[startPoint + 3 * i + 2] = this.env.xor(xyz, andResult[3 * i + 2]);
        });
    }

    /**
     * 生成random的bits
     */
    private boolean[] randomBits(int length) {
        byte[] data = new byte[CommonUtils.getByteLength(length)];
        secureRandom.nextBytes(data);
        return BinaryUtils.uncheckByteArrayToBinary(data, length);
    }
}

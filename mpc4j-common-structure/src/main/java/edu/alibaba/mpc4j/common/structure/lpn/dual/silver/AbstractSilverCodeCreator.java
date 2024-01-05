package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.LowerTriSquareSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.NaiveSparseBitMatrix;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.ExtremeSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * LdpcCreator的抽象类，实现接口LdpcCreator。
 *
 * Ldpc由稀疏矩阵H定义，H由分块矩阵 A, B，C，D, E，F 构成, 上半区 (A,B,C)，下半区 （D,E,F）。
 * 其中各分块矩阵的维度由参数 k 和 参数 gap确定，具体：
 * 矩阵 A, 行 k - gap，列 k - gap；
 * 矩阵 B，行 k - gap, 列 gap；
 * 矩阵 C，行 k - gap, 列 k - gap；
 * 矩阵 D，行 gap, 列 k - gap；
 * 矩阵 E，行 gap, 列 gap；
 * 矩阵 F，行 gap， 列 k - gap；
 * 以上矩阵均为稀疏矩阵 SparseMatrix，其中 D,F 大部分列为空，属于极度稀疏矩阵 ExtremeSparseBitMatrix。
 *
 * 此外，LdpcCoder 的转置编码计算 需要 矩阵 Ep = (F*C^{-1}*B）+ E)^{-1}。
 * LdpcCreator 需要根据指定输出OT数量和Ldpc 类型，生成上述矩阵及相关参数，用于创建 LdpcCoder。
 * 具体定义见论文: Silver: Silent VOLE and Oblivious Transfer from Hardness of Decoding Structured LDPC Codes
 * http://eprint.iacr.org/2021/1150
 *
 * @author Hanwen Feng
 * @date 2022/03/13
 */
public abstract class AbstractSilverCodeCreator implements SilverCodeCreator {
    /**
     * 需要生成的Ldpc 类型
     */
    protected SilverCodeType silverCodeType;
    /**
     * 分块矩阵A
     */
    protected NaiveSparseBitMatrix matrixA;
    /**
     * 分块矩阵B
     */
    protected NaiveSparseBitMatrix matrixB;
    /**
     * 分块矩阵C
     */
    protected LowerTriSquareSparseBitMatrix matrixC;
    /**
     * 分块矩阵D
     */
    protected ExtremeSparseBitMatrix matrixD;
    /**
     * 分块矩阵F
     */
    protected ExtremeSparseBitMatrix matrixF;
    /**
     * 矩阵Ep
     */
    protected DenseBitMatrix matrixEp;
    /**
     * Ldpc Encoder 最终生成的OT数量的对数。
     * 例如ceilLogN = 24, 则该Ldpc 可以产生 2^24的OT。
     */
    protected int ceilLogN;
    /**
     * Ldpc 参数 gap
     */
    protected int gapValue;
    /**
     * Ldpc 参数 k
     */
    protected int kValue;
    /**
     * 矩阵H左矩阵每列的汉明重量
     */
    protected int weight;

    /**
     * Lpdc 根据预置的种子信息生成，种子定义了矩阵H所有非零点的位置。
     * (A,B,D，E) 根据leftseed生成，（C,F）根据rightSeed生成。
     */
    protected int[][] rightSeed;
    protected double[] leftSeed;
    /**
     * LDPC 对应的LPN参数
     */
    protected LpnParams lpnParams;
    /**
     * 存储目录
     */
    protected static final String SILVER_RESOURCES_FILE_PATH = "silver" + File.separator + "z2" + File.separator;
    /**
     * 存储文件后缀
     */
    protected static final String SILVER_RESOURCES_FILE_SURFFIX = ".txt";
    /**
     * 根据code类型，读取生成Ldpc参数信息。
     */
    protected void initParams(SilverCodeType silverCodeType, int ceilLogN) {
        this.ceilLogN = ceilLogN;
        this.silverCodeType = silverCodeType;
        rightSeed = SilverCodeCreatorUtils.getRightSeed(silverCodeType);
        leftSeed = SilverCodeCreatorUtils.getLeftSeed(silverCodeType);
        gapValue = SilverCodeCreatorUtils.getGap(silverCodeType);
        weight = SilverCodeCreatorUtils.getWeight(silverCodeType);
    }

    /**
     * 将lpn参数和矩阵ep写入文件
     */
    protected void writeToFile() {
        String silverFileName = getFileName();
        try {
            File silverFile = new File(silverFileName);
            if (silverFile.exists()) {
                boolean deleted = silverFile.delete();
                if (!deleted) {
                    throw new IllegalStateException("File: " + silverFileName + " exists and cannot delete!");
                }
            }
            silverFile.getParentFile().mkdirs();
            FileWriter fileWriter = new FileWriter(silverFile);
            PrintWriter printWriter = new PrintWriter(fileWriter, true);
            // write lpn params
            int[] lpnParaArray = {lpnParams.getN(), lpnParams.getK(), lpnParams.getT()};
            printWriter.println(Base64.getEncoder().encodeToString(IntUtils.intArrayToByteArray(lpnParaArray)));
            // write matrix Ep
            Arrays.stream(matrixEp.getByteArrayData()).forEach(byteArray ->
                printWriter.println(Base64.getEncoder().encodeToString(byteArray))
            );
            printWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unknown IOException");
        }
    }

    protected void initFromFile() {
        String silverFileName = getFileName();
        try {
            InputStream silverInputStream = Objects.requireNonNull(
                AbstractSilverCodeCreator.class.getClassLoader().getResourceAsStream(silverFileName)
            );
            BufferedReader silverBufferedReader = new BufferedReader(new InputStreamReader(silverInputStream));
            // read lpnParams
            int[] lpnArray = IntUtils.byteArrayToIntArray(Base64.getDecoder().decode(silverBufferedReader.readLine()));
            assert lpnArray.length == 3;
            lpnParams = LpnParams.uncheckCreate(lpnArray[0], lpnArray[1], lpnArray[2]);
            // read matrixEp
            byte[][] matrixEpArrays = new byte[gapValue][];
            for (int rowIndex = 0; rowIndex < gapValue; rowIndex++) {
                matrixEpArrays[rowIndex] = Base64.getDecoder().decode(silverBufferedReader.readLine());
            }
            matrixEp = ByteDenseBitMatrix.createFromDense(gapValue, matrixEpArrays);
            silverBufferedReader.close();
            silverInputStream.close();
        } catch (NullPointerException | IOException e) {
            throw new IllegalStateException("File: " + silverFileName + " cannot be read.");
        }
    }

    private String getFileName() {
        return SILVER_RESOURCES_FILE_PATH + silverCodeType.name() + "_" + ceilLogN + SILVER_RESOURCES_FILE_SURFFIX;
    }

    @Override
    public SilverCoder createCoder() {
        return new SilverCoder(matrixA, matrixB, matrixC, matrixD, matrixF, matrixEp, gapValue, kValue);
    }

    @Override
    public LpnParams getLpnParams() {
        return lpnParams;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractSilverCodeCreator)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        AbstractSilverCodeCreator that = (AbstractSilverCodeCreator) obj;
        /*
        * 主要用于检验多种LdpcCreator创建对象是否相同。
        * 当所有矩阵都相同时，gapValue， kValue， weight， ceiLogN一定相同，所以不参加比较。
         */
        return new EqualsBuilder()
                .append(this.matrixA, that.matrixA)
                .append(this.matrixB, that.matrixB)
                .append(this.matrixC, that.matrixC)
                .append(this.matrixD, that.matrixD)
                .append(this.matrixF, that.matrixF)
                .append(this.matrixEp, that.matrixEp)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.matrixA)
                .append(this.matrixB)
                .append(this.matrixC)
                .append(this.matrixD)
                .append(this.matrixF)
                .append(this.matrixEp)
                .toHashCode();
    }
}

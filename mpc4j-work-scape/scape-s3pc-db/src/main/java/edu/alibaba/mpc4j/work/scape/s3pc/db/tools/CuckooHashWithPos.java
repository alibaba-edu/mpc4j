package edu.alibaba.mpc4j.work.scape.s3pc.db.tools;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * cuckoo hash with position for PK-PK join
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class CuckooHashWithPos {
    /**
     * 随机数
     */
    private final SecureRandom secureRandom;
    /**
     * 最大交换次数系数
     */
    private final static double MAX_SWAP_TIME = 1.0;
    /**
     * 最大交换次数
     */
    private final long maxSwapTime;
    /**
     * 当前交换次数
     */
    private long swapCount;
    /**
     * hash数量
     */
    private final int hashNum;
    /**
     * hash值
     */
    private final int[][] mapValues;
    /**
     * 索引
     */
    private final int[] index;
    public CuckooHashWithPos(int hashNum, int[][] hashPos) {
        this.swapCount = 0;
        this.maxSwapTime = (long) (MAX_SWAP_TIME * hashPos[0].length);
        this.hashNum = hashNum;
        this.mapValues = hashPos;
        this.index = new int[getHashParam(hashPos[0].length)[1]];
        Arrays.fill(this.index, -1);
        secureRandom = new SecureRandom();
    }

    public static int[] getHashParam(int dataNum){
        int hashBitLen = LongUtils.ceilLog2(dataNum) + 1;
        int hashBinNum = 1<<hashBitLen;
        return new int[]{hashBitLen, hashBinNum};
    }

    public boolean insertAllItems() {
        for(int i = 0; i < this.mapValues[0].length; i++){
            if(!this.insertSingle(i)){
                return false;
            }
        }
        return true;
    }

    /**
     * 插入一个元素
     */
    public boolean insertSingle(int sourceIndex){
        while(true){
            int tryIndex = 0;
            // 如果能插进去
            while(tryIndex < this.hashNum){
                int target = this.mapValues[tryIndex][sourceIndex];
                if(this.index[target] == -1){
                    this.index[target] = sourceIndex;
                    return true;
                }
                tryIndex++;
            }
            // 如果插不进去
            this.swapCount++;
            int randomPrfIndex = this.secureRandom.nextInt(this.mapValues.length);
            int tmpSwitchPos = this.mapValues[randomPrfIndex][sourceIndex];
            int swapIndex = this.index[tmpSwitchPos];
            this.index[tmpSwitchPos] = sourceIndex;
            sourceIndex = swapIndex;
            if(this.swapCount >= this.maxSwapTime){
                return false;
            }
        }
    }

    /**
     * 得到当前hash表中对应的映射
     * 如果对应的位置有数据，则填这个数据对应的原始index
     * 如果对应位置没有数据，则填一个随机的、与之前东西不同的index
     */
    public int[] getHashPermutation() {
        int fillNum = this.index.length - this.mapValues[0].length;
        SecureRandom secureRandom = new SecureRandom();
        int[] forPerGen = IntStream.range(0, fillNum).map(i -> secureRandom.nextInt()).toArray();
        int[] randomPai = ShuffleUtils.permutationGeneration(forPerGen);
        int[] res = Arrays.copyOf(this.index, this.index.length);
        int startIndex = 0;
        for(int i = 0; i < res.length; i++){
            if(res[i] == -1){
                res[i] = randomPai[startIndex] + this.mapValues[0].length;
                startIndex++;
            }
        }
        MathPreconditions.checkEqual("startIndex", "fillNum", startIndex, fillNum);
        return res;
    }
}


package edu.alibaba.mpc4j.common.data;

import smile.data.DataFrame;

import java.util.stream.IntStream;

/**
 * 数据帧工具类。
 *
 * @author Weiran Liu
 * @date 2022/6/28
 */
public class DataFrameUtils {
    /**
     * 私有构造函数
     */
    private DataFrameUtils() {
        // empty
    }

    /**
     * 纵向切分数据帧。
     *
     * @param dataFrame 数据帧。
     * @param num       切分数量。
     * @return 切分结果。
     */
    public static DataFrame[] split(DataFrame dataFrame, int num) {
        assert dataFrame.ncols() >= num : "# of columns must be greater than or equal to " + num + ": " + dataFrame.ncols();
        // 根据列数量和切分数量计算平均每个切分数据帧的列数量，按照取整的方式切分数据帧
        int columnNum = dataFrame.ncols() / num;
        return IntStream.range(0, num)
            .mapToObj(splitIndex -> {
                int left = splitIndex * columnNum;
                int right = (splitIndex == num - 1) ? dataFrame.ncols() : (splitIndex + 1) * columnNum;
                return dataFrame.select(IntStream.range(left, right).toArray());
            })
            .toArray(DataFrame[]::new);
    }
}

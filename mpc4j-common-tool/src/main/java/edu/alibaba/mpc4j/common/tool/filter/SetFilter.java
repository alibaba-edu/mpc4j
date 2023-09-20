package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * filter implemented by {@code HashSet<ByteBuffer>}.
 *
 * @author Weiran Liu
 * @date 2020/06/30
 */
public class SetFilter<T> implements MergeFilter<T> {
    /**
     * no hash num
     */
    static final int HASH_NUM = 0;
    /**
     * no hash key
     */
    static final int HASH_KEY_NUM = 0;

    /**
     * Creates an empty filter.
     *
     * @param maxSize max number of inserted elements.
     * @return an empty filter.
     */
    static <X> SetFilter<X> create(int maxSize) {
        MathPreconditions.checkPositive("maxSize", maxSize);
        SetFilter<X> setFilter = new SetFilter<>();
        // 需要用线程安全的集合封装初始化的集合
        setFilter.set = new HashSet<>(maxSize);
        setFilter.maxSize = maxSize;

        return setFilter;
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> SetFilter<X> fromByteArrayList(List<byte[]> byteArrayList) {
        MathPreconditions.checkGreaterOrEqual("byteArrayList.size", byteArrayList.size(), 3);
        Preconditions.checkArgument(byteArrayList.size() >= 3);
        SetFilter<X> setFilter = new SetFilter<>();
        // type
        byteArrayList.remove(0);
        // maxSize
        setFilter.maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // size
        int size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // elements
        MathPreconditions.checkEqual("element num", "size", byteArrayList.size(), size);
        setFilter.set = byteArrayList.stream().map(ByteBuffer::wrap).collect(Collectors.toSet());
        byteArrayList.clear();

        return setFilter;
    }

    /**
     * set
     */
    private Set<ByteBuffer> set;
    /**
     * max number of elements.
     */
    private int maxSize;

    private SetFilter() {
        // empty
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.SET_FILTER;
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public boolean mightContain(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        return set.contains(ByteBuffer.wrap(dataBytes));
    }

    @Override
    public void put(T data) {
        MathPreconditions.checkLess("size", size(), maxSize);
        // insert element in bytes
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        if (!set.add(ByteBuffer.wrap(dataBytes))) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // type
        byteArrayList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
        // maxSize
        byteArrayList.add(IntUtils.intToByteArray(maxSize()));
        // size
        byteArrayList.add(IntUtils.intToByteArray(size()));
        // elements
        byteArrayList.addAll(set.stream().map(ByteBuffer::array).map(BytesUtils::clone).collect(Collectors.toList()));

        return byteArrayList;
    }

    @Override
    public double ratio() {
        return 1.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SetFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        SetFilter<T> that = (SetFilter<T>) obj;
        return new EqualsBuilder()
            .append(this.getFilterType(), that.getFilterType())
            .append(this.maxSize, that.maxSize)
            .append(this.size(), that.size())
            .append(this.set, that.set)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(maxSize)
            .append(size())
            .append(set)
            .toHashCode();
    }

    @Override
    public void merge(MergeFilter<T> other) {
        SetFilter<T> that = (SetFilter<T>) other;
        MathPreconditions.checkEqual("this.maxSize", "that.maxSize", this.maxSize, that.maxSize);
        MathPreconditions.checkLessOrEqual("merge size", this.size() + that.size(), maxSize);
        set.addAll(that.set);
    }
}

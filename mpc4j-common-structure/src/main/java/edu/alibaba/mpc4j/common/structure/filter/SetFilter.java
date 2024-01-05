package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
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
     * filter type
     */
    private static final FilterType FILTER_TYPE = FilterType.SET_FILTER;
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
    static <X> SetFilter<X> load(List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);
        SetFilter<X> setFilter = new SetFilter<>();

        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, FILTER_TYPE.ordinal());

        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // maxSize
        setFilter.maxSize = headerByteBuffer.getInt();
        // size
        int size = headerByteBuffer.getInt();
        Preconditions.checkArgument(size <= setFilter.maxSize);

        // read elements
        List<byte[]> origin = SerializeUtils.decompressUnequal(byteArrayList.remove(0));
        setFilter.set = origin.stream().map(ByteBuffer::wrap).collect(Collectors.toSet());

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
        return FILTER_TYPE;
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
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(FILTER_TYPE.ordinal()));

        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 2);
        // maxSize
        headerByteBuffer.putInt(maxSize());
        // size
        headerByteBuffer.putInt(size());
        byteArrayList.add(headerByteBuffer.array());

        // write data
        List<byte[]> origin = set.stream().map(ByteBuffer::array).collect(Collectors.toList());
        byteArrayList.add(SerializeUtils.compressUnequal(origin));

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

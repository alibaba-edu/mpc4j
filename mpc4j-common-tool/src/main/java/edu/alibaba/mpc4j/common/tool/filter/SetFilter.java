package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
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
 * 用{@code HashSet<ByteBuffer>}实现的过滤器。
 *
 * @author Weiran Liu
 * @date 2020/06/30
 */
public class SetFilter<T> implements MergeFilter<T> {
    /**
     * 集合过滤器没有用到哈希函数。
     */
    static final int HASH_NUM = 0;
    /**
     * 用于存储数据的集合
     */
    private Set<ByteBuffer> set;
    /**
     * 期望插入的元素数量
     */
    private int maxSize;

    /**
     * 创建一个空的集合过滤器。
     *
     * @param maxSize 期望插入的元素数量。
     * @return 空的集合过滤器。
     */
    static <X> SetFilter<X> create(int maxSize) {
        assert maxSize > 0;
        SetFilter<X> setFilter = new SetFilter<>();
        // 需要用线程安全的集合封装初始化的集合
        setFilter.set = Collections.synchronizedSet(new HashSet<>(maxSize));
        setFilter.maxSize = maxSize;

        return setFilter;
    }

    /**
     * 将用{@code List<byte[]>}表示的过滤器转换为集合过滤器。
     *
     * @param byteArrayList 用{@code List<byte[]>}表示的过滤器。
     * @param <X>           过滤器存储元素类型。
     * @return 集合过滤器。
     */
    static <X> SetFilter<X> fromByteArrayList(List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() >= 3);
        SetFilter<X> setFilter = new SetFilter<>();
        // 移除过滤器类型
        byteArrayList.remove(0);
        // 预计插入的元素数量
        setFilter.maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 已经插入的元素数量
        int size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        Preconditions.checkArgument(byteArrayList.size() == size);
        // 剩余的元素应该都是SetFilter中的元素，插入即可
        setFilter.set = byteArrayList.stream().map(ByteBuffer::wrap).collect(Collectors.toSet());
        byteArrayList.clear();

        return setFilter;
    }

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
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        return set.contains(ByteBuffer.wrap(objectBytes));
    }

    @Override
    public void put(T data) {
        assert size() < maxSize;
        // 将元素转换为字节数组后插入
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        if (!set.add(ByteBuffer.wrap(objectBytes))) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // 第1个元素为过滤器类型
        byteArrayList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
        // 第2个元素为SetFilter预计插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(maxSize()));
        // 第3个元素为SetFilter已经插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(size()));
        // 依次插入元素
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
        SetFilter<T> that = (SetFilter<T>)obj;
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
    public void merge(MergeFilter<T> otherFilter) {
        assert otherFilter instanceof SetFilter;
        assert maxSize == otherFilter.maxSize();
        assert maxSize >= size() + otherFilter.size();
        SetFilter<T> otherSetFilter = (SetFilter<T>)otherFilter;
        set.addAll(otherSetFilter.set);
    }
}

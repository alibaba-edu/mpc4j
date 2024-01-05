package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * abstract Bloom Filter.
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
abstract class AbstractBloomFilter<T> implements BloomFilter<T> {
    /**
     * Bloom Filter type
     */
    protected final FilterType bloomFilterType;
    /**
     * max number of elements
     */
    protected final int maxSize;
    /**
     * storage bit length
     */
    protected final int m;
    /**
     * offset
     */
    private final int offset;
    /**
     * storage
     */
    protected final byte[] storage;
    /**
     * number of inserted elements
     */
    protected int size;
    /**
     * item byte length, used for computing compress radio
     */
    protected int itemByteLength;
    /**
     * number of hashes
     */
    private final int hashNum;
    /**
     * hash
     */
    protected final Prf hash;

    /**
     * Creates a Bloom Filter.
     *
     * @param bloomFilterType Bloom Filter type.
     * @param envType         environment.
     * @param maxSize         max number of element.
     * @param m               number of positions in the Bloom Filter.
     * @param size            number of inserted elements.
     * @param storage         the storage.
     * @param itemByteLength  sum of byte length for inserted elements.
     */
    AbstractBloomFilter(FilterType bloomFilterType, EnvType envType, int maxSize, int m, int hashNum, byte[] key,
                        int size, byte[] storage, int itemByteLength) {
        this.bloomFilterType = bloomFilterType;
        MathPreconditions.checkPositive("maxSize", maxSize);
        this.maxSize = maxSize;
        this.m = m;
        int byteM = CommonUtils.getByteLength(m);
        offset = byteM * Byte.SIZE - m;
        MathPreconditions.checkPositive("hashNum", hashNum);
        this.hashNum = hashNum;
        // we use on hash to compute all positions
        hash = PrfFactory.createInstance(envType, hashNum * Integer.BYTES);
        hash.setKey(key);
        MathPreconditions.checkNonNegative("size", size);
        this.size = size;
        MathPreconditions.checkEqual("storage.length", "byteM", storage.length, byteM);
        this.storage = storage;
        MathPreconditions.checkNonNegative("itemByteLength", itemByteLength);
        this.itemByteLength = itemByteLength;
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(bloomFilterType.ordinal()));

        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 3 + CommonConstants.BLOCK_BYTE_LENGTH);
        // maxSize
        headerByteBuffer.putInt(maxSize());
        // size
        headerByteBuffer.putInt(size());
        // item byte length
        headerByteBuffer.putInt(itemByteLength);
        // key
        headerByteBuffer.put(hash.getKey());
        byteArrayList.add(headerByteBuffer.array());

        // write storage
        byteArrayList.add(BytesUtils.clone(storage));

        return byteArrayList;
    }

    @Override
    public FilterType getFilterType() {
        return bloomFilterType;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public byte[] getStorage() {
        return storage;
    }

    @Override
    public int getM() {
        return m;
    }

    @Override
    public boolean mightContain(T data) {
        // verify each position in the sparse indexes is true
        int[] hashIndexes = hashIndexes(data);
        for (int index : hashIndexes) {
            if (!BinaryUtils.getBoolean(storage, index + offset)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void put(T data) {
        MathPreconditions.checkLess("size", size, maxSize);
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        // mightContain has checked if there is any collision, here we can directly insert the item.
        int[] hashIndexes = hashIndexes(data);
        for (int index : hashIndexes) {
            if (!BinaryUtils.getBoolean(storage, index + offset)) {
                BinaryUtils.setBoolean(storage, index + offset, true);
            }
        }
        // update the item byte length
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        itemByteLength += dataBytes.length;
        size++;
    }

    @Override
    public double ratio() {
        return (double) storage.length / itemByteLength;
    }

    @Override
    public void merge(MergeFilter<T> other) {
        Preconditions.checkArgument(this.getClass().equals(other.getClass()));
        AbstractBloomFilter<T> that = (AbstractBloomFilter<T>) other;
        // max size should be the same
        MathPreconditions.checkEqual("this.maxSize", "that.maxSize", this.maxSize, that.maxSize);
        MathPreconditions.checkEqual("this.hashNum", "that.hashNum", this.hashNum, that.hashNum);
        // hash type should be equal
        Preconditions.checkArgument(this.hash.getPrfType().equals(that.hash.getPrfType()));
        // key should be equal
        Preconditions.checkArgument(Arrays.equals(hash.getKey(), that.hash.getKey()));
        MathPreconditions.checkLessOrEqual("merge size", this.size + that.size, maxSize);
        // merge Bloom Filter
        BytesUtils.ori(storage, that.storage);
        size += that.size;
        itemByteLength += that.itemByteLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractBloomFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        AbstractBloomFilter<T> that = (AbstractBloomFilter<T>) obj;
        return new EqualsBuilder()
            .append(this.getClass(), that.getClass())
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.storage, that.storage)
            .append(this.hashNum, that.hashNum)
            .append(this.hash.getPrfType(), that.hash.getPrfType())
            .append(this.hash.getKey(), that.hash.getKey())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getClass())
            .append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(storage)
            .append(hashNum)
            .append(hash.getPrfType())
            .append(hash.getKey())
            .toHashCode();
    }
}

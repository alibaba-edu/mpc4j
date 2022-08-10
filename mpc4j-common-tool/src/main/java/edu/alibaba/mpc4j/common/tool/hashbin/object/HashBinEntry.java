package edu.alibaba.mpc4j.common.tool.hashbin.object;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * 哈希桶条目。
 *
 * @author Weiran Liu
 * @date 2021/12/20
 */
public class HashBinEntry<T> {
    /**
     * 虚拟条目的哈希索引值
     */
    public static final int DUMMY_ITEM_HASH_INDEX = -1;
    /**
     * 哈希桶条目中的元素
     */
    private T item;
    /**
     * 随机元素对应的字节数组
     */
    private byte[] randomByteArray;
    /**
     * 哈希桶条目中元素对应的哈希函数索引值
     */
    private int hashIndex;

    /**
     * 构建真实元素的哈希桶条目。
     *
     * @param hashIndex 哈希索引值。
     * @param item 插入的元素。
     * @param <X> 元素类型。
     * @return 真值元素的哈希桶条目。
     */
    public static <X> HashBinEntry<X> fromRealItem(int hashIndex, X item) {
        // 哈希索引值必须大于等于0，-1用于虚拟哈希元素
        assert hashIndex >= 0;
        HashBinEntry<X> hashBinEntry = new HashBinEntry<>();
        hashBinEntry.item = item;
        hashBinEntry.randomByteArray = null;
        hashBinEntry.hashIndex = hashIndex;

        return hashBinEntry;
    }

    /**
     * 构建空元素的哈希桶条目。
     *
     * @param emptyItem 空元素。
     * @param <X> 元素类型。
     * @return 空元素的哈希桶条目。
     */
    public static <X> HashBinEntry<X> fromEmptyItem(X emptyItem) {
        HashBinEntry<X> hashBinEntry = new HashBinEntry<>();
        hashBinEntry.item = emptyItem;
        hashBinEntry.randomByteArray = null;
        // 空元素的索引值为-1
        hashBinEntry.hashIndex = DUMMY_ITEM_HASH_INDEX;

        return hashBinEntry;
    }

    /**
     * 构建虚拟随机元素的哈希桶条目。
     *
     * @param secureRandom 随机状态。
     * @param <X> 元素类型。
     * @return 随机元素的哈希桶条目。
     */
    public static <X> HashBinEntry<X> fromDummyItem(SecureRandom secureRandom) {
        // 虚拟随机元素的原始值为空，索引值为-1，虚拟元素对应的字节数组是一个128比特长的随机数
        HashBinEntry<X> hashBinEntry = new HashBinEntry<>();
        hashBinEntry.item = null;
        hashBinEntry.hashIndex = DUMMY_ITEM_HASH_INDEX;
        hashBinEntry.randomByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(hashBinEntry.randomByteArray);

        return hashBinEntry;
    }

    private HashBinEntry() {
        // empty
    }

    /**
     * 返回哈希桶条目中的元素。
     *
     * @return 哈希桶条目中的元素。
     */
    public T getItem() {
        return item;
    }

    /**
     * 返回哈希桶条目中元素对应的字节数组。
     *
     * @return 哈希桶条目中元素对应的字节数组。
     */
    public byte[] getItemByteArray() {
        if (randomByteArray == null) {
            return ObjectUtils.objectToByteArray(item);
        } else {
            return randomByteArray;
        }
    }

    /**
     * 返回哈希桶条目中元素对应的哈希函数索引值。
     *
     * @return 哈希桶条目中元素对应的哈希函数索引值。
     */
    public int getHashIndex() {
        return hashIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HashBinEntry)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        @SuppressWarnings("rawtypes")
        HashBinEntry that = (HashBinEntry)obj;
        return new EqualsBuilder()
            .append(this.item, that.item)
            // 虚拟元素的item均为空，因此还要对比字节数组
            .append(this.randomByteArray, that.randomByteArray)
            .append(this.hashIndex, that.hashIndex).
            isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(item)
            .append(randomByteArray)
            .append(hashIndex)
            .toHashCode();
    }
}

package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * 对象工具类，主要用于对象到字节数组的转换，以及将对象映射为（可能不完全随机）整数的方法。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class ObjectUtils {
    /**
     * 私有构造函数。
     */
    private ObjectUtils() {
        // empty
    }

    /**
     * 将任意{@code Object}转换为{@code byte[]}，并保证修改转换得到的{@code byte[]}不影响原始{@code Object}。
     *
     * @param object 给定的{@code Object}。
     * @return 转换结果。
     * @throws UnsupportedOperationException 如果无法将此对象转换为{@code byte[]}。
     */
    public static byte[] objectToByteArray(final Object object) {
        if (object instanceof BigInteger) {
            return BigIntegerUtils.bigIntegerToByteArray((BigInteger)object);
        } else if (object instanceof String) {
            return ((String)object).getBytes(CommonConstants.DEFAULT_CHARSET);
        } else if (object instanceof byte[]) {
            return BytesUtils.clone((byte[])object);
        } else if (object instanceof ByteBuffer) {
            return BytesUtils.clone(((ByteBuffer)object).array());
        } else if (object instanceof Integer) {
            return IntUtils.intToByteArray((Integer) object);
        } else if (object instanceof Long) {
            return LongUtils.longToByteArray((Long) object);
        } else if (object instanceof Double) {
            return DoubleUtils.doubleToByteArray((Double) object);
        } else if (object instanceof Serializable) {
            return serializableObjectToByteArray(object);
        }
        throw new UnsupportedOperationException(
            String.format("无法将%s转换为byte[]", object.getClass().getSimpleName())
        );
    }

    /**
     * 将可序列化{@code Object}转换为{@code byte[]}。
     *
     * @param object 可序列化的{@code Object}。
     * @return 序列化结果。
     * @throws UnsupportedOperationException 如果无法将对象序列化为{@code byte[]}。
     */
    public static byte[] serializableObjectToByteArray(Object object) throws UnsupportedOperationException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            byte[] objectByteArray = byteArrayOutputStream.toByteArray();
            objectOutputStream.close();
            byteArrayOutputStream.close();
            return objectByteArray;
        } catch (IOException e) {
            throw new UnsupportedOperationException(
                String.format("无法将%s转换为byte[]", object.getClass().getSimpleName())
            );
        }
    }

    /**
     * 将字节数组转换为可序列化{@code Object}。
     *
     * @param byteArray 序列化得到的{@code byte[]}。
     * @return 转换结果。
     * @throws UnsupportedOperationException 如果无法将{@code byte[]}反序列化为对象。
     */
    public static Object byteArrayToSerializableObject(byte[] byteArray) throws UnsupportedOperationException {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object = objectInputStream.readObject();
            objectInputStream.close();
            byteArrayInputStream.close();
            return object;
        } catch (IOException | ClassNotFoundException e) {
            throw new UnsupportedOperationException("无法将byte[]反序列化为对象");
        }
    }
}

package edu.alibaba.mpc4j.crypto.fhe.serialization;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;

import java.io.*;

/**
 * test SEAL parameter that members are cloneable.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/serialization.cpp#19
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/12/11
 */
class TestCloneable implements SealCloneable {
    /**
     * a
     */
    int a;
    /**
     * b
     */
    int b;
    /**
     * c
     */
    double c;

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(a);
        dataOutputStream.writeInt(b);
        dataOutputStream.writeDouble(c);
        dataOutputStream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        a = dataInputStream.readInt();
        b = dataInputStream.readInt();
        c = dataInputStream.readDouble();
        dataInputStream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        return unsafeLoad(context, inputStream);
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        unsafeLoad(context, in);
    }
}

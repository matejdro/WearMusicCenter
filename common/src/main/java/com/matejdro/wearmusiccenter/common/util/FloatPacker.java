package com.matejdro.wearmusiccenter.common.util;

import java.nio.ByteBuffer;

public class FloatPacker {
    public static float unpackFloat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }

    public static byte[] packFloat(float number) {
        return ByteBuffer.allocate(4).putFloat(number).array();
    }
}

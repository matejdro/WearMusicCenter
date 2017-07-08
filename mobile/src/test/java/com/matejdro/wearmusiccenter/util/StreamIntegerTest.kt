package com.matejdro.wearmusiccenter.util

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class StreamIntegerTest {
    @Test
    fun testReadingInteger() {
        val arrayStream = ByteArrayOutputStream()
        val dataStream = DataOutputStream(arrayStream)
        dataStream.writeInt(1234)
        dataStream.writeInt(-4321)
        dataStream.writeInt(0)
        dataStream.writeInt(Int.MAX_VALUE)
        dataStream.writeInt(Int.MIN_VALUE)

        val array = arrayStream.toByteArray()
        val inputStream = ByteArrayInputStream(array)

        assertEquals(1234, inputStream.readInt())
        assertEquals(-4321, inputStream.readInt())
        assertEquals(0, inputStream.readInt())
        assertEquals(Int.MAX_VALUE, inputStream.readInt())
        assertEquals(Int.MIN_VALUE, inputStream.readInt())
    }

    @Test
    fun testWritingInteger() {
        val arrayStream = ByteArrayOutputStream()
        arrayStream.writeInt(1234)
        arrayStream.writeInt(-4321)
        arrayStream.writeInt(0)
        arrayStream.writeInt(Int.MAX_VALUE)
        arrayStream.writeInt(Int.MIN_VALUE)

        val array = arrayStream.toByteArray()
        val inputStream = ByteArrayInputStream(array)
        val dataStream = DataInputStream(inputStream)

        assertEquals(1234, dataStream.readInt())
        assertEquals(-4321, dataStream.readInt())
        assertEquals(0, dataStream.readInt())
        assertEquals(Int.MAX_VALUE, dataStream.readInt())
        assertEquals(Int.MIN_VALUE, dataStream.readInt())
    }
}
package com.matejdro.wearmusiccenter.util

import java.io.InputStream
import java.io.OutputStream

fun InputStream.readInt() : Int {
    var number = 0

    number = number or (read() shl 24)
    number = number or (read() shl 16)
    number = number or (read() shl 8)
    number = number or read()

    return number
}

fun OutputStream.writeInt(value: Int) {
    write(value.ushr(24) and 0xFF)
    write(value.ushr(16) and 0xFF)
    write(value.ushr(8) and 0xFF)
    write(value.ushr(0) and 0xFF)
}
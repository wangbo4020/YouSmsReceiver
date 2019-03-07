package com.example.smssend.utils

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.messageStack() = StringWriter().let {
    printStackTrace(PrintWriter(it, true))
    val str = it.buffer.toString()
    it.close()
    str
}
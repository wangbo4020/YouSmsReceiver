package com.example.smssend.utils

import java.io.PrintWriter
import java.io.StringWriter

val Throwable.messageStack
    get() = StringWriter().let {
        printStackTrace(PrintWriter(it, true))
        val str = it.buffer.toString()
        it.close()
        str
    }
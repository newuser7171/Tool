package com.jev.toolkit

/** Embed in an Android application that you own or have permission to instrument. */
object JevBridge {
    init { System.loadLibrary("jevtool") }
    external fun inspect(): String
    external fun explore(filter: String): String
    external fun variables(): String
    external fun setFloat(name: String, value: Float): Boolean
    external fun setTyped(name: String, value: String): Boolean
}

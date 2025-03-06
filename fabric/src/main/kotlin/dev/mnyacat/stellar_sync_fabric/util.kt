package dev.mnyacat.stellar_sync_fabric

import com.mojang.brigadier.StringReader
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.StringNbtReader

/// This function is adapted from the MySQL_PlayerdataSync-4-Fabric repository.
/// Source: https://github.com/pugur523/MySQL_PlayerdataSync-4-Fabric/blob/main/src/main/java/com/pugur/playerdata/MySQLPlayerdataSync.java#L250
/*!
* The MIT License (MIT)
*
* Copyright (c) 2024 pugur
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/
fun parseNbtString(nbtString: String): NbtList {
    val reader = StringReader(nbtString)
    val nbtReader = StringNbtReader(reader)
    val element = nbtReader.parseElement()
    if (element !is NbtList) {
        throw IllegalArgumentException("Parsed NbtElement is not of type NbtList")
    }
    return element
}
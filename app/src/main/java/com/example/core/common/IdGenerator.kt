package com.example.core.common

import java.util.UUID

interface IdGenerator {
    fun newId(): String
    fun newSessionId(): String
}

class UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
    override fun newSessionId(): String = "emg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
}

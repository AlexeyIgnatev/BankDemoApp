package com.esom.bank.common.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorResponseTest {
    @Test
    fun parsesStringMessage() {
        val response = ErrorResponse.fromJson(
            """{"message":"Не существует такого номера телефона","statusCode":400}"""
        )

        assertEquals("Не существует такого номера телефона", response?.message)
        assertEquals(400, response?.statusCode)
    }

    @Test
    fun parsesValidationMessageArray() {
        val response = ErrorResponse.fromJson(
            """{"message":["Укажите сумму","Выберите валюту"],"statusCode":400}"""
        )

        assertEquals("Укажите сумму. Выберите валюту", response?.message)
        assertEquals(400, response?.statusCode)
    }
}

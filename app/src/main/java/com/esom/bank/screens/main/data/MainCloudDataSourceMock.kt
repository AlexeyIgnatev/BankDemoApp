package com.esom.bank.screens.main.data

import com.esom.bank.R
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.screens.chat.dto.SupportDto
import com.esom.bank.screens.chat.enums.SupportRole
import com.esom.bank.screens.history.dto.ReceiptResponseDto
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.dto.FeeDto
import com.esom.bank.screens.main.dto.PaymentFeeDto
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.UserDto
import com.esom.bank.screens.main.dto.WalletDto
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.notification.dto.FinancialReportResponseDto
import com.esom.bank.screens.notification.dto.NotificationDto
import com.esom.bank.screens.swap.dto.ConvertDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject

class MainCloudDataSourceMock @Inject constructor(): MainCloudDataSource {

    override fun getUserInfo(): Flow<ApiResponse<UserDto>> = flow {
        emit(
            ApiResponse.Success(
                UserDto(
                    id = 21028,
                    firstName = "Мадина",
                    middleName = "Салморбековна",
                    lastName = "Байбосунова",
                    phone = "+996 555 687878",
                    email = "madina.b@fkb.kg",
                    privateKey = null,
                    wallets = listOf(
                        WalletDto(
                            currency = CurrencyEnum.SOM,
                            address = "+996 555 687878",
                            balance = 1010.62,
                            buyRate = 1.0,
                            sellRate = 1.0
                        ),
                        WalletDto(
                            currency = CurrencyEnum.ESOM,
                            address = "esom_wallet_address_12345",
                            balance = 500.0,
                            buyRate = 1.05,
                            sellRate = 0.95
                        ),
                        WalletDto(
                            currency = CurrencyEnum.USDT_TRC20,
                            address = "TJkTgPifKq1Q9crT9zNCy5dbcXrd71vvof",
                            balance = 1000.0,
                            buyRate = 88.5,
                            sellRate = 87.2
                        )
                    )
                ),
                code = 200
            )
        )
    }

    override fun getSettings(): Flow<ApiResponse<FeeDto>> = flow {
        emit(
            ApiResponse.Success(
                FeeDto(
                    id = 1,
                    esomPerUsd = 1.0,
                    esomSomConversionFeePct = "5.5",
                    esomSomConversionFeeMin = "0"
                ),
                200
            )
        )
    }

    override fun getFees(): Flow<ApiResponse<List<PaymentFeeDto>>> = flow {
        emit(
            ApiResponse.Success(
                listOf(
                    PaymentFeeDto(
                        operation = "WALLET_TRANSFER_SOM",
                        percentFee = "0",
                        fixedFee = "0"
                    ),
                    PaymentFeeDto(
                        operation = "WALLET_TRANSFER_ESOM",
                        percentFee = "0",
                        fixedFee = "0"
                    ),
                    PaymentFeeDto(
                        operation = "WALLET_TRANSFER_USDT_TRC20",
                        percentFee = "1.5",
                        fixedFee = "0"
                    ),
                    PaymentFeeDto(
                        operation = "CONVERT_SOM_TO_USDT_TRC20",
                        percentFee = "0.2",
                        fixedFee = "0"
                    ),
                    PaymentFeeDto(
                        operation = "CONVERT_USDT_TRC20_TO_SOM",
                        percentFee = "0.2",
                        fixedFee = "0"
                    ),
                    PaymentFeeDto(
                        operation = "CONVERT_ESOM_TO_USDT_TRC20",
                        percentFee = "0.2",
                        fixedFee = "0"
                    ),
                    PaymentFeeDto(
                        operation = "CONVERT_USDT_TRC20_TO_ESOM",
                        percentFee = "0.2",
                        fixedFee = "0"
                    )
                ),
                200
            )
        )
    }

    override fun convert(convert: ConvertDto): Flow<ApiResponse<StatusDto>> = flow {
        emit(ApiResponse.Success(StatusDto("success", transactionId = 1L), 200))
    }

    override fun fiatToCrypto(amount: Double): Flow<ApiResponse<StatusDto>> = flow {
        emit(ApiResponse.Success(StatusDto("success", transactionId = 2L), code = 200))
    }

    override fun cryptoToFiat(amount: Double): Flow<ApiResponse<StatusDto>> = flow {
        emit(ApiResponse.Success(StatusDto("success", transactionId = 3L), code = 200))
    }

    override fun transfer(
        amount: Double,
        phone: String,
        address: String?,
        currencyEnum: CurrencyEnum
    ): Flow<ApiResponse<StatusDto>> = flow {
        emit(ApiResponse.Error(R.string.wallet_ban, null, null))
    }

    override fun history(
        currencyEnum: List<CurrencyEnum>?,
        fromTime: Long,
        toTime: Long,
        take: Int,
        skip: Int
    ): Flow<ApiResponse<List<TransactionDto>>> = flow {

        val allTransactions = List(40) { index ->
            val currencies = listOf(
                CurrencyEnum.SOM,
                CurrencyEnum.ESOM,
                CurrencyEnum.USDT_TRC20
            )
            val randomCurrency = currencies[index % currencies.size]
            val randomType = TransactionEnum.values()[index % TransactionEnum.values().size]
            val createdAt = System.currentTimeMillis() - index * 60_000L
            TransactionDto(
                transactionId = 10_000L + index,
                currencyEnum = randomCurrency,
                type = randomType,
                conversionSide = if (randomType == TransactionEnum.CONVERSION) {
                    if (index % 2 == 0) ConversionSide.IN else ConversionSide.OUT
                } else null,
                amount = (10..1000).random().toDouble(),
                successful = true,
                createdAt = createdAt
            )
        }

        val filtered = allTransactions.filter { tx ->
            val matchCurrency = currencyEnum?.let { it.contains(tx.currencyEnum) } ?: true
            val matchTime = if (take == 5) true else tx.createdAt in fromTime..toTime
            matchCurrency && matchTime
        }

        val result = filtered.drop(skip).take(take)

        emit(ApiResponse.Success(result, code = 200))
    }

    override fun receipt(
        transactionId: Long,
        conversionSide: ConversionSide?
    ): Flow<ApiResponse<ReceiptResponseDto>> = flow {
        emit(
            ApiResponse.Success(
                ReceiptResponseDto(
                    successful = true,
                    amount = 260.0,
                    type = "TRANSFER",
                    currency = "SOM",
                    createdAt = System.currentTimeMillis(),
                    fee = if (conversionSide != null) 5.5 else 0.0,
                    accountDetails = "996557501281",
                    recipientFullName = "Мирлан Т. у.",
                    paidFromAccount = "****1234",
                    conversionSide = conversionSide,
                    absAccount = "ABS-40602810200000001234",
                    absFromAccount = "ABS-40702810900000005678",
                    absToAccount = "ABS-40817810900000004321",
                    receiptNumber = "TX-$transactionId-${System.currentTimeMillis()}"
                ),
                code = 200
            )
        )
    }

    override fun getMessages(): Flow<ApiResponse<List<SupportDto>>> = flow {
        val messages = listOf(
            SupportDto(
                id = 1,
                ticketId = 45,
                text = "Здравствуйте! У меня вопрос по переводу средств.",
                role = SupportRole.USER.name,
                createdAt = getTimestamp(2, 15) // 15 дней назад
            ),
            SupportDto(
                id = 2,
                ticketId = 45,
                text = "Добрый день! Чем могу помочь? Опишите, пожалуйста, вашу проблему подробнее.",
                role = SupportRole.ASSISTANT.name,
                createdAt = getTimestamp(2, 15, 5) // 15 дней назад + 5 минут
            ),
            SupportDto(
                id = 3,
                ticketId = 45,
                text = "Я пытался перевести USDT на другой кошелек, но транзакция висит в статусе 'В обработке' уже 2 часа.",
                role = SupportRole.USER.name,
                createdAt = getTimestamp(2, 15, 10)
            ),
            SupportDto(
                id = 4,
                ticketId = 45,
                text = "Проверил вашу транзакцию. Это нормально для сети TRC20, иногда требуется до 4 часов. Если статус не изменится через 2 часа, напишите мне.",
                role = SupportRole.ADMIN.name,
                createdAt = getTimestamp(2, 15, 15)
            ),
            SupportDto(
                id = 5,
                ticketId = 45,
                text = "Спасибо! Транзакция прошла успешно.",
                role = SupportRole.USER.name,
                createdAt = getTimestamp(1, 5) // 5 дней назад
            ),
            SupportDto(
                id = 6,
                ticketId = 46,
                text = "Как узнать курс обмена ESOM на USDT?",
                role = SupportRole.USER.name,
                createdAt = getTimestamp(0, 1) // 1 день назад
            ),
            SupportDto(
                id = 7,
                ticketId = 46,
                text = "Текущий курс ESOM/USDT вы можете посмотреть в разделе 'Кошельки'. На сегодня это 1 ESOM = 0.85 USDT.",
                role = SupportRole.ADMIN.name,
                createdAt = getTimestamp(0, 1, 30)
            )
        )
        emit(ApiResponse.Success(messages, code = 200))
    }

    override fun sendMessage(text: String): Flow<ApiResponse<SupportDto>> = flow {
        val newMessage = SupportDto(
            id = (8..1000).random(),
            ticketId = 46,
            text = text,
            role = SupportRole.USER.name,
            createdAt = System.currentTimeMillis()
        )
        emit(ApiResponse.Success(newMessage, code = 200))
    }

    override fun getNotifications(): Flow<ApiResponse<List<NotificationDto>>> = flow {
        val notifications = listOf(
            NotificationDto(
                id = 1,
                title = "Пополнение счета",
                text = "Ваш кошелек USDT пополнен на 150.0 USDT",
                createdAt = getTimestamp(2, 20) // 20 дней назад
            ),
            NotificationDto(
                id = 2,
                title = "Курс обновлен",
                text = "Обновлены курсы валют. Проверьте новые ставки в приложении",
                createdAt = getTimestamp(2, 18)
            ),
            NotificationDto(
                id = 3,
                title = "Перевод выполнен",
                text = "Перевод 5000 KGS на счет +996 555 123456 выполнен успешно",
                createdAt = getTimestamp(2, 10)
            ),
            NotificationDto(
                id = 4,
                title = "Технические работы",
                text = "28 ноября с 03:00 до 05:00 планируются технические работы",
                createdAt = getTimestamp(1, 25) // 25 дней назад (в прошлом месяце)
            ),
            NotificationDto(
                id = 5,
                title = "Новая функция",
                text = "Добавлена возможность быстрого перевода по QR-коду",
                createdAt = getTimestamp(1, 15)
            ),
            NotificationDto(
                id = 6,
                title = "Безопасность",
                text = "Рекомендуем включить двухфакторную аутентификацию",
                createdAt = getTimestamp(1, 8)
            ),
            NotificationDto(
                id = 7,
                title = "Обновление приложения",
                text = "Доступно новое обновление приложения. Установите для улучшенной работы",
                createdAt = getTimestamp(0, 3) // 3 дня назад
            ),
            NotificationDto(
                id = 8,
                title = "Кэшбэк",
                text = "За все переводы в этом месяце вы получаете 1% кэшбэка в ESOM",
                createdAt = getTimestamp(0, 1)
            ),
            NotificationDto(
                id = 9,
                title = "Поддержка 24/7",
                text = "Техническая поддержка теперь доступна круглосуточно",
                createdAt = getTimestamp(0, 0, 2) // 2 часа назад
            )
        )
        emit(ApiResponse.Success(notifications, code = 200))
    }

    override fun sendFinancialReport(
        email: String?,
        fromTime: Long?,
        toTime: Long?
    ): Flow<ApiResponse<FinancialReportResponseDto>> = flow {
        emit(
            ApiResponse.Success(
                FinancialReportResponseDto(successful = true),
                code = 200
            )
        )
    }

    override fun sendFcmToken(token: String): Flow<ApiResponse<Unit>> = flow {
        emit(ApiResponse.Success(Unit, code = 200))
    }

    override fun updatePushSettings(pushEnabled: Boolean): Flow<ApiResponse<Unit>> = flow {
        emit(ApiResponse.Success(Unit, code = 200))
    }

    private fun getTimestamp(monthsAgo: Int, daysAgo: Int, minutesAgo: Int = 0): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -monthsAgo)
        calendar.add(Calendar.DAY_OF_MONTH, -daysAgo)
        calendar.add(Calendar.MINUTE, -minutesAgo)
        calendar.set(Calendar.HOUR_OF_DAY, (10..18).random())
        calendar.set(Calendar.MINUTE, (0..59).random())
        calendar.set(Calendar.SECOND, (0..59).random())
        return calendar.timeInMillis
    }
}

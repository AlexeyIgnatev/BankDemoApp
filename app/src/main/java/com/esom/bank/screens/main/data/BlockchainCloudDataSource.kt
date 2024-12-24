package com.esom.bank.screens.main.data

import com.esom.bank.BuildConfig
import com.esom.bank.R
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import java.math.BigInteger
import javax.inject.Inject

interface BlockchainCloudDataSource {
    fun transferFromFiat(
        account: String,
        amount: BigInteger,
        privateKey: String
    ): Flow<ApiResponse<Unit>>

    fun transferToFiat(
        amount: BigInteger,
        privateKey: String
    ): Flow<ApiResponse<Unit>>

    fun transferToUser(
        account: String,
        amount: BigInteger,
        privateKey: String
    ): Flow<ApiResponse<Unit>>

    fun getBalance(account: String): Flow<ApiResponse<BigInteger>>
}

class BlockchainCloudDataSourceImpl @Inject constructor() : BlockchainCloudDataSource {
    override fun transferFromFiat(
        account: String,
        amount: BigInteger,
        privateKey: String
    ): Flow<ApiResponse<Unit>> = flow<ApiResponse<Unit>> {
        val web3j = Web3j.build(HttpService(BuildConfig.RPC_URL))

        try {
            // Create credentials from private key
            val credentials = Credentials.create(privateKey)

            // Get the current nonce for the sender's account
            val nonce = web3j.ethGetTransactionCount(
                credentials.address,
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
            ).send().transactionCount

            // Encode the function call
            val encodedFunction = org.web3j.abi.FunctionEncoder.encode(
                org.web3j.abi.datatypes.Function(
                    "transferFromFiat",
                    listOf(
                        Address(account),
                        Uint256(amount)
                    ),
                    emptyList()
                )
            )

            // Create the raw transaction
            val gasPrice = DefaultGasProvider.GAS_PRICE
            val gasLimit = DefaultGasProvider.GAS_LIMIT

            val rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                BuildConfig.TOKEN_ADDRESS,
                encodedFunction
            )

            // Sign the transaction
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val signedTransactionData = Numeric.toHexString(signedMessage)

            // Send the transaction
            val transactionResponse = web3j.ethSendRawTransaction(signedTransactionData).send()

            if (transactionResponse.error != null) {
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        ErrorResponse(message = transactionResponse.error.message)
                    )
                )
            } else {
                emit(ApiResponse.Success(Unit, 200))
            }
        } catch (e: Exception) {
            emit(ApiResponse.Error(R.string.something_went_wrong, ErrorResponse(message = e.message)))
        } finally {
            web3j.shutdown()
        }
    }.flowOn(Dispatchers.IO)

    override fun transferToFiat(
        amount: BigInteger,
        privateKey: String
    ): Flow<ApiResponse<Unit>> = flow<ApiResponse<Unit>> {
        val web3j = Web3j.build(HttpService(BuildConfig.RPC_URL))

        try {
            // Create credentials from private key
            val credentials = Credentials.create(privateKey)

            // Get the current nonce for the sender's account
            val nonce = web3j.ethGetTransactionCount(
                credentials.address,
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
            ).send().transactionCount

            // Encode the function call
            val encodedFunction = org.web3j.abi.FunctionEncoder.encode(
                org.web3j.abi.datatypes.Function(
                    "transferToFiat",
                    listOf(
                        Uint256(amount)
                    ),
                    emptyList()
                )
            )

            // Create the raw transaction
            val gasPrice = DefaultGasProvider.GAS_PRICE
            val gasLimit = DefaultGasProvider.GAS_LIMIT

            val rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                BuildConfig.TOKEN_ADDRESS,
                encodedFunction
            )

            // Sign the transaction
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val signedTransactionData = Numeric.toHexString(signedMessage)

            // Send the transaction
            val transactionResponse = web3j.ethSendRawTransaction(signedTransactionData).send()

            if (transactionResponse.error != null) {
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        ErrorResponse(message = transactionResponse.error.message)
                    )
                )
            } else {
                emit(ApiResponse.Success(Unit, 200))
            }
        } catch (e: Exception) {
            emit(ApiResponse.Error(R.string.something_went_wrong, ErrorResponse(message = e.message)))
        } finally {
            web3j.shutdown()
        }
    }.flowOn(Dispatchers.IO)

    override fun transferToUser(
        account: String,
        amount: BigInteger,
        privateKey: String
    ): Flow<ApiResponse<Unit>> = flow<ApiResponse<Unit>> {
        val web3j = Web3j.build(HttpService(BuildConfig.RPC_URL))

        try {
            // Create credentials from private key
            val credentials = Credentials.create(privateKey)

            // Get the current nonce for the sender's account
            val nonce = web3j.ethGetTransactionCount(
                credentials.address,
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
            ).send().transactionCount

            // Encode the function call
            val encodedFunction = org.web3j.abi.FunctionEncoder.encode(
                org.web3j.abi.datatypes.Function(
                    "transfer",
                    listOf(
                        Address(account),
                        Uint256(amount)
                    ),
                    emptyList()
                )
            )

            // Create the raw transaction
            val gasPrice = DefaultGasProvider.GAS_PRICE
            val gasLimit = DefaultGasProvider.GAS_LIMIT

            val rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                BuildConfig.TOKEN_ADDRESS,
                encodedFunction
            )

            // Sign the transaction
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val signedTransactionData = Numeric.toHexString(signedMessage)

            // Send the transaction
            val transactionResponse = web3j.ethSendRawTransaction(signedTransactionData).send()

            if (transactionResponse.error != null) {
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        ErrorResponse(message = transactionResponse.error.message)
                    )
                )
            } else {
                emit(ApiResponse.Success(Unit, 200))
            }
        } catch (e: Exception) {
            emit(ApiResponse.Error(R.string.something_went_wrong, ErrorResponse(message = e.message)))
        } finally {
            web3j.shutdown()
        }
    }.flowOn(Dispatchers.IO)

    override fun getBalance(account: String): Flow<ApiResponse<BigInteger>> =
        flow<ApiResponse<BigInteger>> {
            val web3j = Web3j.build(HttpService(BuildConfig.RPC_URL))

            try {
                val function = org.web3j.abi.datatypes.Function(
                    "balanceOf",
                    listOf(Address(account)),
                    listOf(object : TypeReference<Uint256>() {})
                )

                // Encode the function call
                val encodedFunction = org.web3j.abi.FunctionEncoder.encode(function)

                // Call the smart contract
                val response = web3j.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        account, // From address (can be null)
                        BuildConfig.TOKEN_ADDRESS, // To address (ERC-20 contract address)
                        encodedFunction // Data to send
                    ),
                    DefaultBlockParameterName.LATEST // Read latest state
                ).send()

                // Decode the result
                val result = response.result
                if (result != null) {
                    val decoded = FunctionReturnDecoder.decode(result, function.outputParameters)
                    if (decoded.isNotEmpty()) {
                        emit(ApiResponse.Success(decoded[0].value as BigInteger, 200))
                    } else {
                        emit(ApiResponse.Error(R.string.something_went_wrong))
                    }
                } else {
                    emit(ApiResponse.Error(R.string.something_went_wrong))
                }
            } catch (e: Exception) {
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        ErrorResponse(message = e.message)
                    )
                )
            } finally {
                web3j.shutdown()
            }
        }.flowOn(Dispatchers.IO)
}
package com.esom.bank.screens.history.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.esom.bank.common.model.UiState
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.main.data.MainRepository
import com.esom.bank.screens.main.enums.CurrencyEnum

class TransactionsPagingSource(
    private val repository: MainRepository,
    private val currencyEnum: List<CurrencyEnum>?,
    private val fromTime: Long,
    private val toTime: Long
) : PagingSource<Int, TransactionModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TransactionModel> {
        val offset = params.key ?: 0
        return try {
            Log.d("PAGING", "Loading transactions with offset: $offset")

            val response = repository.history(
                currencyEnum = currencyEnum,
                fromTime = fromTime,
                toTime = toTime,
                take = params.loadSize,
                skip = offset
            )

            var result: UiState<List<TransactionModel?>>? = null
            var collected = false

            response.collect { state ->
                if (!collected) {
                    result = state
                    collected = true
                }
            }

            when (val finalResult = result) {
                is UiState.Success -> {
                    val transactions = finalResult.data.map { dto ->
                        TransactionModel(
                            currencyEnum = dto?.currencyEnum,
                            type = dto?.type,
                            amount = dto?.amount,
                            successful = dto?.successful,
                            createdAt = dto?.createdAt
                        )

                    }

                    Log.d("PAGING", "Loaded ${transactions.size} transactions")

                    val nextOffset = if (transactions.size < params.loadSize) null else offset + params.loadSize
                    val prevOffset = if (offset == 0) null else offset - params.loadSize

                    LoadResult.Page(
                        data = transactions,
                        prevKey = prevOffset,
                        nextKey = nextOffset
                    )
                }
                is UiState.Error -> {
                    Log.e("PAGING", "Error: ${finalResult.message}")
                    LoadResult.Error(Exception(finalResult.message))
                }
                else -> {
                    Log.d("PAGING", "Empty or loading state")
                    LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                }
            }
        } catch (e: Exception) {
            Log.e("PAGING", "Exception in load: ${e.message}")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TransactionModel>): Int? {
        return null
    }
}
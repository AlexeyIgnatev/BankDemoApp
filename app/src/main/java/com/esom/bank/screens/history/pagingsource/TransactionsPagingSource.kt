package com.esom.bank.screens.history.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
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
                    // The repository has already mapped the complete DTO. Keep that model intact:
                    // sender/recipient/account fields are required to distinguish server-side
                    // INCOME/EXPENSE transfers from other operations.
                    val transactions = finalResult.data.filterNotNull()

                    val nextOffset = if (transactions.size < params.loadSize) null else offset + params.loadSize
                    val prevOffset = if (offset == 0) null else offset - params.loadSize

                    LoadResult.Page(
                        data = transactions,
                        prevKey = prevOffset,
                        nextKey = nextOffset
                    )
                }
                is UiState.Error -> {
                    LoadResult.Error(Exception(finalResult.message))
                }
                else -> {
                    LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TransactionModel>): Int? {
        return null
    }
}

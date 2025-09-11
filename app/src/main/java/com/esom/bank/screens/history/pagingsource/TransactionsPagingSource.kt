package com.esom.bank.screens.history.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.UiState
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.main.data.MainRepository
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlinx.coroutines.flow.first

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
            ).first()

            if (response is UiState.Success) {

                val transactions = response.data.map { dto ->
                    TransactionModel(
                        currencyEnum = dto.currencyEnum,
                        type = dto.type,
                        amount = dto.amount,
                        successful = dto.successful,
                        createdAt = dto.createdAt
                    )
                }

                val nextOffset =
                    if (transactions.size < params.loadSize) null else offset + params.loadSize
                val prevOffset = if (offset == 0) null else offset - params.loadSize

                LoadResult.Page(
                    data = transactions,
                    prevKey = prevOffset,
                    nextKey = nextOffset
                )
            } else {
                Log.e("PAGING", "Repository returned error or empty state")
                LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }
        } catch (e: Exception) {
            Log.e("PAGING", "Exception: ${e.message}")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TransactionModel>): Int? {
        return null
    }
}

package com.esom.bank.screens.history.di

import com.esom.bank.screens.history.data.HistoryLocalDataSource
import com.esom.bank.screens.history.data.HistoryLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {
    @Binds
    abstract fun bindHistoryLocalDataSource(
        historyLocalDataSourceImpl: HistoryLocalDataSourceImpl
    ): HistoryLocalDataSource
}
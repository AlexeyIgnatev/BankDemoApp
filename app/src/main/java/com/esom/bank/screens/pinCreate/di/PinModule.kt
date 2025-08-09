package com.esom.bank.screens.pinCreate.di

import com.esom.bank.screens.pinCreate.data.PinLocalDataSource
import com.esom.bank.screens.pinCreate.data.PinLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PinModule {
    @Binds
    abstract fun bindPinLocalDataSource(
        pinLocalDataSourceImpl: PinLocalDataSourceImpl
    ): PinLocalDataSource
}
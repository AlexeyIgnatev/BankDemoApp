package com.esom.bank.screens.main.di

import com.esom.bank.screens.main.data.BlockchainCloudDataSource
import com.esom.bank.screens.main.data.BlockchainCloudDataSourceImpl
import com.esom.bank.screens.main.data.MainCloudDataSource
import com.esom.bank.screens.main.data.MainCloudDataSourceImpl
import com.esom.bank.screens.main.data.MainRepository
import com.esom.bank.screens.main.data.MainRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MainModule {
    @Binds
    abstract fun bindMainCloudDataSource(
        mainCloudDataSourceImpl: MainCloudDataSourceImpl
    ): MainCloudDataSource

    @Binds
    abstract fun bindBlockchainCloudDataSource(
        blockchainCloudDataSourceImpl: BlockchainCloudDataSourceImpl
    ): BlockchainCloudDataSource

    @Binds
    abstract fun bindMainRepository(
        mainRepositoryImpl: MainRepositoryImpl
    ): MainRepository

}
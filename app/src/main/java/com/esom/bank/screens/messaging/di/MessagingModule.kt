package com.esom.bank.screens.messaging.di

import com.esom.bank.screens.messaging.data.MessagingLocalDataSource
import com.esom.bank.screens.messaging.data.MessagingLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagingModule {
    @Binds
    abstract fun bindMessagingLocalDataSource(
        messagingLocalDataSourceImpl: MessagingLocalDataSourceImpl
    ): MessagingLocalDataSource
}

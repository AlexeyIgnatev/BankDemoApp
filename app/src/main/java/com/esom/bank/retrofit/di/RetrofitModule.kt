package com.esom.bank.retrofit.di

import com.esom.bank.BuildConfig
import com.esom.bank.retrofit.api.ServerApi
import com.esom.bank.retrofit.interceptor.AuthInterceptor
import com.esom.bank.retrofit.interceptor.InternetInterceptor
import com.esom.bank.retrofit.service.InternetConnectionService
import com.esom.bank.retrofit.service.InternetConnectionServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    private const val TIMEOUT_FOR_REQUEST = 120L

    @Provides
    fun createServerApi(
        httpClient: OkHttpClient
    ): ServerApi {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BuildConfig.BACKEND_URL)
            .client(httpClient)
            .build()
            .create(ServerApi::class.java)
    }


    @Provides
    fun createHttpClient(
        internetInterceptor: InternetInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val httpClientBuilder = OkHttpClient.Builder()

        httpClientBuilder.readTimeout(TIMEOUT_FOR_REQUEST, TimeUnit.SECONDS)
        httpClientBuilder.writeTimeout(TIMEOUT_FOR_REQUEST, TimeUnit.SECONDS)

        httpClientBuilder.addInterceptor(internetInterceptor)
        httpClientBuilder.addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.redactHeader("Authorization")
            logging.redactHeader("Cookie")
            logging.level = HttpLoggingInterceptor.Level.HEADERS
            httpClientBuilder.addInterceptor(logging)
        }

        return httpClientBuilder.build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class InternetConnectionServiceModule {
    @Binds
    abstract fun bindInternetConnectionService(
        internetConnectionServiceImpl: InternetConnectionServiceImpl
    ): InternetConnectionService
}



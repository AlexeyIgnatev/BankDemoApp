package com.esom.bank.retrofit.di

import com.esom.bank.BuildConfig
import com.esom.bank.retrofit.api.ServerApi
import com.esom.bank.retrofit.interceptor.InternetInterceptor
import com.esom.bank.retrofit.service.InternetConnectionService
import com.esom.bank.retrofit.service.InternetConnectionServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
            .baseUrl(BuildConfig.RPC_URL)
            .client(httpClient)
            .build()
            .create(ServerApi::class.java)
    }


    @Provides
    fun createHttpClient(
        internetInterceptor: InternetInterceptor
    ): OkHttpClient {
        val httpClientBuilder = OkHttpClient.Builder()

        httpClientBuilder.readTimeout(TIMEOUT_FOR_REQUEST, TimeUnit.SECONDS)
        httpClientBuilder.writeTimeout(TIMEOUT_FOR_REQUEST, TimeUnit.SECONDS)

        httpClientBuilder.addInterceptor(internetInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            httpClientBuilder.addInterceptor(logging)
        }

        return httpClientBuilder.ignoreAllSSLErrors().build()
    }

    private fun createNoLogsHttpClient(
        internetInterceptor: InternetInterceptor
    ): OkHttpClient {
        val httpClientBuilder = OkHttpClient.Builder()
        httpClientBuilder.readTimeout(TIMEOUT_FOR_REQUEST, TimeUnit.SECONDS)
        httpClientBuilder.writeTimeout(TIMEOUT_FOR_REQUEST, TimeUnit.SECONDS)
        httpClientBuilder.addInterceptor(internetInterceptor)
        return httpClientBuilder.ignoreAllSSLErrors().build()
    }

    private fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
        val naiveTrustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) =
                Unit

            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) =
                Unit
        }

        val insecureSocketFactory = SSLContext.getInstance("TLSv1.2").apply {
            val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
            init(null, trustAllCerts, SecureRandom())
        }.socketFactory

        sslSocketFactory(insecureSocketFactory, naiveTrustManager)
        hostnameVerifier(HostnameVerifier { _, _ -> true })
        connectionSpecs(
            listOf(
                ConnectionSpec.CLEARTEXT,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.RESTRICTED_TLS
            )
        )
        return this
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



package com.example.sp500auth.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.BuildConfig
import com.example.sp500auth.data.api.SessionAuthenticator
import com.example.sp500auth.data.api.SessionRefreshApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor, tokenManager: TokenManager): OkHttpClient {
        val refreshApi = Retrofit.Builder().baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(SessionRefreshApi::class.java)
        return OkHttpClient.Builder()
            .authenticator(SessionAuthenticator(tokenManager, refreshApi))
            .addInterceptor { chain ->
                val request = chain.request()
                val publicPaths = listOf("auth/token/", "auth/register/", "auth/refresh/", "auth/password/reset/", "auth/password/reset/confirm/")
                val builder = request.newBuilder()
                tokenManager.getAccessToken()?.let { token ->
                    if (publicPaths.none { request.url.encodedPath.endsWith(it) }) {
                        builder.header("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // La inferencia local puede tardar más que una consulta de datos.
                val effective = if (chain.request().url.encodedPath.endsWith("/analysis/"))
                    chain.withReadTimeout(160, TimeUnit.SECONDS) else chain
                effective.proceed(chain.request())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): com.example.sp500auth.data.api.AuthApiService {
        return retrofit.create(com.example.sp500auth.data.api.AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}

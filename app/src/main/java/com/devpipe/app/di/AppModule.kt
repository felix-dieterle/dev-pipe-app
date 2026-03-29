package com.devpipe.app.di

import com.devpipe.app.data.api.DevPipeApi
import com.devpipe.app.data.api.PhpDiscoveryApi
import com.devpipe.app.data.repository.DevPipeRepository
import com.devpipe.app.data.storage.PreferencesManager
import com.devpipe.app.data.storage.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor =
        AuthInterceptor(tokenManager)

    @Provides
    @Singleton
    @Named("devpipe")
    fun provideOkHttpClient(
        dynamicUrlInterceptor: DynamicUrlInterceptor,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideDevPipeApi(@Named("devpipe") client: OkHttpClient): DevPipeApi =
        Retrofit.Builder()
            .baseUrl(PreferencesManager.DEFAULT_BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DevPipeApi::class.java)

    @Provides
    @Singleton
    @Named("discovery")
    fun provideDiscoveryOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun providePhpDiscoveryApi(
        @Named("discovery") client: OkHttpClient
    ): PhpDiscoveryApi {
        // Use a placeholder base URL; the actual discovery URL is set by the user in Settings
        // and passed as a full URL via the query param token. We build the Retrofit instance
        // pointing at a dummy host; callers supply the full PHP endpoint URL via phpDiscoveryUrl.
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhpDiscoveryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDevPipeRepository(
        api: DevPipeApi,
        phpApi: PhpDiscoveryApi
    ): DevPipeRepository = DevPipeRepository(api, phpApi)
}

class AuthInterceptor(private val tokenManager: TokenManager) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val token = tokenManager.getToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

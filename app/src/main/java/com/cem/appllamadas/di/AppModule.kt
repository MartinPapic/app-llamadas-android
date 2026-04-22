package com.cem.appllamadas.di

import android.content.Context
import androidx.room.Room
import com.cem.appllamadas.data.local.AppDatabase
import com.cem.appllamadas.data.local.SessionManager
import com.cem.appllamadas.data.remote.ApiService
import com.cem.appllamadas.data.remote.AuthApiService
import com.cem.appllamadas.domain.repository.ContactoRepository
import com.cem.appllamadas.domain.repository.LlamadaRepository
import com.cem.appllamadas.domain.repository.ProyectoRepository
import com.cem.appllamadas.domain.usecase.ObtenerSiguienteContactoUseCase
import com.cem.appllamadas.domain.usecase.RegistrarLlamadaUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://app-llamadas-backend-production.up.railway.app"

    /** OkHttpClient con interceptor JWT — agrega Authorization: Bearer <token> a cada request */
    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = sessionManager.getAccessToken()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
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
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_llamadas_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideContactoDao(appDatabase: AppDatabase): com.cem.appllamadas.data.local.dao.ContactoDao {
        return appDatabase.contactoDao
    }

    @Provides
    fun provideLlamadaDao(appDatabase: AppDatabase): com.cem.appllamadas.data.local.dao.LlamadaDao {
        return appDatabase.llamadaDao
    }

    @Provides
    fun provideEncuestaDao(appDatabase: AppDatabase): com.cem.appllamadas.data.local.dao.EncuestaDao {
        return appDatabase.encuestaDao
    }

    @Provides
    fun provideProyectoDao(appDatabase: AppDatabase): com.cem.appllamadas.data.local.dao.ProyectoDao {
        return appDatabase.proyectoDao
    }

    @Provides
    @Singleton
    fun provideContactoRepository(
        dao: com.cem.appllamadas.data.local.dao.ContactoDao,
        apiService: ApiService
    ): ContactoRepository {
        return com.cem.appllamadas.data.repository.ContactoRepositoryImpl(dao, apiService)
    }

    @Provides
    @Singleton
    fun provideLlamadaRepository(
        dao: com.cem.appllamadas.data.local.dao.LlamadaDao,
        contactoDao: com.cem.appllamadas.data.local.dao.ContactoDao,
        apiService: ApiService
    ): LlamadaRepository {
        return com.cem.appllamadas.data.repository.LlamadaRepositoryImpl(dao, contactoDao, apiService)
    }

    @Provides
    @Singleton
    fun provideEncuestaRepository(
        dao: com.cem.appllamadas.data.local.dao.EncuestaDao,
        apiService: ApiService
    ): com.cem.appllamadas.domain.repository.EncuestaRepository {
        return com.cem.appllamadas.data.repository.EncuestaRepositoryImpl(dao, apiService)
    }

    @Provides
    @Singleton
    fun provideProyectoRepository(
        dao: com.cem.appllamadas.data.local.dao.ProyectoDao,
        apiService: ApiService
    ): ProyectoRepository {
        return com.cem.appllamadas.data.repository.ProyectoRepositoryImpl(dao, apiService)
    }

    @Provides
    @Singleton
    fun provideObtenerSiguienteContactoUseCase(contactoRepository: ContactoRepository): ObtenerSiguienteContactoUseCase {
        return ObtenerSiguienteContactoUseCase(contactoRepository)
    }

    @Provides
    @Singleton
    fun provideRegistrarLlamadaUseCase(
        llamadaRepository: LlamadaRepository,
        contactoRepository: ContactoRepository
    ): RegistrarLlamadaUseCase {
        return RegistrarLlamadaUseCase(llamadaRepository, contactoRepository)
    }
}

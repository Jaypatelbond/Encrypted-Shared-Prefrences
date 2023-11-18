package com.encryptedsharedprefrencesdemo.di

import PreferenceManager
import android.content.Context
import com.encryptedsharedprefrencesdemo.data.local.EncryptedPreferences
import com.encryptedsharedprefrencesdemo.data.local.Preference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    // Provides an instance of EncPref for encrypted shared preferences
    @Singleton
    @Provides
    fun provideAppEncSharedPref(@ApplicationContext context: Context): EncryptedPreferences {
        return EncryptedPreferences.Builder()
            .setPrefName(context.packageName) // Set the name for the shared preferences
            .setContext(context) // Set the context
            //.setDebuggable(BuildConfig.DEBUG) // Set the debuggable flag based on the build configuration
            .build() // Build the EncPref instance
    }

    // Provides an instance of Preference using EncPref and the application context
    @Singleton
    @Provides
    fun provideAppPreference(encPref: EncryptedPreferences, @ApplicationContext context: Context): Preference {
        return PreferenceManager(encPref)
    }
}
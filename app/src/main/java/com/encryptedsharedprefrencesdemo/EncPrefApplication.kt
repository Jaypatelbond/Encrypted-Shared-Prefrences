package com.encryptedsharedprefrencesdemo

import android.app.Application
import com.encryptedsharedprefrencesdemo.data.local.Preference
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EncPrefApplication : Application() {

    @Inject
    lateinit var preference: Preference

}
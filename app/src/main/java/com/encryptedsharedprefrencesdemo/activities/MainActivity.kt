package com.encryptedsharedprefrencesdemo.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.encryptedsharedprefrencesdemo.R
import com.encryptedsharedprefrencesdemo.data.local.Preference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var preference: Preference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        writeSharedPreferences(this)

        preference.setUserId("12345")
        preference.setLogin(false)

    }

    private fun writeSharedPreferences(context: Context) {
        // Initializing SharedPreferences object with name "my_preferences"
        val sharedPreferences = context.getSharedPreferences("my_preferences", MODE_PRIVATE)

        // Creating an editor to modify SharedPreferences
        val editor = sharedPreferences.edit()

        // Adding values to SharedPreferences
        editor.putString("my_string", "hello world!")
        editor.putInt("my_integer", 124243)
        editor.putBoolean("my_boolean", true)

        // Committing the changes
        editor.apply()
    }
}
package com.encryptedsharedprefrencesdemo.data.local

interface Preference {

    fun setLogin(isLogin: Boolean)

    fun isLogin(): Boolean

    fun setUserId(userId: String)

    fun getUserId(): String


}

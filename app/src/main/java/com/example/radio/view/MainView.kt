package com.example.radio.view

interface MainView{
    fun updatePlayButton(text: String)
    fun showStatusMessage(message: String)
    fun showLoginSuccess()
    fun showLoginError(message: String)
    fun navigateToComentsFragment()
    fun hideLoginButton()
    fun showLoginButton()
    fun showLogoutButton()
    fun hideLogoutButton()
    fun removeCommentsFragment()
    fun showGoodbyeMessage(name: String)

}
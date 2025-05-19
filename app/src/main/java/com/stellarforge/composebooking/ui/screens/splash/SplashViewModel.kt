package com.stellarforge.composebooking.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    private var listenerAttached = false
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        Timber.d("SplashViewModel initialized.")
        // Listener'ı sadece bir kez attach et
        if (!listenerAttached) {
            setupAndAttachAuthStateListener()
            listenerAttached = true
        }
    }

    private fun setupAndAttachAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            // Bu listener ana thread'de çağrılabilir, ancak UI güncellemeleri için
            // viewModelScope kullanmak iyi bir pratiktir, özellikle token alma gibi
            // asenkron bir işlem yapacaksak.
            viewModelScope.launch {
                // Yönlendirme sadece _startDestination null ise yapılsın
                // ve listener daha önce işlem yapmadıysa (bu kontrol zaten listenerAttached ile sağlanıyor olabilir ama ek güvenlik)
                if (_startDestination.value == null) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        Timber.d("AuthStateListener: User is signed in (UID: ${firebaseUser.uid}). Attempting to get ID token before navigation.")
                        // Token'ı almayı dene (bu, backend ile senkronizasyonu zorlayabilir)
                        // `false` parametresi, token süresi dolmamışsa mevcut token'ı kullanır,
                        // dolmuşsa veya yoksa yenisini alır. Bu işlem ağ çağrısı yapabilir.
                        firebaseUser.getIdToken(true)
                            .addOnCompleteListener { task ->
                                // addOnCompleteListener farklı bir thread'de çalışabilir,
                                // StateFlow güncellemesini ana thread'e (veya viewModelScope'a) almak güvenlidir.
                                viewModelScope.launch {
                                    if (task.isSuccessful && task.result?.token != null) {
                                        Timber.i("SplashViewModel: ID Token retrieved successfully. Navigating to ServiceList.")
                                        _startDestination.value = ScreenRoutes.ServiceList.route
                                    } else {
                                        Timber.e(task.exception, "SplashViewModel: Failed to get ID token or token is null after sign-in. Navigating to Login as fallback.")
                                        // Token alınamazsa (bir sorun var demektir), yine de Login'e yönlendir.
                                        _startDestination.value = ScreenRoutes.Login.route
                                    }
                                }
                            }
                    } else {
                        Timber.d("AuthStateListener: User is signed out. Navigating to Login.")
                        _startDestination.value = ScreenRoutes.Login.route
                    }
                } else {
                    Timber.d("AuthStateListener: Start destination already set to ${_startDestination.value}, ignoring further auth state changes in Splash.")
                }
            }
        }
        firebaseAuth.addAuthStateListener(authStateListener!!)
        Timber.d("AuthStateListener attached.")
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let {
            firebaseAuth.removeAuthStateListener(it)
            Timber.d("SplashViewModel cleared. AuthStateListener removed.")
        }
        listenerAttached = false
        authStateListener = null
    }
}
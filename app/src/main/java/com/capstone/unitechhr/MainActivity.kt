package com.capstone.unitechhr

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.capstone.unitechhr.fragments.ForgotPasswordFragment
import com.capstone.unitechhr.fragments.LoginFragment
import com.capstone.unitechhr.fragments.RegistrationFragment
import com.capstone.unitechhr.fragments.ResetPasswordFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        goToFragment(LoginFragment())
    }

    fun goToFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        when (fragment) {
            is LoginFragment -> {
                transaction.replace(R.id.fragment_container, fragment)
            }
            is RegistrationFragment -> {
                transaction.replace(R.id.fragment_container, fragment)
            }
            is ForgotPasswordFragment -> transaction.replace(R.id.fragment_container, fragment)
            is ResetPasswordFragment -> transaction.replace(R.id.fragment_container, fragment)
        }

        transaction.commit()
    }
}
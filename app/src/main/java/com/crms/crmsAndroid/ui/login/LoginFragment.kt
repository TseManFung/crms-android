package com.crms.crmsAndroid.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.databinding.FragmentLoginBinding
import com.fyp.crms_backend.utils.AccessPagePermission
import com.fyp.crms_backend.utils.Permission
import com.google.android.material.navigation.NavigationView

class LoginFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as MainActivity
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel = ViewModelProvider(mainActivity).get(SharedViewModel::class.java)
        loginViewModel =
            ViewModelProvider(this, LoginViewModelFactory(sharedViewModel.loginRepository))
                .get(LoginViewModel::class.java)

        val usernameEditText = binding.username
        val passwordEditText = binding.password
        val loginButton = binding.login
        val loadingProgressBar = binding.loading

        loginViewModel.loginFormState.observe(viewLifecycleOwner,
            Observer { loginFormState ->
                if (loginFormState == null) {
                    return@Observer
                }
                loginButton.isEnabled = loginFormState.isDataValid
                loginFormState.usernameError?.let {
                    usernameEditText.error = getString(it)
                }
                loginFormState.passwordError?.let {
                    passwordEditText.error = getString(it)
                }
            })

        loginViewModel.loginResult.observe(viewLifecycleOwner,
            Observer { loginResult ->
                loginResult ?: return@Observer
                loadingProgressBar.visibility = View.GONE
                loginResult.error?.let {
                    showLoginFailed(it)
                }
                loginResult.success?.let {
                    updateUiWithUser(it)
                }
            })

        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                loginViewModel.loginDataChanged(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
        }
        usernameEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginViewModel.login(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
            false
        }

        loginButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            loginViewModel.login(
                usernameEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcomeMsg = getString(R.string.welcome) + model.displayName
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, welcomeMsg, Toast.LENGTH_LONG).show()

        // Update navigation header with user info
        val navView: NavigationView = mainActivity.findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)

        val tvName = headerView.findViewById<TextView>(R.id.tvName)
        val tvIdentity = headerView.findViewById<TextView>(R.id.tvIdentity)

        tvName.text = model.displayName // Set user's name

        // Set identity using Permission based on access level
        val accessLevel = model.accessLevel // Assume accessLevel is part of the model
        val permission = Permission.fromLevel(accessLevel)
        tvIdentity.text =
            permission?.displayName ?: "Unknown" // Set to permission name or "Unknown"
        sharedViewModel.updateAccessPage(model.accessPage)
        // Navigate to the fragment
        val targetId = findFirstAllowedFragment(model.accessPage)
        mainActivity.navController.navigate(targetId)
    }

    private fun findFirstAllowedFragment(accessPage: Int): Int {
        // 定義檢查順序 (按優先級排列)
        val orderedFragments = listOf(
            //R.id.nav_inventory,
            R.id.nav_manInventory,
            R.id.searchItem,
            //R.id.nav_updateItem,
            R.id.nav_updateLoc,
            //R.id.nav_addItem,
            R.id.nav_deleteItem,
            R.id.nav_newRoom,
        )

        orderedFragments.forEach { fragmentId ->
            val permission = AccessPagePermission.fragmentPermissions[fragmentId]
            if (permission == null) return fragmentId // 未列出的默認允許
            if (AccessPagePermission.hasPermission(accessPage, permission)) {
                return fragmentId
            }
        }
        return R.id.searchItem
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
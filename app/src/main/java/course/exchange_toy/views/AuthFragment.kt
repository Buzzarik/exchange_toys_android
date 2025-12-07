package course.exchange_toy.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import course.exchange_toy.MainActivity
import course.exchange_toy.R
import course.exchange_toy.controller.UserController
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.models.user.AuthViewModel
import course.exchange_toy.databinding.FragmentAuthBinding
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast
import course.exchange_toy.utils.showToast
import kotlinx.coroutines.launch

/**
 * Фрагмент авторизации и регистрации
 */
class AuthFragment : BaseFragment<FragmentAuthBinding>() {
    
    private val viewModel: AuthViewModel by viewModels()
    
    private lateinit var userController: UserController
    private lateinit var sessionManager: UserSession
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAuthBinding {
        return FragmentAuthBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = UserSession(requireContext())
        
        // Восстанавливаем текстовые поля из ViewModel СНАЧАЛА
        restoreState()
        
        // Затем восстанавливаем режим toggle
        if (viewModel.isRegisterMode) {
            binding.toggleAuthMode.check(R.id.btnModeRegister)
        } else {
            binding.toggleAuthMode.check(R.id.btnModeLogin)
        }
        
        // Настраиваем UI (теперь apiHost уже восстановлен)
        setupControllers()
        setupToggleMode()
        setupSubmitButton()
        setupTextWatchers()
    }
    
    private fun restoreState() {
        // Если в ViewModel пусто (первый запуск), берем из SharedPreferences
        if (viewModel.apiHost.isEmpty()) {
            viewModel.apiHost = sessionManager.getApiHost()
        }
        
        // Восстанавливаем значения полей из ViewModel
        binding.etApiHost.setText(viewModel.apiHost)
        binding.etEmail.setText(viewModel.email)
        binding.etPassword.setText(viewModel.password)
        binding.etConfirmPassword.setText(viewModel.confirmPassword)
        binding.etFirstName.setText(viewModel.firstName)
        binding.etLastName.setText(viewModel.lastName)
        binding.etMiddleName.setText(viewModel.middleName)
    }
    
    private fun setupControllers() {
        val client = sessionManager.createApiClient()
        userController = UserController(client)
    }
    
    private fun setupTextWatchers() {
        // Сохраняем изменения в ViewModel при вводе текста
        binding.etApiHost.doOnTextChanged { text, _, _, _ ->
            viewModel.apiHost = text.toString()
        }
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            viewModel.email = text.toString()
        }
        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            viewModel.password = text.toString()
        }
        binding.etConfirmPassword.doOnTextChanged { text, _, _, _ ->
            viewModel.confirmPassword = text.toString()
        }
        binding.etFirstName.doOnTextChanged { text, _, _, _ ->
            viewModel.firstName = text.toString()
        }
        binding.etLastName.doOnTextChanged { text, _, _, _ ->
            viewModel.lastName = text.toString()
        }
        binding.etMiddleName.doOnTextChanged { text, _, _, _ ->
            viewModel.middleName = text.toString()
        }
    }
    
    private fun setupToggleMode() {
        // Устанавливаем видимость полей в зависимости от режима
        updateUIForMode(viewModel.isRegisterMode)
        
        binding.toggleAuthMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnModeLogin -> {
                        // Режим входа
                        viewModel.isRegisterMode = false
                        updateUIForMode(false)
                    }
                    R.id.btnModeRegister -> {
                        // Режим регистрации
                        viewModel.isRegisterMode = true
                        updateUIForMode(true)
                    }
                }
            }
        }
    }
    
    private fun updateUIForMode(isRegister: Boolean) {
        if (isRegister) {
            binding.layoutRegisterFields.visibility = View.VISIBLE
            binding.btnSubmit.text = "Зарегистрироваться"
        } else {
            binding.layoutRegisterFields.visibility = View.GONE
            binding.btnSubmit.text = "Войти"
        }
    }
    
    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            // Пересоздаем контроллер с актуальным хостом
            setupControllers()
            
            if (viewModel.isRegisterMode) {
                performRegister()
            } else {
                performLogin()
            }
        }
    }
    
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Валидация
        if (email.isEmpty() || password.isEmpty()) {
            showLongToast("Заполните все поля")
            return
        }
        
        // Используем callback из контроллера
        userController.login(
            scope = lifecycleScope,
            email = email,
            password = password,
            callback = object : UserController.AuthCallback {
                override fun onLoading() {
                    setLoading(true)
                }
                
                override fun onSuccess(userId: String) {
                    // Сохраняем user_id и хост
                    sessionManager.saveUserId(userId)
                    sessionManager.saveApiHost(binding.etApiHost.text.toString())
                    
                    // Очищаем форму - данные больше не нужны
                    viewModel.clearForm()
                    
                    setLoading(false)
                    showToast("Вход выполнен успешно!")
                    navigateToMain()
                }
                
                override fun onError(message: String) {
                    setLoading(false)
                    showLongToast(message)
                }
            }
        )
    }
    
    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val middleName = binding.etMiddleName.text.toString().trim().takeIf { it.isNotEmpty() }
        
        // Валидация
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
            firstName.isEmpty() || lastName.isEmpty()) {
            showLongToast("Заполните все обязательные поля")
            return
        }
        
        if (password != confirmPassword) {
            showLongToast("Пароли не совпадают")
            return
        }
        
        // Используем callback из контроллера
        userController.register(
            scope = lifecycleScope,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            firstName = firstName,
            lastName = lastName,
            middleName = middleName,
            callback = object : UserController.AuthCallback {
                override fun onLoading() {
                    setLoading(true)
                }
                
                override fun onSuccess(userId: String) {
                    // Сохраняем user_id и хост
                    sessionManager.saveUserId(userId)
                    sessionManager.saveApiHost(binding.etApiHost.text.toString())
                    
                    // Очищаем форму - данные больше не нужны
                    viewModel.clearForm()
                    
                    setLoading(false)
                    showToast("Регистрация успешна!")
                    navigateToMain()
                }
                
                override fun onError(message: String) {
                    setLoading(false)
                    showLongToast(message)
                }
            }
        )
    }
    
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.etFirstName.isEnabled = !isLoading
        binding.etLastName.isEnabled = !isLoading
        binding.etMiddleName.isEnabled = !isLoading
        binding.etApiHost.isEnabled = !isLoading
    }
    
    private fun navigateToMain() {
        // Вызываем метод MainActivity для переключения на главный экран
        (requireActivity() as? MainActivity)?.updateScreenBasedOnAuth()
    }
}


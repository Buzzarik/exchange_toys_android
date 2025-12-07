package course.exchange_toy

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.databinding.ActivityMainBinding
import course.exchange_toy.views.AuthFragment
import course.exchange_toy.views.MainContentFragment

/**
 * Главная Activity
 * 
 * Логика:
 * 1. Проверяет user_id при запуске
 * 2. Если НЕТ user_id -> показывает AuthFragment
 * 3. Если ЕСТЬ user_id -> показывает главный контент + кнопка "Выйти"
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: UserSession
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = UserSession(this)
        
        // Настройка edge-to-edge с учетом system bars
        setupEdgeToEdge()
        
        // Проверяем авторизацию и показываем нужный экран
        // ВАЖНО: только при первом запуске, не при повороте экрана!
        if (savedInstanceState == null) {
            checkAuthAndShowScreen()
        }
    }
    
    /**
     * Настраивает edge-to-edge отображение с правильной обработкой system bars
     */
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Применяем padding только сверху и снизу для корневого контейнера
            // Боковые отступы не нужны, так как контент должен занимать всю ширину
            view.setPadding(
                0, // left - не нужен
                systemBarsInsets.top, // top - для статус бара
                0, // right - не нужен  
                0  // bottom - будет обрабатываться в фрагментах с навигацией
            )
            
            insets
        }
    }
    
    override fun onResume() {
        super.onResume()
        // При повороте экрана обновляем видимость toolbar
        binding.appBarLayout.visibility = if (sessionManager.isLoggedIn()) View.VISIBLE else View.GONE
    }
    
    /**
     * Обновляет экран на основе статуса авторизации
     * Вызывается из AuthFragment после успешной авторизации
     */
    fun updateScreenBasedOnAuth() {
        val isLoggedIn = sessionManager.isLoggedIn()
        
        if (isLoggedIn) {
            // Пользователь авторизован -> показываем главный экран
            showMainContent()
        } else {
            // Пользователь НЕ авторизован -> показываем AuthFragment
            showAuthScreen()
        }
    }
    
    /**
     * Проверяет авторизацию и показывает соответствующий экран
     */
    private fun checkAuthAndShowScreen() {
        if (sessionManager.isLoggedIn()) {
            // Пользователь авторизован -> показываем главный экран
            showMainContent()
        } else {
            // Пользователь НЕ авторизован -> показываем AuthFragment
            showAuthScreen()
        }
    }
    
    /**
     * Показывает экран авторизации
     */
    private fun showAuthScreen() {
        // Скрываем toolbar с кнопкой выхода
        binding.appBarLayout.visibility = View.GONE
        
        // Показываем фрагмент авторизации
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, AuthFragment())
        }
    }
    
    /**
     * Показывает главный контент (после успешной авторизации)
     */
    private fun showMainContent() {
        // Показываем toolbar с кнопкой выхода
        binding.appBarLayout.visibility = View.VISIBLE
        
        // Показываем главный контент с Bottom Navigation
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, MainContentFragment())
        }
    }
    
}





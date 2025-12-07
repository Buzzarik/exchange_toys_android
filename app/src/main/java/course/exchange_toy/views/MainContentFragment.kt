package course.exchange_toy.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import course.exchange_toy.R
import course.exchange_toy.databinding.FragmentMainContentBinding

/**
 * Главный фрагмент с Bottom Navigation
 * Управляет переключением между разделами приложения
 */
class MainContentFragment : BaseFragment<FragmentMainContentBinding>() {
    
    // Отключаем базовую обработку insets, так как здесь нужна специальная логика для BottomNav
    override val applyWindowInsets: Boolean = false
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMainContentBinding {
        return FragmentMainContentBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Настройка window insets для Bottom Navigation
        setupWindowInsets()
        
        // По умолчанию показываем "Мои игрушки"
        if (savedInstanceState == null) {
            showFragment(MyToysFragment())
        }
        
        // Настраиваем Bottom Navigation
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_my_toys -> {
                    showFragment(MyToysFragment())
                    true
                }
                R.id.nav_shop -> {
                    showFragment(ShopFragment())
                    true
                }
                R.id.nav_exchanges -> {
                    showFragment(ExchangesFragment())
                    true
                }
                R.id.nav_profile -> {
                    showFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Настраивает обработку window insets для корректного отображения
     * с учетом системных панелей (навигационная панель снизу)
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Применяем padding только снизу для нижней навигации
            binding.bottomNav.setPadding(
                0,
                0,
                0,
                systemBarsInsets.bottom
            )
            
            insets
        }
    }
    
    private fun showFragment(fragment: Fragment) {
        childFragmentManager.commit {
            replace(R.id.navHostFragment, fragment)
        }
    }
}

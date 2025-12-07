package course.exchange_toy.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import course.exchange_toy.MainActivity
import course.exchange_toy.databinding.FragmentProfileBinding
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.showToast

/**
 * Фрагмент "Профиль"
 * Содержит кнопку выхода
 */
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    
    private lateinit var sessionManager: UserSession
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = UserSession(requireContext())
        
        // Кнопка выхода
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        // Удаляем user_id
        sessionManager.logout()
        
        // Показываем уведомление
        showToast("Вы вышли из аккаунта")
        
        // Обновляем экран через MainActivity
        (requireActivity() as? MainActivity)?.updateScreenBasedOnAuth()
    }
}


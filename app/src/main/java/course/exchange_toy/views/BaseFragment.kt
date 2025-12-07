package course.exchange_toy.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    
    private var _binding: VB? = null
    
    /**
     * Доступ к binding (безопасно использовать между onCreateView и onDestroyView)
     */
    protected val binding: VB
        get() = _binding!!
    
    /**
     * Создать ViewBinding для этого фрагмента
     * Должно быть реализовано в дочернем классе
     */
    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    
    /**
     * Нужна ли автоматическая обработка window insets
     * По умолчанию true, можно переопределить в дочернем классе
     */
    protected open val applyWindowInsets: Boolean = true
    
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = createBinding(inflater, container)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Автоматическая обработка window insets для всех фрагментов
        if (applyWindowInsets) {
            setupWindowInsets(view)
        }
    }
    
    /**
     * Настраивает обработку window insets для корректного отображения
     * с учетом системных панелей (статус бар, навигационная панель)
     */
    private fun setupWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Применяем padding снизу для навигационной панели
            // Верхний padding уже установлен в Activity
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemBarsInsets.bottom
            )
            
            insets
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


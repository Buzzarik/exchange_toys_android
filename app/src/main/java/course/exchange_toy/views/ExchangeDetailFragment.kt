package course.exchange_toy.views

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import course.exchange_toy.R
import course.exchange_toy.controller.ExchangeController
import course.exchange_toy.databinding.FragmentExchangeDetailBinding
import course.exchange_toy.models.remote.models.ExchangeDetailsStatus
import course.exchange_toy.models.remote.models.ExchangeInfo
import course.exchange_toy.models.remote.models.ExchangeStatus
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast
import course.exchange_toy.utils.showToast

/**
 * Детальный просмотр обмена
 * Показывает информацию об обмене и позволяет подтвердить/отменить
 */
class ExchangeDetailFragment : BaseFragment<FragmentExchangeDetailBinding>() {
    
    private lateinit var sessionManager: UserSession
    private lateinit var exchangeController: ExchangeController
    
    private var currentExchange: ExchangeInfo? = null
    private var exchangeId: String = ""
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentExchangeDetailBinding {
        return FragmentExchangeDetailBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        exchangeId = arguments?.getString(ARG_EXCHANGE_ID) ?: run {
            showToast("Ошибка: нет ID обмена")
            goBack()
            return
        }
        
        sessionManager = UserSession(requireContext())
        
        val client = sessionManager.createApiClient()
        exchangeController = ExchangeController(client)
        
        setupButtons()
        loadExchangeDetails()
    }
    
    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            goBack()
        }
        
        binding.btnCancel.setOnClickListener {
            showCancelConfirmation()
        }
        
        binding.btnAction.setOnClickListener {
            performAction()
        }
    }
    
    private fun loadExchangeDetails() {
        val userId = sessionManager.getUserId() ?: return
        
        exchangeController.getExchangeDetails(
            scope = lifecycleScope,
            userId = userId,
            exchangeId = exchangeId,
            callback = object : ExchangeController.ExchangeDetailCallback {
                override fun onLoading() {
                    binding.progressBar.visibility = View.VISIBLE
                }
                
                override fun onSuccess(exchange: ExchangeInfo) {
                    binding.progressBar.visibility = View.GONE
                    currentExchange = exchange
                    displayExchange(exchange)
                }
                
                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                    goBack()
                }
            }
        )
    }
    
    private fun displayExchange(exchange: ExchangeInfo) {
        val userId = sessionManager.getUserId() ?: return
        
        // Находим детали для текущего пользователя и другого
        val myDetails = exchange.details.find { it.toy.userId == userId }
        val otherDetails = exchange.details.find { it.toy.userId != userId }
        
        if (myDetails == null || otherDetails == null) {
            showToast("Ошибка: некорректные данные обмена")
            goBack()
            return
        }
        
        // Отображаем информацию об обмене
        binding.tvMyToy.text = "Моя игрушка: ${myDetails.toy.name}"
        binding.tvMyToyDescription.text = myDetails.toy.description ?: "Нет описания"
        
        binding.tvOtherToy.text = "Игрушка для обмена: ${otherDetails.toy.name}"
        binding.tvOtherToyDescription.text = otherDetails.toy.description ?: "Нет описания"
        binding.tvOtherUser.text = "Владелец: ${otherDetails.user.firstName} ${otherDetails.user.lastName}"
        
        // Статус обмена с цветом
        val (overallStatusText, overallStatusColor) = when (exchange.status) {
            ExchangeStatus.CREATED -> "Ожидает действий" to Color.parseColor("#2196F3") // 🔵 Синий
            ExchangeStatus.CONFIRM -> "Обе стороны готовы" to Color.parseColor("#FF9800") // 🟠 Оранжевый
            ExchangeStatus.SUCCESS -> "Обмен завершен" to Color.parseColor("#4CAF50") // 🟢 Зеленый
            ExchangeStatus.FAILED -> "Отменен" to Color.parseColor("#F44336") // 🔴 Красный
        }
        binding.tvOverallStatus.text = "Статус обмена: $overallStatusText"
        binding.tvOverallStatus.setTextColor(overallStatusColor)
        
        // Мой статус с цветом
        val (myStatusText, myStatusColor) = when (myDetails.status) {
            ExchangeDetailsStatus.CREATED -> "Ожидает действия" to Color.parseColor("#2196F3") // 🔵 Синий
            ExchangeDetailsStatus.CONFIRM_1 -> "Готовы списаться" to Color.parseColor("#FF9800") // 🟠 Оранжевый
            ExchangeDetailsStatus.CONFIRM_2 -> "Подтвердили обмен" to Color.parseColor("#FF9800") // 🟠 Оранжевый
            ExchangeDetailsStatus.SUCCESS -> "Обмен завершен" to Color.parseColor("#4CAF50") // 🟢 Зеленый
            ExchangeDetailsStatus.FAILED -> "Отменено" to Color.parseColor("#F44336") // 🔴 Красный
        }
        binding.tvMyStatus.text = "Ваш статус: $myStatusText"
        binding.tvMyStatus.setTextColor(myStatusColor)
        
        // Статус другого пользователя с цветом
        val (otherStatusText, otherStatusColor) = when (otherDetails.status) {
            ExchangeDetailsStatus.CREATED -> "Ожидает действия" to Color.parseColor("#2196F3") // 🔵 Синий
            ExchangeDetailsStatus.CONFIRM_1 -> "Готов списаться" to Color.parseColor("#FF9800") // 🟠 Оранжевый
            ExchangeDetailsStatus.CONFIRM_2 -> "Подтвердил обмен" to Color.parseColor("#FF9800") // 🟠 Оранжевый
            ExchangeDetailsStatus.SUCCESS -> "Обмен завершен" to Color.parseColor("#4CAF50") // 🟢 Зеленый
            ExchangeDetailsStatus.FAILED -> "Отменено" to Color.parseColor("#F44336") // 🔴 Красный
        }
        binding.tvOtherStatus.text = "Статус собеседника: $otherStatusText"
        binding.tvOtherStatus.setTextColor(otherStatusColor)
        
        // Настройка кнопок в зависимости от статусов
        setupActionButtons(myDetails.status, otherDetails.status, exchange.status)
    }
    
    private fun setupActionButtons(
        myStatus: ExchangeDetailsStatus,
        otherStatus: ExchangeDetailsStatus,
        overallStatus: ExchangeStatus
    ) {
        // По умолчанию скрываем статусное сообщение
        binding.tvStatusMessage.visibility = View.GONE
        
        when {
            // ========== ТЕРМИНАЛЬНЫЕ СТАТУСЫ (только SUCCESS и FAILED!) ==========
            
            // ТЕРМИНАЛЬНЫЙ - обмен завершен успешно
            overallStatus == ExchangeStatus.SUCCESS -> {
                binding.btnAction.visibility = View.GONE
                binding.btnCancel.visibility = View.GONE
                binding.tvStatusMessage.visibility = View.VISIBLE
                binding.tvStatusMessage.text = "✓ Обмен успешно завершен!"
                binding.tvStatusMessage.setTextColor(Color.parseColor("#4CAF50")) // 🟢 Зеленый
            }
            
            // ТЕРМИНАЛЬНЫЙ - обмен отменен
            overallStatus == ExchangeStatus.FAILED -> {
                binding.btnAction.visibility = View.GONE
                binding.btnCancel.visibility = View.GONE
                binding.tvStatusMessage.visibility = View.VISIBLE
                binding.tvStatusMessage.text = "✗ Обмен отменен"
                binding.tvStatusMessage.setTextColor(Color.parseColor("#F44336")) // 🔴 Красный
            }
            
            // ========== НЕ ТЕРМИНАЛЬНЫЕ СТАТУСЫ (можно менять) ==========
            
            // 1) СПИСАТЬСЯ - я еще не согласился (CREATED)
            // Независимо от статуса другого пользователя
            myStatus == ExchangeDetailsStatus.CREATED -> {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = "Списаться"
                binding.btnAction.isEnabled = true
                binding.btnCancel.visibility = View.VISIBLE
            }
            
            // Я подтвердил обмен (CONFIRM_2), жду другого
            myStatus == ExchangeDetailsStatus.CONFIRM_2 -> {
                binding.btnAction.visibility = View.GONE
                binding.btnCancel.visibility = View.GONE
                binding.tvStatusMessage.visibility = View.VISIBLE
                binding.tvStatusMessage.text = "⏳ Ждем подтверждения обмена от другого пользователя"
                binding.tvStatusMessage.setTextColor(Color.parseColor("#FF9800")) // 🟠 Оранжевый
            }
            
            // 2) ОБА НАЖАЛИ СПИСАТЬСЯ (оба в CONFIRM_1) - можно подтвердить обмен
            // ⚠️ Общий статус обмена будет CONFIRM, но это НЕ терминальный!
            myStatus == ExchangeDetailsStatus.CONFIRM_1 && 
                otherStatus == ExchangeDetailsStatus.CONFIRM_1 -> {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = "Подтвердить обмен"
                binding.btnAction.isEnabled = true
                binding.btnCancel.visibility = View.VISIBLE
            }
            
            // Я нажал "Списаться", но другой еще нет - жду
            myStatus == ExchangeDetailsStatus.CONFIRM_1 && 
                otherStatus == ExchangeDetailsStatus.CREATED -> {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = "Ждем ответа другого пользователя"
                binding.btnAction.isEnabled = false
                binding.btnCancel.visibility = View.VISIBLE
            }
            
            // Другой уже подтвердил обмен, а я еще только готов списаться
            myStatus == ExchangeDetailsStatus.CONFIRM_1 && 
                otherStatus == ExchangeDetailsStatus.CONFIRM_2 -> {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = "Подтвердить обмен"
                binding.btnAction.isEnabled = true
                binding.btnCancel.visibility = View.VISIBLE
            }
            
            // Другие случаи - обработка не должна происходить
            else -> {
                binding.btnAction.visibility = View.GONE
                binding.btnCancel.visibility = View.GONE
                binding.tvStatusMessage.visibility = View.VISIBLE
                binding.tvStatusMessage.text = "Обработка..."
                binding.tvStatusMessage.setTextColor(Color.parseColor("#9E9E9E")) // Серый
            }
        }
    }
    
    private fun performAction() {
        val userId = sessionManager.getUserId() ?: return
        val exchange = currentExchange ?: return
        
        val myDetails = exchange.details.find { it.toy.userId == userId } ?: return
        val otherDetails = exchange.details.find { it.toy.userId != userId } ?: return
        
        // Определяем действие в зависимости от статусов
        when {
            // 1) CREATED → Списаться (отправляем confirm_1)
            myDetails.status == ExchangeDetailsStatus.CREATED -> {
                confirmExchange("confirm_1", "Вы готовы списаться! Ждем ответа другого пользователя")
            }
            
            // 2) Я CONFIRM_1 + другой CONFIRM_1 → Подтвердить обмен (отправляем confirm_2)
            myDetails.status == ExchangeDetailsStatus.CONFIRM_1 && 
                otherDetails.status == ExchangeDetailsStatus.CONFIRM_1 -> {
                confirmExchange("confirm_2", "Вы подтвердили обмен! Ждем подтверждения от другого пользователя")
            }
            
            // 3) Я CONFIRM_1 + другой CONFIRM_2 → Подтвердить обмен (отправляем confirm_2)
            // Другой уже подтвердил, я догоняю
            myDetails.status == ExchangeDetailsStatus.CONFIRM_1 && 
                otherDetails.status == ExchangeDetailsStatus.CONFIRM_2 -> {
                confirmExchange("confirm_2", "Вы подтвердили обмен! Обмен завершен")
            }
            
            // Другие случаи - не делаем ничего
            else -> {
                showToast("Действие недоступно")
            }
        }
    }
    
    private fun confirmExchange(status: String, successMessage: String) {
        val userId = sessionManager.getUserId() ?: return
        
        exchangeController.confirmExchange(
            scope = lifecycleScope,
            userId = userId,
            exchangeId = exchangeId,
            status = status,
            callback = object : ExchangeController.ExchangeActionCallback {
                override fun onLoading() {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnAction.isEnabled = false
                }
                
                override fun onSuccess() {
                    binding.progressBar.visibility = View.GONE
                    showToast(successMessage)
                    loadExchangeDetails() // Перезагружаем данные
                }
                
                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnAction.isEnabled = true
                    showLongToast(message)
                }
            }
        )
    }
    
    private fun showCancelConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Отменить обмен?")
            .setMessage("Вы уверены, что хотите отменить этот обмен?")
            .setPositiveButton("Да") { _, _ ->
                cancelExchange()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
    
    private fun cancelExchange() {
        val userId = sessionManager.getUserId() ?: return
        
        exchangeController.cancelExchange(
            scope = lifecycleScope,
            userId = userId,
            exchangeId = exchangeId,
            callback = object : ExchangeController.ExchangeActionCallback {
                override fun onLoading() {
                    binding.progressBar.visibility = View.VISIBLE
                }
                
                override fun onSuccess() {
                    binding.progressBar.visibility = View.GONE
                    showToast("Обмен отменен")
                    goBack()
                }
                
                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                }
            }
        )
    }
    
    private fun goBack() {
        parentFragmentManager.popBackStack()
    }
    
    companion object {
        private const val ARG_EXCHANGE_ID = "exchange_id"
        
        fun newInstance(exchangeId: String): ExchangeDetailFragment {
            return ExchangeDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EXCHANGE_ID, exchangeId)
                }
            }
        }
    }
}


package course.exchange_toy.views

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import course.exchange_toy.controller.ToyController
import course.exchange_toy.databinding.DialogCreateExchangeBinding
import course.exchange_toy.models.remote.models.Toy
import course.exchange_toy.models.remote.models.ToyStatus
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast
import course.exchange_toy.utils.showToast

/**
 * Диалог создания обмена
 * Показывает список своих игрушек для выбора
 */
class CreateExchangeDialog : DialogFragment() {
    
    private var _binding: DialogCreateExchangeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionManager: UserSession
    private lateinit var toyController: ToyController
    private lateinit var selectionAdapter: ToySelectionAdapter
    
    private var targetToyId: String = ""
    private var targetToyName: String = ""
    private var targetUserId: String = ""
    private var onSuccess: (() -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateExchangeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = UserSession(requireContext())
        
        // Создаем клиент и контроллер
        val client = sessionManager.createApiClient()
        toyController = ToyController(client)
        
        // Получаем аргументы
        targetToyId = arguments?.getString(ARG_TARGET_TOY_ID) ?: ""
        targetToyName = arguments?.getString(ARG_TARGET_TOY_NAME) ?: ""
        targetUserId = arguments?.getString(ARG_TARGET_USER_ID) ?: ""
        
        binding.tvTargetToy.text = targetToyName
        
        setupRecyclerView()
        setupButtons()
        loadMyToys()
    }
    
    private fun setupRecyclerView() {
        selectionAdapter = ToySelectionAdapter { toy ->
            // Игрушка выбрана - визуально выделяется адаптером
        }
        
        binding.rvMyToys.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectionAdapter
        }
    }
    
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnCreate.setOnClickListener {
            createExchange()
        }
    }
    
    private fun loadMyToys() {
        val userId = sessionManager.getUserId() ?: return
        
        toyController.getMyToys(
            scope = lifecycleScope,
            userId = userId,
            cursor = null,
            limit = 100, // Загружаем все свои игрушки
            callback = object : ToyController.ToysListCallback {
                override fun onLoading() {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvMyToys.visibility = View.GONE
                }
                
                override fun onSuccess(newToys: List<Toy>, nextCursor: String?) {
                    binding.progressBar.visibility = View.GONE
                    
                    // Фильтруем только игрушки на обмене и созданные
                    val availableToys = newToys.filter { 
                        it.status == ToyStatus.EXCHANGING || it.status == ToyStatus.CREATED
                    }
                    
                    selectionAdapter.submitList(availableToys)
                    
                    if (availableToys.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvMyToys.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvMyToys.visibility = View.VISIBLE
                    }
                }
                
                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                }
            }
        )
    }
    
    private fun createExchange() {
        val selectedToy = selectionAdapter.getSelectedToy()
        if (selectedToy == null) {
            showToast("Выберите свою игрушку")
            return
        }
        
        val userId = sessionManager.getUserId() ?: return
        
        toyController.createExchange(
            scope = lifecycleScope,
            currentUserId = userId,
            myToyId = selectedToy.toyId,
            targetUserId = targetUserId,
            targetToyId = targetToyId,
            callback = object : ToyController.ExchangeCallback {
                override fun onLoading() {
                    setLoading(true)
                }
                
                override fun onSuccess() {
                    setLoading(false)
                    onSuccess?.invoke()
                    dismiss()
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
        binding.btnCreate.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
        binding.rvMyToys.isEnabled = !isLoading
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_TARGET_TOY_ID = "target_toy_id"
        private const val ARG_TARGET_TOY_NAME = "target_toy_name"
        private const val ARG_TARGET_USER_ID = "target_user_id"
        
        fun newInstance(
            targetToyId: String,
            targetToyName: String,
            targetUserId: String,
            onSuccess: () -> Unit
        ): CreateExchangeDialog {
            return CreateExchangeDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TARGET_TOY_ID, targetToyId)
                    putString(ARG_TARGET_TOY_NAME, targetToyName)
                    putString(ARG_TARGET_USER_ID, targetUserId)
                }
                this.onSuccess = onSuccess
            }
        }
    }
}


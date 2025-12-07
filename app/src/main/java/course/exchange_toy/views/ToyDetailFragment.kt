package course.exchange_toy.views

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.load
import course.exchange_toy.R
import course.exchange_toy.controller.ToyController
import course.exchange_toy.databinding.FragmentToyDetailBinding
import course.exchange_toy.models.remote.models.Toy
import course.exchange_toy.models.remote.models.ToyStatus
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast
import course.exchange_toy.utils.showToast

/**
 * Детальный экран игрушки
 * Показывает полную информацию и позволяет редактировать, удалять, менять статус
 */
class ToyDetailFragment : BaseFragment<FragmentToyDetailBinding>() {
    
    private lateinit var sessionManager: UserSession
    private lateinit var toyController: ToyController
    
    private var currentToy: Toy? = null
    private var toyId: String = ""
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentToyDetailBinding {
        return FragmentToyDetailBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Получаем toy_id из аргументов
        toyId = arguments?.getString(ARG_TOY_ID) ?: run {
            showToast("Ошибка: нет ID игрушки")
            goBack()
            return
        }
        
        sessionManager = UserSession(requireContext())
        
        // Создаем клиент и контроллер
        val client = sessionManager.createApiClient()
        toyController = ToyController(client)
        
        setupButtons()
        loadToyDetails()
    }
    
    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            goBack()
        }
        
        binding.btnEdit.setOnClickListener {
            // Открываем диалог редактирования
            ToyFormDialog.newInstance(toyId = toyId, onSuccess = {
                loadToyDetails() // Перезагружаем данные после редактирования
            }).show(parentFragmentManager, "ToyFormDialog")
        }
        
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
        
        binding.btnToggleStatus.setOnClickListener {
            toggleToyStatus()
        }
    }
    
    private fun loadToyDetails() {
        val userId = sessionManager.getUserId() ?: run {
            showToast("Ошибка: не авторизован")
            goBack()
            return
        }
        
        toyController.getToyDetails(
            scope = lifecycleScope,
            userId = userId,
            toyId = toyId,
            callback = object : ToyController.ToyDetailCallback {
                override fun onLoading() {
                    binding.progressBar.visibility = View.VISIBLE
                }
                
                override fun onSuccess(toy: Toy) {
                    binding.progressBar.visibility = View.GONE
                    currentToy = toy
                    displayToy(toy)
                }
                
                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                    goBack()
                }
            }
        )
    }
    
    private fun displayToy(toy: Toy) {
        binding.tvToyName.text = toy.name
        binding.tvDescription.text = toy.description ?: "Нет описания"
        
        // DEBUG: Логируем photoUrl
        android.util.Log.d("ToyDetail", "PhotoUrl from server: '${toy.photoUrl}'")
        
        // Статус (ToyStatus - это enum)
        val statusText = when (toy.status) {
            ToyStatus.CREATED -> "Создана"
            ToyStatus.EXCHANGING -> "На обмене"
            ToyStatus.EXCHANGED -> "Обменяна"
            ToyStatus.REMOVED -> "Удалена"
        }
        binding.tvToyStatus.text = "Статус: $statusText"
        
        // Кнопка изменения статуса
        binding.btnToggleStatus.text = when (toy.status) {
            ToyStatus.CREATED -> "Выставить на обмен"
            ToyStatus.EXCHANGING -> "Убрать с обмена"
            ToyStatus.EXCHANGED -> "Игрушка обменяна"
            ToyStatus.REMOVED -> "Игрушка удалена"
        }
        
        // Загрузка фото через Coil
        if (!toy.photoUrl.isNullOrEmpty()) {
            val apiHost = sessionManager.getApiHost()
            
            // Формируем полный URL (заменяем хост)
            val fullPhotoUrl = if (toy.photoUrl.contains("/upload")) {
                val pathFromUpload = toy.photoUrl.substringAfter("/upload")
                "$apiHost/upload$pathFromUpload"
            } else {
                toy.photoUrl
            }
            
            android.util.Log.d("ToyDetail", "Loading photo: $fullPhotoUrl")
            
            binding.ivToyPhoto.load(fullPhotoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_toy)
                error(R.drawable.ic_placeholder_toy)
                listener(
                    onSuccess = { _, _ -> 
                        android.util.Log.d("ToyDetail", "Photo loaded successfully") 
                    },
                    onError = { _, result -> 
                        android.util.Log.e("ToyDetail", "Photo load error: ${result.throwable.message}") 
                    }
                )
            }
        } else {
            android.util.Log.d("ToyDetail", "Photo URL is null or empty")
            // Если фото нет - показываем placeholder
            binding.ivToyPhoto.setImageResource(R.drawable.ic_placeholder_toy)
        }
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить игрушку?")
            .setMessage("Вы уверены, что хотите удалить эту игрушку? Это действие нельзя отменить.")
            .setPositiveButton("Да") { _, _ ->
                deleteToy()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
    
    private fun deleteToy() {
        val userId = sessionManager.getUserId() ?: return
        
        toyController.deleteToy(
            scope = lifecycleScope,
            userId = userId,
            toyId = toyId,
            callback = object : ToyController.ToyCallback {
                override fun onLoading() {
                    binding.progressBar.visibility = View.VISIBLE
                }
                
                override fun onSuccess() {
                    binding.progressBar.visibility = View.GONE
                    showToast("Игрушка успешно удалена")
                    goBack()
                }
                
                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                }
            }
        )
    }
    
    private fun toggleToyStatus() {
        val userId = sessionManager.getUserId() ?: return
        val currentStatus = currentToy?.status ?: return
        
        // Определяем новый статус (ToyStatus - enum)
        val newStatus = when (currentStatus) {
            ToyStatus.CREATED -> "exchanging"
            ToyStatus.EXCHANGING -> "created"
            ToyStatus.EXCHANGED -> {
                showToast("Обменянную игрушку нельзя изменить")
                return
            }
            ToyStatus.REMOVED -> {
                showToast("Удаленную игрушку нельзя изменить")
                return
            }
        }
        
        toyController.changeToyStatus(
            scope = lifecycleScope,
            userId = userId,
            toyId = toyId,
            newStatus = newStatus,
            callback = object : ToyController.ToyCallback {
                override fun onLoading() {
                    binding.progressBar.visibility = View.VISIBLE
                }
                
                override fun onSuccess() {
                    binding.progressBar.visibility = View.GONE
                    val message = if (newStatus == "exchanging") {
                        "Игрушка выставлена на обмен"
                    } else {
                        "Игрушка убрана с обмена"
                    }
                    showToast(message)
                    // Перезагружаем данные
                    loadToyDetails()
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
        private const val ARG_TOY_ID = "toy_id"
        
        fun newInstance(toyId: String): ToyDetailFragment {
            return ToyDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOY_ID, toyId)
                }
            }
        }
    }
}


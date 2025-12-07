package course.exchange_toy.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.load
import course.exchange_toy.R
import course.exchange_toy.controller.ToyController
import course.exchange_toy.databinding.FragmentShopToyDetailBinding
import course.exchange_toy.models.remote.models.Toy
import course.exchange_toy.models.remote.models.ToyStatus
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast
import course.exchange_toy.utils.showToast

/**
 * Детальный просмотр игрушки из магазина
 * Показывает информацию об игрушке другого пользователя
 * Позволяет предложить обмен
 */
class ShopToyDetailFragment : BaseFragment<FragmentShopToyDetailBinding>() {
    
    private lateinit var sessionManager: UserSession
    private lateinit var toyController: ToyController
    
    private var currentToy: Toy? = null
    private var toyId: String = ""
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentShopToyDetailBinding {
        return FragmentShopToyDetailBinding.inflate(inflater, container, false)
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
        
        binding.btnProposeExchange.setOnClickListener {
            proposeExchange()
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
        
        // Статус (ToyStatus - это enum)
        val statusText = when (toy.status) {
            ToyStatus.CREATED -> "Создана"
            ToyStatus.EXCHANGING -> "На обмене"
            ToyStatus.EXCHANGED -> "Обменяна"
            ToyStatus.REMOVED -> "Удалена"
        }
        binding.tvToyStatus.text = "Статус: $statusText"
        
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
            
            android.util.Log.d("ShopToyDetail", "Loading photo: $fullPhotoUrl")
            
            binding.ivToyPhoto.load(fullPhotoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_toy)
                error(R.drawable.ic_placeholder_toy)
                listener(
                    onSuccess = { _, _ -> 
                        android.util.Log.d("ShopToyDetail", "Photo loaded successfully") 
                    },
                    onError = { _, result -> 
                        android.util.Log.e("ShopToyDetail", "Photo load error: ${result.throwable.message}") 
                    }
                )
            }
        } else {
            android.util.Log.d("ShopToyDetail", "Photo URL is null or empty")
            // Если фото нет - показываем placeholder
            binding.ivToyPhoto.setImageResource(R.drawable.ic_placeholder_toy)
        }
    }
    
    private fun proposeExchange() {
        val targetToy = currentToy ?: return
        
        // Открываем диалог выбора своей игрушки для обмена
        CreateExchangeDialog.newInstance(
            targetToyId = targetToy.toyId,
            targetToyName = targetToy.name,
            targetUserId = targetToy.userId, // ID владельца игрушки
            onSuccess = {
                showToast("Предложение обмена создано!")
                goBack()
            }
        ).show(parentFragmentManager, "CreateExchangeDialog")
    }
    
    private fun goBack() {
        parentFragmentManager.popBackStack()
    }
    
    companion object {
        private const val ARG_TOY_ID = "toy_id"
        
        fun newInstance(toyId: String): ShopToyDetailFragment {
            return ShopToyDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOY_ID, toyId)
                }
            }
        }
    }
}


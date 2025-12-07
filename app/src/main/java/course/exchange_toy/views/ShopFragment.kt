package course.exchange_toy.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import course.exchange_toy.R
import course.exchange_toy.controller.ToyController
import course.exchange_toy.databinding.FragmentShopBinding
import course.exchange_toy.models.remote.models.Toy
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast

/**
 * Фрагмент "Магазин обмена"
 * Отображает игрушки других пользователей, доступные для обмена
 */
class ShopFragment : BaseFragment<FragmentShopBinding>() {
    
    private lateinit var sessionManager: UserSession
    private lateinit var toyController: ToyController
    private lateinit var toysAdapter: ToysAdapter
    
    private val toys = mutableListOf<Toy>()
    private var currentCursor: String? = null
    private var isLoading = false
    private var hasMorePages = true
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentShopBinding {
        return FragmentShopBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = UserSession(requireContext())
        
        // Создаем клиент и контроллер
        val client = sessionManager.createApiClient()
        toyController = ToyController(client)
        
        setupRecyclerView()
        loadShopToys()
    }
    
    private fun setupRecyclerView() {
        toysAdapter = ToysAdapter { toy ->
            // При клике открываем детали игрушки из магазина
            openShopToyDetails(toy)
        }
        
        binding.rvShopToys.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = toysAdapter
            
            // Пагинация при прокрутке
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Загружаем следующую страницу
                    if (!isLoading && hasMorePages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadShopToys(currentCursor)
                        }
                    }
                }
            })
        }
    }
    
    /**
     * Загрузка игрушек других пользователей (статус: exchanging)
     */
    private fun loadShopToys(cursor: String? = null) {
        val userId = sessionManager.getUserId() ?: return
        
        toyController.getShopToys(
            scope = lifecycleScope,
            currentUserId = userId,
            cursor = cursor,
            limit = 20,
            callback = object : ToyController.ToysListCallback {
                override fun onLoading() {
                    isLoading = true
                    if (cursor == null) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvShopToys.visibility = View.GONE
                        binding.tvEmpty.visibility = View.GONE
                    }
                }
                
                override fun onSuccess(newToys: List<Toy>, nextCursor: String?) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    
                    if (cursor == null) {
                        // Первая загрузка - заменяем список
                        toys.clear()
                    }
                    
                    toys.addAll(newToys)
                    toysAdapter.submitList(toys.toList())
                    
                    // Обновляем курсор
                    currentCursor = nextCursor
                    hasMorePages = nextCursor != null
                    
                    // Показываем соответствующий UI
                    if (toys.isEmpty()) {
                        binding.rvShopToys.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvShopToys.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }
    }
    
                override fun onError(message: String) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                    
                    if (cursor == null && toys.isEmpty()) {
                        binding.rvShopToys.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        )
    }
    
    private fun openShopToyDetails(toy: Toy) {
        // Открываем детали игрушки из магазина (только просмотр + кнопка обмена)
        val detailFragment = ShopToyDetailFragment.newInstance(toy.toyId)
        parentFragmentManager.beginTransaction()
            .replace(parentFragmentManager.findFragmentById(R.id.navHostFragment)?.id ?: 0, detailFragment)
            .addToBackStack(null)
            .commit()
    }
    
    /**
     * Обновление списка (вызывается после успешного обмена)
     */
    fun refreshShopToys() {
        currentCursor = null
        hasMorePages = true
        toys.clear()
        toysAdapter.submitList(null) {
            loadShopToys()
        }
    }
}

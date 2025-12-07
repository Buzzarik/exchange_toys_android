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
import course.exchange_toy.databinding.FragmentMyToysBinding
import course.exchange_toy.models.remote.models.Toy
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast

/**
 * Фрагмент "Мои игрушки"
 * Отображает список игрушек пользователя с пагинацией по курсору
 */
class MyToysFragment : BaseFragment<FragmentMyToysBinding>() {
    
    private lateinit var sessionManager: UserSession
    private lateinit var toyController: ToyController
    private lateinit var toysAdapter: ToysAdapter
    
    private val toys = mutableListOf<Toy>()
    private var currentCursor: String? = null
    private var isLoading = false
    private var hasMorePages = true
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMyToysBinding {
        return FragmentMyToysBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = UserSession(requireContext())
        
        // Создаем клиент и контроллер
        val client = sessionManager.createApiClient()
        toyController = ToyController(client)
        
        setupRecyclerView()
        setupFab()
        loadToys()
    }
    
    private fun setupRecyclerView() {
        toysAdapter = ToysAdapter { toy ->
            // При клике на "Детали" открываем детальный экран
            openToyDetails(toy)
        }
        
        binding.rvToys.apply {
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
                    
                    // Загружаем следующую страницу, когда достигли конца списка
                    if (!isLoading && hasMorePages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadToys(currentCursor)
                        }
                    }
                }
            })
        }
    }
    
    private fun setupFab() {
        binding.fabAddToy.setOnClickListener {
            // Открываем диалог создания игрушки
            ToyFormDialog.newInstance(onSuccess = {
                refreshToys() // Обновляем список после создания
            }).show(parentFragmentManager, "ToyFormDialog")
        }
    }
    
    /**
     * Загрузка игрушек с курсором
     */
    private fun loadToys(cursor: String? = null) {
        val userId = sessionManager.getUserId() ?: return
        
        toyController.getMyToys(
            scope = lifecycleScope,
            userId = userId,
            cursor = cursor,
            limit = 20,
            callback = object : ToyController.ToysListCallback {
                override fun onLoading() {
                    isLoading = true
                    if (cursor == null) {
                        // Первая загрузка
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvToys.visibility = View.GONE
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
                        binding.rvToys.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvToys.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }
                }
                
                override fun onError(message: String) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                    
                    // Если первая загрузка не удалась, показываем пустой экран
                    if (cursor == null && toys.isEmpty()) {
                        binding.rvToys.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        )
    }
    
    private fun openToyDetails(toy: Toy) {
        // Открываем детальный экран
        val detailFragment = ToyDetailFragment.newInstance(toy.toyId)
        parentFragmentManager.beginTransaction()
            .replace(parentFragmentManager.findFragmentById(R.id.navHostFragment)?.id ?: 0, detailFragment)
            .addToBackStack(null)
            .commit()
    }
    
    /**
     * Обновление списка (например, после создания/удаления игрушки)
     */
    fun refreshToys() {
        currentCursor = null
        hasMorePages = true
        toys.clear() // Очищаем старые данные
        toysAdapter.submitList(null) { // Очищаем адаптер с callback
            loadToys()
        }
    }
}


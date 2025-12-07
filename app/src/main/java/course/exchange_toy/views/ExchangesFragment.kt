package course.exchange_toy.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import course.exchange_toy.R
import course.exchange_toy.controller.ExchangeController
import course.exchange_toy.databinding.FragmentExchangesBinding
import course.exchange_toy.models.remote.models.ExchangeInfo
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast

/**
 * Фрагмент "Обмены"
 * Отображает список текущих обменов пользователя
 */
class ExchangesFragment : BaseFragment<FragmentExchangesBinding>() {
    
    private lateinit var sessionManager: UserSession
    private lateinit var exchangeController: ExchangeController
    private lateinit var exchangesAdapter: ExchangesAdapter
    
    private val exchanges = mutableListOf<ExchangeInfo>()
    private var currentCursor: String? = null
    private var isLoading = false
    private var hasMorePages = true
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentExchangesBinding {
        return FragmentExchangesBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = UserSession(requireContext())
        
        // Создаем клиент и контроллер
        val client = sessionManager.createApiClient()
        exchangeController = ExchangeController(client)
        
        setupRecyclerView()
        loadExchanges()
    }
    
    private fun setupRecyclerView() {
        exchangesAdapter = ExchangesAdapter { exchange ->
            // Открываем детали обмена
            openExchangeDetails(exchange)
        }
        
        binding.rvExchanges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exchangesAdapter
            
            // Пагинация при прокрутке
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    if (!isLoading && hasMorePages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadExchanges(currentCursor)
                        }
                    }
                }
            })
        }
    }
    
    private fun loadExchanges(cursor: String? = null) {
        val userId = sessionManager.getUserId() ?: return
        
        exchangeController.getMyExchanges(
            scope = lifecycleScope,
            userId = userId,
            cursor = cursor,
            limit = 20,
            callback = object : ExchangeController.ExchangeListCallback {
                override fun onLoading() {
                    isLoading = true
                    if (cursor == null) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvExchanges.visibility = View.GONE
                        binding.tvEmpty.visibility = View.GONE
                    }
    }
    
                override fun onSuccess(newExchanges: List<ExchangeInfo>, nextCursor: String?) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    
                    if (cursor == null) {
                        exchanges.clear()
                    }
                    
                    exchanges.addAll(newExchanges)
                    exchangesAdapter.submitList(exchanges.toList())
                    
                    currentCursor = nextCursor
                    hasMorePages = nextCursor != null
                    
                    if (exchanges.isEmpty()) {
                        binding.rvExchanges.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvExchanges.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }
                }
                
                override fun onError(message: String) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    showLongToast(message)
                    
                    if (cursor == null && exchanges.isEmpty()) {
                        binding.rvExchanges.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        )
    }
    
    private fun openExchangeDetails(exchange: ExchangeInfo) {
        val detailFragment = ExchangeDetailFragment.newInstance(exchange.exchangeId)
        parentFragmentManager.beginTransaction()
            .replace(parentFragmentManager.findFragmentById(R.id.navHostFragment)?.id ?: 0, detailFragment)
            .addToBackStack(null)
            .commit()
    }
    
    /**
     * Обновить список (после изменения статуса обмена)
     */
    fun refreshExchanges() {
        currentCursor = null
        hasMorePages = true
        exchanges.clear()
        exchangesAdapter.submitList(null) {
            loadExchanges()
        }
    }
}


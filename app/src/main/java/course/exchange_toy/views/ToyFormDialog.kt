package course.exchange_toy.views

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import course.exchange_toy.controller.ToyController
import course.exchange_toy.databinding.DialogToyFormBinding
import course.exchange_toy.models.user.UserSession
import course.exchange_toy.utils.createApiClient
import course.exchange_toy.utils.showLongToast
import course.exchange_toy.utils.showToast
import java.io.File
import java.io.FileOutputStream

/**
 * Диалог создания/редактирования игрушки
 */
class ToyFormDialog : DialogFragment() {
    
    private var _binding: DialogToyFormBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionManager: UserSession
    private lateinit var toyController: ToyController
    
    private var selectedPhotoUri: Uri? = null
    private var selectedPhotoFile: File? = null
    
    private var toyId: String? = null // null = создание, не null = редактирование
    private var onSuccess: (() -> Unit)? = null
    
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            selectedPhotoFile = copyUriToFile(it)
            binding.tvSelectedFile.text = "Файл выбран: ${it.lastPathSegment}"
        }
    }
    
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
        _binding = DialogToyFormBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = UserSession(requireContext())
        
        // Создаем клиент и контроллер
        val client = sessionManager.createApiClient()
        toyController = ToyController(client)
        
        // Получаем аргументы
        toyId = arguments?.getString(ARG_TOY_ID)
        
        // Устанавливаем заголовок
        binding.tvTitle.text = if (toyId == null) "Создать игрушку" else "Редактировать игрушку"
        
        setupButtons()
        
        // TODO: Если это редактирование, загрузить данные игрушки
    }
    
    private fun setupButtons() {
        binding.btnSelectPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveToy()
        }
    }
    
    private fun saveToy() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim().takeIf { it.isNotEmpty() }
        
        // Валидация
        if (name.isEmpty()) {
            showToast("Введите название")
            return
        }
        
        val userId = sessionManager.getUserId() ?: return
        
        if (toyId == null) {
            // Создание новой игрушки
            createToy(userId, name, description)
        } else {
            // Редактирование существующей
            updateToy(toyId!!, name, description)
        }
    }
    
    private fun createToy(userId: String, name: String, description: String?) {
        toyController.createToy(
            scope = lifecycleScope,
            userId = userId,
            name = name,
            description = description,
            photoFile = selectedPhotoFile,
            callback = object : ToyController.ToyCallback {
                override fun onLoading() {
                    setLoading(true)
                }
                
                override fun onSuccess() {
                    setLoading(false)
                    showToast("Игрушка создана!")
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
    
    private fun updateToy(toyId: String, name: String, description: String?) {
        val userId = sessionManager.getUserId() ?: return
        
        toyController.updateToy(
            scope = lifecycleScope,
            userId = userId,
            toyId = toyId,
            name = name,
            description = description,
            photoFile = selectedPhotoFile,
            callback = object : ToyController.ToyCallback {
                override fun onLoading() {
                    setLoading(true)
                }
                
                override fun onSuccess() {
                    setLoading(false)
                    showToast("Игрушка обновлена!")
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
        binding.btnSave.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
        binding.etName.isEnabled = !isLoading
        binding.etDescription.isEnabled = !isLoading
        binding.btnSelectPhoto.isEnabled = !isLoading
    }
    
    private fun copyUriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_TOY_ID = "toy_id"
        
        fun newInstance(toyId: String? = null, onSuccess: () -> Unit): ToyFormDialog {
            return ToyFormDialog().apply {
                arguments = Bundle().apply {
                    toyId?.let { putString(ARG_TOY_ID, it) }
                }
                this.onSuccess = onSuccess
            }
        }
    }
}


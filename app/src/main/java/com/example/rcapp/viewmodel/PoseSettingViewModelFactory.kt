import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.model.PoseLandmarkerHelper
import com.example.rcapp.viewmodel.PoseSettingViewModel

/**
 * 构造方法，用于创建能够传递poseLandmarkerHelper实例的ViewModel
 */
class PoseSettingViewModelFactory(
    private val model: PoseLandmarkerHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoseSettingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PoseSettingViewModel(model) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
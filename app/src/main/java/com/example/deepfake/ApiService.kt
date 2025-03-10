import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/predict/image")
    fun predictImage(@Part file: MultipartBody.Part): Call<ResponseBody>

    @Multipart
    @POST("/predict/video")
    fun predictVideo(@Part file: MultipartBody.Part): Call<ResponseBody>

    @Multipart
    @POST("/predict/audio")
    fun predictAudio(@Part file: MultipartBody.Part): Call<ResponseBody>
}

package com.example.deepfake
import ApiService
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var loadingOverlay: RelativeLayout
    private val PICK_FILE_REQUEST_CODE = 1
    private lateinit var endpoint: String
    private var fileUri: Uri? = null

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://bccd-182-48-224-201.ngrok-free.app")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSelectImage: Button = findViewById(R.id.btnSelectImage)
        val btnSelectVideo: Button = findViewById(R.id.btnSelectVideo)
        val btnSelectAudio: Button = findViewById(R.id.btnSelectAudio)
        tvResult = findViewById(R.id.tvResult)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        btnSelectImage.setOnClickListener { selectFile("image/*", "/predict/image") }
        btnSelectVideo.setOnClickListener { selectFile("video/*", "/predict/video") }
        btnSelectAudio.setOnClickListener { selectFile("audio/*", "/predict/audio") }
    }

    private fun selectFile(type: String, endpoint: String) {
        this.endpoint = endpoint
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = type
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data
            fileUri?.let { uploadFile(it, endpoint) }
        }
    }

    private fun uploadFile(fileUri: Uri, endpoint: String) {
        val file = uriToFile(fileUri)
        val requestFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        // Show loading overlay
        loadingOverlay.visibility = View.VISIBLE

        val call = when (endpoint) {
            "/predict/image" -> apiService.predictImage(body)
            "/predict/video" -> apiService.predictVideo(body)
            "/predict/audio" -> apiService.predictAudio(body)
            else -> null
        }

        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                loadingOverlay.visibility = View.GONE // Hide loading overlay
                response.body()?.let {
                    val result = it.string()
                    tvResult.text = "Result: $result"
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                loadingOverlay.visibility = View.GONE // Hide loading overlay
                Log.e("MainActivity", "Error: ${t.message}")
                tvResult.text = "Error: ${t.message}"
            }
        })
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".tmp", cacheDir)
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}

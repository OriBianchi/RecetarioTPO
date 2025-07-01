package com.example.desarrollotpo.presentation.VerReceta

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class RecetaDetalleViewModel : ViewModel() {

    val recetaLiveData = MutableLiveData<JSONObject>()
    val isSaved = MutableLiveData<Boolean>()

    fun cargarReceta(id: String, token: String) {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/$id"
        val request = Request.Builder().url(url).get()
        if (token.isNotEmpty()) request.header("Authorization", "Bearer $token")
        client.newCall(request.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                recetaLiveData.postValue(json)
                isSaved.postValue(json.optBoolean("isSaved", false))
            }
        })
    }

    fun guardarReceta(id: String, token: String) {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/user/recipes/save"
        val body = FormBody.Builder().add("recipeId", id).build()
        val request = Request.Builder().url(url).post(body)
        if (token.isNotEmpty()) request.header("Authorization", "Bearer $token")
        client.newCall(request.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                isSaved.postValue(true)
            }
        })
    }

    fun desguardarReceta(id: String, token: String) {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/user/recipes/unsave"
        val body = FormBody.Builder().add("recipeId", id).build()
        val request = Request.Builder().url(url).post(body)
        if (token.isNotEmpty()) request.header("Authorization", "Bearer $token")
        client.newCall(request.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                isSaved.postValue(false)
            }
        })
    }
}

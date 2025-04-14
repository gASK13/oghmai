package net.gask13.oghmai.network

import net.gask13.oghmai.model.DescriptionRequest
import net.gask13.oghmai.model.WordList
import net.gask13.oghmai.model.WordResult
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("describe-word")
    suspend fun describeWord(@Body word: DescriptionRequest): WordResult

    @DELETE("words")
    suspend fun purgeWords()

    @POST("save-word")
    suspend fun saveWord(@Body word: WordResult)

    @GET("words")
    suspend fun getWords(): WordList

    @GET("word/{word}")
    suspend fun getWord(@Path("word") word: String): WordResult

    @DELETE("word/{word}")
    suspend fun deleteWord(@Path("word") word: String)
}
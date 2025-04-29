package net.gask13.oghmai.network

import net.gask13.oghmai.model.DescriptionRequest
import net.gask13.oghmai.model.TestChallenge
import net.gask13.oghmai.model.TestResult
import net.gask13.oghmai.model.TestStatistics
import net.gask13.oghmai.model.WordList
import net.gask13.oghmai.model.WordResult
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("describe-word")
    suspend fun describeWord(@Body word: DescriptionRequest): Response<WordResult>

    @DELETE("words")
    suspend fun purgeWords()

    @POST("save-word")
    suspend fun saveWord(@Body word: WordResult)

    // Status is a comma-delimited list of WordStatus values
    @GET("words")
    suspend fun getWords(@Query("status") status: String? = null, @Query("failed_last_test") failedLastTest: Boolean? = null, @Query("contains") contains: String? = null ): WordList

    @GET("word/{word}")
    suspend fun getWord(@Path("word") word: String): WordResult

    @DELETE("word/{word}")
    suspend fun deleteWord(@Path("word") word: String)

    @PATCH("word/{word}")
    suspend fun undoDeleteWord(@Path("word") word: String, @Query("action") action: String = "undelete")

    @GET("test")
    suspend fun getAvailableTests(): TestStatistics

    @GET("test/next")
    suspend fun getNextTest(): Response<TestChallenge>

    @PUT("test/{id}")
    suspend fun submitChallengeGuess(
        @Path("id") id: String,
        @Query("guess") guess: String
    ): TestResult
}
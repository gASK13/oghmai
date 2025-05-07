package net.gask13.oghmai.auth

import android.content.Context
import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserState
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.config.AWSConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine
import net.gask13.oghmai.BuildConfig
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manager class for handling authentication with AWS Cognito
 */
object AuthManager {

    private const val TAG = "AuthManager"

    /**
     * Initialize the AWS Mobile Client
     */
    suspend fun initialize(context : Context): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Get Cognito configuration from BuildConfig
            val poolId = BuildConfig.COGNITO_POOL_ID
            val clientId = BuildConfig.COGNITO_CLIENT_ID
            val region = BuildConfig.COGNITO_REGION

            // Log the configuration values
            Log.d(TAG, "Using Cognito configuration from BuildConfig")
            Log.d(TAG, "Region: $region, Pool ID: $poolId, Client ID: $clientId")
            val configJson = getCognitoConfigJson(poolId, clientId, region)

            // Create AWSConfiguration from the JSON object
            val awsConfiguration = AWSConfiguration(configJson)

            // Initialize with the configuration
            AWSMobileClient.getInstance().initialize(context, awsConfiguration, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails) {
                    Log.i(TAG, "AWSMobileClient initialized. User State: ${result.userState}")
                    continuation.resume(true)
                }

                override fun onError(e: Exception) {
                    Log.e(TAG, "Error initializing AWSMobileClient", e)
                    continuation.resumeWithException(e)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in initialize", e)
            continuation.resumeWithException(e)
        }
    }

    private fun getCognitoConfigJson(poolId: String, clientId: String, region: String): JSONObject {
        // Create a JSON configuration object
        val cognitoConfig = JSONObject().apply {
            put("PoolId", poolId)
            put("AppClientId", clientId)
            put("Region", region)
        }

        val defaultCognitoConfig = JSONObject().apply {
            put("Default", cognitoConfig)
        }

        val identityManager = JSONObject().apply {
            put("Default", JSONObject())
        }

        val configJson = JSONObject().apply {
            put("IdentityManager", identityManager)
            put("CognitoUserPool", defaultCognitoConfig)
        }
        return configJson
    }

    /**
     * Check if a user is signed in
     */
    fun isSignedIn(): Boolean {
        return try {
            val userState = AWSMobileClient.getInstance().currentUserState().userState
            userState == UserState.SIGNED_IN
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sign-in status", e)
            false
        }
    }

    /**
     * Get current username
     */
    fun getCurrentUsername(): String? {
        return try {
            if (isSignedIn()) {
                AWSMobileClient.getInstance().username
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current username", e)
            null
        }
    }

    /**
     * Sign in with username and password
     */
    suspend fun signIn(username: String, password: String): SignInResult = suspendCancellableCoroutine { continuation ->
        try {
            AWSMobileClient.getInstance().signIn(username, password, null, object : Callback<SignInResult> {
                override fun onResult(result: SignInResult) {
                    Log.i(TAG, "Sign-in result: ${result.signInState}")
                    continuation.resume(result)
                }

                override fun onError(e: Exception) {
                    Log.e(TAG, "Error during sign-in", e)
                    continuation.resumeWithException(e)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in signIn", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Simple sign out without options
            AWSMobileClient.getInstance().signOut()
            Log.i(TAG, "Sign-out completed")
            continuation.resume(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signOut", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Get the current authentication token
     * @return The JWT token or null if not signed in or error occurs
     */
    fun getAuthToken(): String? {
        return try {
            if (isSignedIn()) {
                val tokens = AWSMobileClient.getInstance().tokens
                tokens.idToken.tokenString
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting authentication token", e)
            null
        }
    }
}

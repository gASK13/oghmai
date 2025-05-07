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
     * Initialize the AWS Mobile Client and Credentials Manager
     * This method will also attempt to restore the previous authentication state
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

                    // Check if we have a persisted session
                    if (result.userState == UserState.SIGNED_IN) {
                        Log.i(TAG, "User is already signed in from a previous session")

                        // Refresh tokens if needed
                        refreshTokensIfNeeded()
                    } else {
                        Log.i(TAG, "No previous session found, user needs to sign in")
                    }

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

    fun isSignedIn(): Boolean {
        return try {
            val userState = AWSMobileClient.getInstance().currentUserState().userState
            userState == UserState.SIGNED_IN
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sign-in status", e)
            false
        }
    }

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

    suspend fun signIn(username: String, password: String): SignInResult = suspendCancellableCoroutine { continuation ->
        try {
            AWSMobileClient.getInstance().signIn(username, password, null, object : Callback<SignInResult> {
                override fun onResult(result: SignInResult) {
                    Log.i(TAG, "Sign-in result: ${result.signInState}")

                    // The AWSMobileClient already persists the session by default
                    // We just need to make sure it's refreshed
                    refreshTokensIfNeeded()

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

    private fun refreshTokensIfNeeded() {
        try {
            if (isSignedIn()) {
                // Get current tokens - this will automatically refresh if needed
                AWSMobileClient.getInstance().tokens
                Log.d(TAG, "Tokens refreshed if needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing tokens", e)
        }
    }

    fun validateSession(): Boolean {
        return try {
            if (isSignedIn()) {
                refreshTokensIfNeeded()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating session", e)
            false
        }
    }
}

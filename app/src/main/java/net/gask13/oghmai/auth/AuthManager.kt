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
class AuthManager private constructor(private val context: Context) {

    private val tag = "AuthManager"
    private val awsMobileClient = AWSMobileClient.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Initialize the AWS Mobile Client
     */
    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Get Cognito configuration from BuildConfig
            val poolId = BuildConfig.COGNITO_POOL_ID
            val clientId = BuildConfig.COGNITO_CLIENT_ID
            val region = BuildConfig.COGNITO_REGION

            // Log the configuration values
            Log.d(tag, "Using Cognito configuration from BuildConfig")
            Log.d(tag, "Region: $region, Pool ID: $poolId, Client ID: $clientId")

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

            // Create AWSConfiguration from the JSON object
            val awsConfiguration = AWSConfiguration(configJson)

            // Initialize with the configuration
            awsMobileClient.initialize(context, awsConfiguration, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails) {
                    Log.i(tag, "AWSMobileClient initialized. User State: ${result.userState}")
                    continuation.resume(true)
                }

                override fun onError(e: Exception) {
                    Log.e(tag, "Error initializing AWSMobileClient", e)
                    continuation.resumeWithException(e)
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Error in initialize", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Check if a user is signed in
     */
    fun isSignedIn(): Boolean {
        return try {
            val userState = awsMobileClient.currentUserState().userState
            userState == UserState.SIGNED_IN
        } catch (e: Exception) {
            Log.e(tag, "Error checking sign-in status", e)
            false
        }
    }

    /**
     * Get current username
     */
    fun getCurrentUsername(): String? {
        return try {
            if (isSignedIn()) {
                awsMobileClient.username
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting current username", e)
            null
        }
    }

    /**
     * Sign in with username and password
     */
    suspend fun signIn(username: String, password: String): SignInResult = suspendCancellableCoroutine { continuation ->
        try {
            awsMobileClient.signIn(username, password, null, object : Callback<SignInResult> {
                override fun onResult(result: SignInResult) {
                    Log.i(tag, "Sign-in result: ${result.signInState}")
                    continuation.resume(result)
                }

                override fun onError(e: Exception) {
                    Log.e(tag, "Error during sign-in", e)
                    continuation.resumeWithException(e)
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Error in signIn", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Simple sign out without options
            awsMobileClient.signOut()
            Log.i(tag, "Sign-out completed")
            continuation.resume(true)
        } catch (e: Exception) {
            Log.e(tag, "Error in signOut", e)
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
                val tokens = awsMobileClient.tokens
                tokens.idToken.tokenString
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting authentication token", e)
            null
        }
    }
}

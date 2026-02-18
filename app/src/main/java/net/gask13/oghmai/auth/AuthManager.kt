package net.gask13.oghmai.auth

import android.content.Context
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Amplify
import kotlinx.coroutines.suspendCancellableCoroutine
import net.gask13.oghmai.BuildConfig
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manager class for handling authentication with AWS Amplify Cognito.
 */
object AuthManager {

    private const val TAG = "AuthManager"

    @Volatile
    private var initialized = false

    /**
     * Initialize Amplify and restore persisted authentication state.
     */
    suspend fun initialize(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        if (initialized) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        try {
            val configJson = getAmplifyConfigJson(
                poolId = BuildConfig.COGNITO_POOL_ID,
                clientId = BuildConfig.COGNITO_CLIENT_ID,
                region = BuildConfig.COGNITO_REGION
            )

            Log.d(TAG, "Configuring Amplify Auth with BuildConfig Cognito values")
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(configJson, context.applicationContext)
            initialized = true

            Amplify.Auth.fetchAuthSession(
                {
                    Log.i(TAG, "Amplify initialized and auth session fetched")
                    continuation.resume(true)
                },
                { error ->
                    Log.e(TAG, "Failed to fetch auth session after Amplify init", error)
                    continuation.resumeWithException(error)
                }
            )
        } catch (alreadyConfigured: AmplifyException) {
            Log.w(TAG, "Amplify already configured, continuing", alreadyConfigured)
            initialized = true
            continuation.resume(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Amplify", e)
            continuation.resumeWithException(e)
        }
    }

    private fun getAmplifyConfigJson(poolId: String, clientId: String, region: String): JSONObject {
        val userPool = JSONObject().apply {
            put("PoolId", poolId)
            put("AppClientId", clientId)
            put("Region", region)
        }

        val pluginConfig = JSONObject().apply {
            put("CognitoUserPool", JSONObject().put("Default", userPool))
            put("Auth", JSONObject().put("Default", JSONObject()))
        }

        return JSONObject().apply {
            put(
                "auth",
                JSONObject().put(
                    "plugins",
                    JSONObject().put("awsCognitoAuthPlugin", pluginConfig)
                )
            )
        }
    }

    fun isSignedIn(): Boolean {
        return try {
            fetchAuthSessionSync()?.isSignedIn == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sign-in status", e)
            false
        }
    }

    fun getCurrentUsername(): String? {
        return try {
            val user: AuthUser = Amplify.Auth.getCurrentUser()
            user.username
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current username", e)
            null
        }
    }

    suspend fun signIn(username: String, password: String): Boolean = suspendCancellableCoroutine { continuation ->
        Amplify.Auth.signIn(
            username,
            password,
            AuthSignInOptions.defaults(),
            { result ->
                val signInComplete = result.isSignedIn && result.nextStep.signInStep == AuthSignInStep.DONE
                Log.i(TAG, "Sign-in completed=$signInComplete, nextStep=${result.nextStep.signInStep}")
                continuation.resume(signInComplete)
            },
            { error ->
                Log.e(TAG, "Error during sign-in", error)
                continuation.resumeWithException(error)
            }
        )
    }

    suspend fun signOut(): Boolean = suspendCancellableCoroutine { continuation ->
        Amplify.Auth.signOut { result ->
            val complete = result is AuthSignOutResult.CompleteSignOut
            Log.i(TAG, "Sign-out completed=$complete")
            continuation.resume(complete)
        }
    }

    fun getAuthToken(): String? {
        return try {
            val authSession = fetchAuthSessionSync() as? AWSCognitoAuthSession
            authSession?.userPoolTokensResult?.value?.idToken
        } catch (e: Exception) {
            Log.e(TAG, "Error getting authentication token", e)
            null
        }
    }

    fun validateSession(): Boolean {
        return try {
            isSignedIn()
        } catch (e: Exception) {
            Log.e(TAG, "Error validating session", e)
            false
        }
    }

    private fun fetchAuthSessionSync(): AuthSession? {
        val latch = CountDownLatch(1)
        val sessionRef = AtomicReference<AuthSession?>(null)
        val errorRef = AtomicReference<Throwable?>(null)

        Amplify.Auth.fetchAuthSession(
            { session ->
                sessionRef.set(session)
                latch.countDown()
            },
            { error ->
                errorRef.set(error)
                latch.countDown()
            }
        )

        latch.await()
        errorRef.get()?.let { throw it }
        return sessionRef.get()
    }
}

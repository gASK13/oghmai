# Android Migration Plan: AWS SDK for Android → AWS Amplify for Android

> Primary source of truth: **Mobile SDK to Amplify Android Migration Guide**.

## 1) Dependency updates

### What to remove (legacy)
From `app/build.gradle.kts` / version catalog, remove legacy Mobile SDK artifacts:

- `com.amazonaws:aws-android-sdk-mobile-client`
- `com.amazonaws:aws-android-sdk-cognitoidentityprovider`

### What to add (Amplify + AWS SDK for Kotlin)
Add these modern dependencies:

- `com.amplifyframework:core`
- `com.amplifyframework:aws-auth-cognito`
- `aws.sdk.kotlin:s3` (for direct Kotlin SDK usage when needed)
- `aws.sdk.kotlin:cognitoidentityprovider` (for service-level calls when needed)

This repo now keeps these in `gradle/libs.versions.toml` and consumes them from `app/build.gradle.kts`.

---

## 2) Configuration updates

## Old pattern
- Legacy `AWSMobileClient` consumed `awsconfiguration.json` format (or runtime-built `AWSConfiguration`).

## New pattern
- Amplify expects `amplifyconfiguration.json` shape.
- In this codebase, we build the Amplify configuration JSON from `BuildConfig` values at runtime (for environment-specific settings) and pass it to `Amplify.configure(...)`.

### Example `amplifyconfiguration.json` (reference format)
```json
{
  "auth": {
    "plugins": {
      "awsCognitoAuthPlugin": {
        "CognitoUserPool": {
          "Default": {
            "PoolId": "<user_pool_id>",
            "AppClientId": "<app_client_id>",
            "Region": "<region>"
          }
        },
        "Auth": {
          "Default": {}
        }
      }
    }
  }
}
```

---

## 3) Code migration examples (old → new)

## A. Initialize auth

### Old (AWSMobileClient)
```kotlin
AWSMobileClient.getInstance().initialize(context, awsConfiguration, callback)
```

### New (Amplify)
```kotlin
Amplify.addPlugin(AWSCognitoAuthPlugin())
Amplify.configure(amplifyConfigJson, context)
Amplify.Auth.fetchAuthSession(onSuccess = { /* signed-in state */ }, onError = { /* handle */ })
```

## B. Sign in with Cognito user pool

### Old
```kotlin
AWSMobileClient.getInstance().signIn(username, password, null, callback)
```

### New
```kotlin
Amplify.Auth.signIn(
    username,
    password,
    AuthSignInOptions.defaults(),
    { result ->
        val done = result.isSignedIn && result.nextStep.signInStep == AuthSignInStep.DONE
    },
    { error -> /* handle */ }
)
```

## C. Get session/token for API Authorization header

### Old
```kotlin
val idToken = AWSMobileClient.getInstance().tokens.idToken.tokenString
```

### New
```kotlin
val authSession = KotlinAmplify.Auth.fetchAuthSession() as AWSCognitoAuthSession
val idToken = authSession.userPoolTokensResult.value.idToken
```

## D. API call example (Amplify API plugin)

> This app currently uses Retrofit to call backend APIs. If you migrate API calls to Amplify API plugin, the style becomes:

```kotlin
Amplify.API.get(
    "/v1/words",
    { response -> Log.i("API", response.data.asString()) },
    { error -> Log.e("API", "Failed", error) }
)
```

## E. S3 storage example

### Old (legacy TransferUtility style)
```kotlin
// transferUtility.upload(bucket, key, file)
```

### New (Amplify Storage plugin style)
```kotlin
Amplify.Storage.uploadFile(
    "user-uploads/image.jpg",
    file,
    { result -> Log.i("Storage", "Uploaded: ${result.key}") },
    { error -> Log.e("Storage", "Upload failed", error) }
)
```

### Alternative with AWS SDK for Kotlin (direct service client)
```kotlin
val s3 = S3Client { region = "eu-central-1" }
s3.putObject {
    bucket = "my-bucket"
    key = "user-uploads/image.jpg"
    body = ByteStream.fromBytes(bytes)
}
```

---

## 4) Testing considerations

1. **Auth lifecycle tests**
   - Initialize Amplify once per process.
   - Verify persisted session restore after app restart.
   - Verify sign-in/sign-out behavior and error handling.

2. **Token propagation tests**
   - Confirm Retrofit interceptor adds `Authorization: Bearer <idToken>` when signed in.
   - Confirm anonymous behavior when signed out.

3. **Instrumentation tests**
   - Login screen: valid and invalid credentials.
   - Navigation guards based on auth state.

4. **Smoke test matrix**
   - Fresh install, cold start, cached session, expired token, network loss.

5. **Regression checks**
   - Existing backend calls still function with new token source.
   - No duplicate Amplify initialization exceptions in lifecycle edge cases.

---

## 5) Suggested rollout sequence

1. Migrate dependencies and compile.
2. Replace `AWSMobileClient` auth layer with Amplify Auth.
3. Verify token-dependent API flows.
4. Migrate storage/API components incrementally if/when adopted.
5. Remove any remaining legacy AWS Mobile SDK references.

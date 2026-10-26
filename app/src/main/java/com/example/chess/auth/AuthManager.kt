package com.example.chess.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.json.JSONObject

data class GoogleAccount(
    val uid: String,    // Google's permanent user id ("sub")
    val email: String,
    val name: String
)

sealed class SignInResult {
    data class Success(val account: GoogleAccount) : SignInResult()
    data class Failure(val message: String) : SignInResult()
    object Cancelled : SignInResult()
}

/**
 * "Continue with Google" without Firebase Auth. The phone's Google account picker gives us a
 * signed ID token; we only use who the person is (id, e-mail, name).
 */
class AuthManager(context: Context) {

    private val prefs = context.getSharedPreferences("checkmate_auth", Context.MODE_PRIVATE)

    /** True once the person chose "guest" or "Google" on the first-launch screen. */
    var welcomeDone: Boolean
        get() = prefs.getBoolean("welcome_done", false)
        set(value) { prefs.edit().putBoolean("welcome_done", value).apply() }

    /** How many times the name was changed (max 2 per person). */
    var nameChangesUsed: Int
        get() = prefs.getInt("name_changes_used", 0)
        set(value) { prefs.edit().putInt("name_changes_used", value).apply() }

    fun savedAccount(): GoogleAccount? {
        val uid = prefs.getString("uid", null) ?: return null
        return GoogleAccount(
            uid = uid,
            email = prefs.getString("email", "") ?: "",
            name = prefs.getString("name", "") ?: ""
        )
    }

    fun saveAccount(account: GoogleAccount) {
        prefs.edit()
            .putString("uid", account.uid)
            .putString("email", account.email)
            .putString("name", account.name)
            .apply()
    }

    fun clearAccount() {
        prefs.edit().remove("uid").remove("email").remove("name").apply()
    }

    private fun webClientId(): String {
        val v = BuildConfig.GOOGLE_WEB_CLIENT_ID
        return if (v.isBlank() || v.startsWith("YOUR_")) "" else v
    }

    val isConfigured: Boolean get() = webClientId().isNotEmpty()

    suspend fun signInWithGoogle(activity: Activity): SignInResult {
        val webId = webClientId()
        if (webId.isEmpty()) {
            return SignInResult.Failure("Google login abhi set nahi hai (GOOGLE_WEB_CLIENT_ID missing).")
        }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val result = CredentialManager.create(activity).getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val uid = subFromIdToken(google.idToken) ?: google.id
                val name = google.displayName
                    ?: google.givenName
                    ?: google.id.substringBefore('@')
                SignInResult.Success(GoogleAccount(uid = uid, email = google.id, name = name))
            } else {
                SignInResult.Failure("Google se samajh me na aane wala jawab aaya.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Failure("Is phone me Google account nahi mila. Settings me Gmail add karo.")
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Google sign-in failed", e)
            SignInResult.Failure("Google login fail hua (${e.type}). Setup check karo: ${e.message ?: ""}")
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("AuthManager", "Bad Google token", e)
            SignInResult.Failure("Google token padh nahi paya.")
        } catch (e: Exception) {
            Log.e("AuthManager", "Google sign-in error", e)
            SignInResult.Failure("Google login me dikkat: ${e.message ?: "unknown"}")
        }
    }

    private fun subFromIdToken(idToken: String): String? = try {
        val payload = idToken.split('.')[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        JSONObject(json).optString("sub").ifEmpty { null }
    } catch (e: Exception) {
        null
    }
}

/** Compose gives a wrapped Context; the Credential Manager needs the real Activity. */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

package com.rncoding.testvineshield.core.domain.auth

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError

/**
 * Global authentication/session state of the application.
 *
 * This is NOT the state of the login form.
 *
 * AuthState answers:
 *
 * "Can the user currently access the authenticated
 * part of the application?"
 */
sealed interface AuthState {

    /**
     * The application is determining whether an existing
     * session can be restored.
     *
     * This should normally be the initial state.
     */
    data object Loading : AuthState

    /**
     * There is no usable authenticated session.
     */
    data class Unauthenticated(
        val reason: Reason
    ) : AuthState {

        enum class Reason {

            /**
             * No previous session exists.
             *
             * Typical first application launch.
             */
            NotLoggedIn,

            /**
             * User explicitly logged out.
             */
            LoggedOut,

            /**
             * The session reached its maximum lifetime
             * or otherwise became invalid.
             */
            SessionExpired
        }
    }

    /**
     * The user has a valid authenticated session and
     * can access the application.
     */
    data class Authenticated(
        val user: UserDomainModel
    ) : AuthState

    /**
     * A valid session still exists, but the application
     * is temporarily locked and requires local
     * authentication.
     */
    data class Locked(
        val reason: LockReason
    ) : AuthState {

        enum class LockReason {

            /**
             * The inactivity timeout was reached.
             */
            InactivityTimeout,

            /**
             * The user has enabled "require authentication
             * when the application starts".
             */
            AppLaunchRequiresUnlock
        }
    }

    /**
     * An unrecoverable authentication/session operation
     * failed.
     */
    data class Error(
        val error: AppError
    ) : AuthState
}
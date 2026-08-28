package com.rncoding.testvineshield.core.domain.security

class ValidateEmail {
    operator fun invoke(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}
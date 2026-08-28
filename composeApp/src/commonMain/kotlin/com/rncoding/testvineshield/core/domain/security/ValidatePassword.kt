package com.rncoding.testvineshield.core.domain.security

class ValidatePassword {
    operator fun invoke(password: String): Boolean {
        return password.length >= 8
    }
}
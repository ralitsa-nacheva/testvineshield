package com.rncoding.testvineshield.core.data.security

import com.rncoding.testvineshield.core.domain.security.PasswordHasher

class BCryptPasswordHasher : PasswordHasher {

    override fun generateSalt(): String {
        return org.mindrot.jbcrypt.BCrypt.gensalt()
    }

    override fun hash(password: String, salt: String): String {
        return org.mindrot.jbcrypt.BCrypt.hashpw(password, salt)
    }

    override fun verify(password: String, salt: String, hash: String): Boolean {
        return org.mindrot.jbcrypt.BCrypt.checkpw(password, hash)
    }
}
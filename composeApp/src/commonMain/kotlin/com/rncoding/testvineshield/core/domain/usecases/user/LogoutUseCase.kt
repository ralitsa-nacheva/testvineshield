package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.repository.UserRepository

class LogoutUseCase (
    private val userRepository: UserRepository
    ) {
        suspend operator fun invoke() {
            userRepository.logout()
        }
    }
package com.calistenia.domain.usecase

import com.calistenia.domain.model.SetupState

class ResolveSetupStateUseCase {
    operator fun invoke(safetyAccepted: Boolean, hasProfile: Boolean, hasAssessment: Boolean, hasPlan: Boolean): SetupState = when {
        !safetyAccepted -> SetupState.SAFETY_PENDING
        !hasProfile -> SetupState.PROFILE_PENDING
        !hasAssessment -> SetupState.ASSESSMENT_PENDING
        !hasPlan -> SetupState.PLAN_PENDING
        else -> SetupState.READY
    }
}

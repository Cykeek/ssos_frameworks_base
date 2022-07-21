/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.keyguard.domain.usecase

import android.content.Intent
import androidx.test.filters.SmallTest
import com.android.internal.widget.LockPatternUtils
import com.android.systemui.SysuiTestCase
import com.android.systemui.animation.ActivityLaunchAnimator
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.policy.KeyguardStateController
import com.android.systemui.util.mockito.any
import com.android.systemui.util.mockito.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(Parameterized::class)
class LaunchKeyguardQuickAffordanceUseCaseImplTest : SysuiTestCase() {

    companion object {
        private val INTENT = Intent("some.intent.action")

        @Parameters(
            name =
                "needStrongAuthAfterBoot={0}, canShowWhileLocked={1}," +
                    " keyguardIsUnlocked={2}, needsToUnlockFirst={3}"
        )
        @JvmStatic
        fun data() =
            listOf(
                arrayOf(
                    /* needStrongAuthAfterBoot= */ false,
                    /* canShowWhileLocked= */ false,
                    /* keyguardIsUnlocked= */ false,
                    /* needsToUnlockFirst= */ true,
                ),
                arrayOf(
                    /* needStrongAuthAfterBoot= */ false,
                    /* canShowWhileLocked= */ false,
                    /* keyguardIsUnlocked= */ true,
                    /* needsToUnlockFirst= */ false,
                ),
                arrayOf(
                    /* needStrongAuthAfterBoot= */ false,
                    /* canShowWhileLocked= */ true,
                    /* keyguardIsUnlocked= */ false,
                    /* needsToUnlockFirst= */ false,
                ),
                arrayOf(
                    /* needStrongAuthAfterBoot= */ false,
                    /* canShowWhileLocked= */ true,
                    /* keyguardIsUnlocked= */ true,
                    /* needsToUnlockFirst= */ false,
                ),
                arrayOf(
                    /* needStrongAuthAfterBoot= */ true,
                    /* canShowWhileLocked= */ false,
                    /* keyguardIsUnlocked= */ false,
                    /* needsToUnlockFirst= */ true,
                ),
                arrayOf(
                    /* needStrongAuthAfterBoot= */ true,
                    /* canShowWhileLocked= */ false,
                    /* keyguardIsUnlocked= */ true,
                    /* needsToUnlockFirst= */ true,
                ),
                arrayOf(
                    /* needStrongAuthAfterBoot= */ true,
                    /* canShowWhileLocked= */ true,
                    /* keyguardIsUnlocked= */ false,
                    /* needsToUnlockFirst= */ true,
                ),
                arrayOf(
                    /* needStrongAuthAfterBoot= */ true,
                    /* canShowWhileLocked= */ true,
                    /* keyguardIsUnlocked= */ true,
                    /* needsToUnlockFirst= */ true,
                ),
            )
    }

    @Mock private lateinit var lockPatternUtils: LockPatternUtils
    @Mock private lateinit var keyguardStateController: KeyguardStateController
    @Mock private lateinit var userTracker: UserTracker
    @Mock private lateinit var activityStarter: ActivityStarter
    @Mock private lateinit var animationController: ActivityLaunchAnimator.Controller

    private lateinit var underTest: LaunchKeyguardQuickAffordanceUseCase

    @JvmField @Parameter(0) var needStrongAuthAfterBoot: Boolean = false
    @JvmField @Parameter(1) var canShowWhileLocked: Boolean = false
    @JvmField @Parameter(2) var keyguardIsUnlocked: Boolean = false
    @JvmField @Parameter(3) var needsToUnlockFirst: Boolean = false

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        underTest =
            LaunchKeyguardQuickAffordanceUseCaseImpl(
                lockPatternUtils = lockPatternUtils,
                keyguardStateController = keyguardStateController,
                userTracker = userTracker,
                activityStarter = activityStarter,
            )
    }

    @Test
    fun invoke() {
        setUpMocks(
            needStrongAuthAfterBoot = needStrongAuthAfterBoot,
            keyguardIsUnlocked = keyguardIsUnlocked,
        )

        underTest(
            intent = INTENT,
            canShowWhileLocked = canShowWhileLocked,
            animationController = animationController,
        )

        if (needsToUnlockFirst) {
            verify(activityStarter)
                .postStartActivityDismissingKeyguard(
                    INTENT,
                    /* delay= */ 0,
                    animationController,
                )
        } else {
            verify(activityStarter)
                .startActivity(
                    INTENT,
                    /* dismissShade= */ true,
                    animationController,
                    /* showOverLockscreenWhenLocked= */ true,
                )
        }
    }

    private fun setUpMocks(
        needStrongAuthAfterBoot: Boolean = true,
        keyguardIsUnlocked: Boolean = false,
    ) {
        whenever(userTracker.userHandle).thenReturn(mock())
        whenever(lockPatternUtils.getStrongAuthForUser(any()))
            .thenReturn(
                if (needStrongAuthAfterBoot) {
                    LockPatternUtils.StrongAuthTracker.STRONG_AUTH_REQUIRED_AFTER_BOOT
                } else {
                    LockPatternUtils.StrongAuthTracker.STRONG_AUTH_NOT_REQUIRED
                }
            )
        whenever(keyguardStateController.isUnlocked).thenReturn(keyguardIsUnlocked)
    }
}

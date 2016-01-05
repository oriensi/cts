/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.server.cts;

import com.android.tradefed.device.DeviceNotAvailableException;

import java.lang.Exception;
import java.lang.String;

public class ActivityManagerPinnedStackTests extends ActivityManagerTestBase {
    private static final boolean PRETEND_DEVICE_SUPPORTS_PIP = false;

    private static final String PIP_ACTIVITY_COMPONENT_NAME = "android.server.app/.PipActivity";
    private static final String PIP_WINDOW_NAME =
            "android.server.app/android.server.app.PipActivity";

    private static final String AUTO_ENTER_PIP_ACTIVITY_COMPONENT_NAME =
            "android.server.app/.AutoEnterPipActivity";
    private static final String AUTO_ENTER_PIP_WINDOW_NAME =
            "android.server.app/android.server.app.AutoEnterPipActivity";

    private static final String ALWAYS_FOCUSABLE_PIP_ACTIVITY_COMPONENT_NAME =
            "android.server.app/.AlwaysFocusablePipActivity";
    private static final String ALWAYS_FOCUSABLE_PIP_WINDOW_NAME =
            "android.server.app/android.server.app.AlwaysFocusablePipActivity";

    private static final String LAUNCH_INTO_PINNED_STACK_PIP_ACTIVITY_COMPONENT_NAME =
            "android.server.app/.LaunchIntoPinnedStackPipActivity";
    private static final String LAUNCH_INTO_PINNED_STACK_PIP_WINDOW_NAME =
            "android.server.app/android.server.app.LaunchIntoPinnedStackPipActivity";

    private static final String AM_START_PIP_ACTIVITY =
            "am start -n " + PIP_ACTIVITY_COMPONENT_NAME;
    private static final String AM_MOVE_TOP_ACTIVITY_TO_PINNED_STACK =
            "am stack move-top-activity-to-pinned-stack 1 0 0 500 500";
    private static final String AM_START_AUTO_ENTER_PIP_ACTIVITY =
            "am start -n " + AUTO_ENTER_PIP_ACTIVITY_COMPONENT_NAME;
    private static final String AM_START_ALWAYS_FOCUSABLE_PIP_ACTIVITY =
            "am start -n " + ALWAYS_FOCUSABLE_PIP_ACTIVITY_COMPONENT_NAME;
    private static final String AM_START_LAUNCH_INTO_PINNED_STACK_PIP_ACTIVITY =
            "am start -n " + LAUNCH_INTO_PINNED_STACK_PIP_ACTIVITY_COMPONENT_NAME;

    private static final String AM_FORCE_STOP_TEST_PACKAGE = "am force-stop android.server.app";

    @Override
    protected void tearDown() {
        try {
            mDevice.executeShellCommand(AM_FORCE_STOP_TEST_PACKAGE);
        } catch (DeviceNotAvailableException e) {
        }
    }

    public void testEnterPictureInPictureMode() throws Exception {
        final String[] commands = { AM_START_AUTO_ENTER_PIP_ACTIVITY };
        pinnedStackTester(AUTO_ENTER_PIP_ACTIVITY_COMPONENT_NAME,
                AUTO_ENTER_PIP_WINDOW_NAME, commands, false);
    }

    public void testMoveTopActivityToPinnedStack() throws Exception {
        final String[] commands = { AM_START_PIP_ACTIVITY, AM_MOVE_TOP_ACTIVITY_TO_PINNED_STACK };
        pinnedStackTester(PIP_ACTIVITY_COMPONENT_NAME, PIP_WINDOW_NAME, commands, false);
    }

    public void testAlwaysFocusablePipActivity() throws Exception {
        final String[] commands =
                { AM_START_ALWAYS_FOCUSABLE_PIP_ACTIVITY, AM_MOVE_TOP_ACTIVITY_TO_PINNED_STACK };
        pinnedStackTester(ALWAYS_FOCUSABLE_PIP_ACTIVITY_COMPONENT_NAME,
                ALWAYS_FOCUSABLE_PIP_WINDOW_NAME, commands, true);
    }

    public void testLaunchIntoPinnedStack() throws Exception {
        final String[] commands = { AM_START_LAUNCH_INTO_PINNED_STACK_PIP_ACTIVITY };
        pinnedStackTester(
                ALWAYS_FOCUSABLE_PIP_ACTIVITY_COMPONENT_NAME,
                ALWAYS_FOCUSABLE_PIP_WINDOW_NAME, commands, true);
        // TODO:
        // - Verify size of pinned stack
        // - Verify that LAUNCH_INTO_PINNED_STACK_PIP_ACTIVITY_COMPONENT_NAME is on fullscreen
        //   stack as we don't want the fact that it launched
        //   ALWAYS_FOCUSABLE_PIP_ACTIVITY_COMPONENT_NAME to move it to the pinned stack just
        //   bacause they have the same task affinity due to package.
    }

    private void pinnedStackTester(String activiyName, String windowName, String[] commands,
            boolean isFocusable) throws Exception {
        final boolean supportsPip = hasDeviceFeature("android.software.picture_in_picture")
                || PRETEND_DEVICE_SUPPORTS_PIP;

        for (String command : commands) {
            mDevice.executeShellCommand(command);
        }

        mAmState.processActivities(mDevice);
        final boolean containsPinnedStack = mAmState.containsStack(PINNED_STACK_ID);

        mWmState.processVisibleAppWindows(mDevice);
        assertNotNull("Must have front window.", mWmState.getFrontWindow());
        assertNotNull("Must have focused window.", mWmState.getFocusedWindow());
        assertNotNull("Must have app.", mWmState.getFocusedApp());

        if (supportsPip) {
            assertTrue("Stacks must contain pinned stack.", containsPinnedStack);
            assertEquals("Pinned stack must be the front stack.",
                    PINNED_STACK_ID, mAmState.getFrontStackId());
            assertEquals("There should be one and only one resumed acivity in the system.",
                        1, mAmState.getResumedActivitiesCount());

            assertEquals("Pinned window must be the front window.",
                    windowName, mWmState.getFrontWindow());

            if (isFocusable) {
                assertEquals("Pinned stack must be the focused stack.",
                        PINNED_STACK_ID, mAmState.getFocusedStackId());
                assertEquals("Pinned activity must be focused activity.",
                        activiyName, mAmState.getFocusedActivity());
                assertEquals("Pinned activity must be the resumed activity.",
                        activiyName, mAmState.getResumedActivity());

                assertEquals("Pinned window must be focused window.",
                        windowName, mWmState.getFocusedWindow());
                assertEquals("Pinned app must be focused app.",
                        activiyName, mWmState.getFocusedApp());
            } else {
                if (PINNED_STACK_ID == mAmState.getFocusedStackId()) {
                    failNotEquals("Pinned stack can't be the focused stack.",
                            PINNED_STACK_ID, mAmState.getFocusedStackId());
                }
                if (activiyName.equals(mAmState.getFocusedActivity())) {
                    failNotEquals("Pinned stack can't be the focused activity.",
                            activiyName, mAmState.getFocusedActivity());
                }
                if (activiyName.equals(mAmState.getResumedActivity())) {
                    failNotEquals("Pinned stack can't be the resumed activity.",
                            activiyName, mAmState.getResumedActivity());
                }

                if (windowName.equals(mWmState.getFocusedWindow())) {
                    failNotEquals("Pinned window can't be focused window.",
                            windowName, mWmState.getFocusedWindow());
                }
                if (activiyName.equals(mWmState.getFocusedApp())) {
                    failNotEquals("Pinned window can't be focused app.",
                            activiyName, mWmState.getFocusedApp());
                }
            }
        } else {
            assertFalse("Stacks must not contain pinned stack.", containsPinnedStack);
        }
    }
}

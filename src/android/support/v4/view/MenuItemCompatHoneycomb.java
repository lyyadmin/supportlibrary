/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view;

import android.support.annotation.RequiresApi;
import android.annotation.TargetApi;
import android.view.MenuItem;
import android.view.View;

/**
 * Implementation of menu compatibility that can call Honeycomb APIs.
 */

@RequiresApi(11)
@TargetApi(11)
class MenuItemCompatHoneycomb {
    public static void setShowAsAction(MenuItem item, int actionEnum) {
        item.setShowAsAction(actionEnum);
    }

    public static MenuItem setActionView(MenuItem item, View view) {
        return item.setActionView(view);
    }

    public static MenuItem setActionView(MenuItem item, int resId) {
        return item.setActionView(resId);
    }

    public static View getActionView(MenuItem item) {
        return item.getActionView();
    }
}

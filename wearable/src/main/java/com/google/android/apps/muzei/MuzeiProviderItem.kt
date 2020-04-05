/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.muzei

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.wear.widget.RoundedDrawable
import com.google.android.apps.muzei.datalayer.ActivateMuzeiIntentService
import com.google.android.apps.muzei.featuredart.BuildConfig
import com.google.android.apps.muzei.sync.ProviderManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.nurik.roman.muzei.R
import net.nurik.roman.muzei.databinding.MuzeiProviderItemBinding

data class ProviderData(
        val icon: Drawable,
        val label: CharSequence,
        val description: String,
        val settingsActivity: ComponentName?
)

class MuzeiProviderViewModel(application: Application) : AndroidViewModel(application) {
    val providerLiveData = ProviderManager.getInstance(getApplication()).switchMap { provider ->
        liveData {
            val app = getApplication<Application>()
            if (provider != null) {
                val pm = app.packageManager
                val providerInfo = pm.resolveContentProvider(provider.authority,
                        PackageManager.GET_META_DATA)
                if (providerInfo != null) {
                    val icon = providerInfo.loadIcon(pm)
                    val label = providerInfo.loadLabel(pm)
                    val settingsActivity = providerInfo.metaData?.getString("settingsActivity")?.run {
                        ComponentName(providerInfo.packageName, this)
                    }
                    emit(ProviderData(icon, label,
                            ProviderManager.getDescription(app, provider.authority),
                            settingsActivity))
                }
            } else {
                GlobalScope.launch {
                    ProviderManager.select(app, BuildConfig.FEATURED_ART_AUTHORITY)
                    ActivateMuzeiIntentService.checkForPhoneApp(app)
                }
            }
        }
    }
}

fun MuzeiProviderItemBinding.create() {
    val context = root.context
    provider.setOnClickListener {
        context.startActivity(Intent(context, ChooseProviderActivity::class.java))
    }
    settings.setCompoundDrawablesRelative(RoundedDrawable().apply {
        isClipEnabled = true
        radius = context.resources.getDimensionPixelSize(R.dimen.art_detail_open_on_phone_radius)
        backgroundColor = ContextCompat.getColor(context, R.color.theme_primary)
        drawable = ContextCompat.getDrawable(context, R.drawable.ic_provider_settings)
        bounds = Rect(0, 0, radius * 2, radius * 2)
    }, null, null, null)
}

fun MuzeiProviderItemBinding.bind(providerData: ProviderData) {
    val context = root.context
    val size = context.resources.getDimensionPixelSize(R.dimen.choose_provider_image_size)
    providerData.icon.bounds = Rect(0, 0, size, size)
    provider.setCompoundDrawablesRelative(providerData.icon,
            null, null, null)
    provider.text = providerData.label
    providerDescription.isGone = providerData.description.isBlank()
    providerDescription.text = providerData.description
    settings.isVisible = providerData.settingsActivity != null
    settings.setOnClickListener {
        if (providerData.settingsActivity != null) {
            context.startActivity(Intent().apply {
                component = providerData.settingsActivity
            })
        }
    }
}

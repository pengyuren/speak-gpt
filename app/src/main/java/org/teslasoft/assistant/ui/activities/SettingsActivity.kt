/**************************************************************************
 * Copyright (c) 2023-2024 Dmytro Ostapenko. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

package org.teslasoft.assistant.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import org.teslasoft.assistant.R
import org.teslasoft.assistant.preferences.ApiEndpointPreferences
import org.teslasoft.assistant.preferences.ChatPreferences
import org.teslasoft.assistant.preferences.DeviceInfoProvider
import org.teslasoft.assistant.preferences.Logger
import org.teslasoft.assistant.preferences.Preferences
import org.teslasoft.assistant.preferences.dto.ApiEndpointObject
import org.teslasoft.assistant.ui.fragments.TileFragment
import org.teslasoft.assistant.ui.fragments.dialogs.ActivationPromptDialogFragment
import org.teslasoft.assistant.ui.fragments.dialogs.AdvancedSettingsDialogFragment
import org.teslasoft.assistant.ui.fragments.dialogs.CustomizeAssistantDialog
import org.teslasoft.assistant.ui.fragments.dialogs.LanguageSelectorDialogFragment
import org.teslasoft.assistant.ui.fragments.dialogs.SelectResolutionFragment
import org.teslasoft.assistant.ui.fragments.dialogs.SystemMessageDialogFragment
import org.teslasoft.assistant.ui.fragments.dialogs.VoiceSelectorDialogFragment
import org.teslasoft.assistant.util.TestDevicesAds
import org.teslasoft.core.auth.AccountSyncListener
import org.teslasoft.core.auth.client.SettingsListener
import org.teslasoft.core.auth.client.SyncListener
import org.teslasoft.core.auth.client.TeslasoftIDClient
import org.teslasoft.core.auth.widget.TeslasoftIDCircledButton
import java.io.IOException
import java.util.Locale

class SettingsActivity : FragmentActivity() {

    private var tileAccountFragment: TileFragment? = null
    private var tileAssistant: TileFragment? = null
    private var tileApiKey: TileFragment? = null
    private var tileAutoSend: TileFragment? = null
    private var tileVoice: TileFragment? = null
    private var tileVoiceLanguage: TileFragment? = null
    private var tileImageModel: TileFragment? = null
    private var tileImageResolution: TileFragment? = null
    private var tileTTS: TileFragment? = null
    private var tileSTT: TileFragment? = null
    private var tileSilentMode: TileFragment? = null
    private var tileAlwaysSpeak: TileFragment? = null
    private var tileTextModel: TileFragment? = null
    private var tileActivationMessage: TileFragment? = null
    private var tileSystemMessage: TileFragment? = null
    private var tileLangDetect: TileFragment? = null
    private var tileChatLayout: TileFragment? = null
    private var tileFunctionCalling: TileFragment? = null
    private var tileSlashCommands: TileFragment? = null
    private var tileDesktopMode: TileFragment? = null
    private var tileNewLook: TileFragment? = null
    private var tileAboutApp: TileFragment? = null
    private var tileClearChat: TileFragment? = null
    private var tileDocumentation: TileFragment? = null
    private var tileAmoledMode: TileFragment? = null
    private var tileLockAssistantWindow: TileFragment? = null
    private var tileCustomize: TileFragment? = null
    private var tileDeleteData: TileFragment? = null
    private var tileSendDiagnosticData: TileFragment? = null
    private var tileRevokeAuthorization: TileFragment? = null
    private var tileGetNewInstallationId: TileFragment? = null
    private var tileCrashLog: TileFragment? = null
    private var tileAdsLog: TileFragment? = null
    private var tileEventLog: TileFragment? = null
    private var tileDebugTestAds: TileFragment? = null
    private var tileChatsAutoSave: TileFragment? = null
    private var tileShowChatErrors: TileFragment? = null
    private var threadLoading: LinearLayout? = null
    private var btnRemoveAds: MaterialButton? = null
    private var root: ScrollView? = null
    private var textGlobal: TextView? = null
    private var ad: LinearLayout? = null
    private var btnBack: ImageButton? = null
    private var teslasoftIDCircledButton: TeslasoftIDCircledButton? = null

    private var adId = "<Loading...>"
    private var installationId = ""
    private var androidId = ""
    private var testAdsReady: Boolean = false
    private var areFragmentsInitialized = false
    private var chatId = ""
    private var preferences: Preferences? = null
    private var model = ""
    private var activationPrompt = ""
    private var systemMessage = ""
    private var language = "en"
    private var resolution = ""
    private var voice = ""
    private var host = ""
    private var ttsEngine = "google"
    private var apiEndpoint: ApiEndpointObject? = null

    private var teslasoftIDClient: TeslasoftIDClient? = null
    private var apiEndpointPreferences: ApiEndpointPreferences? = null

    private var modelChangedListener: AdvancedSettingsDialogFragment.StateChangesListener = object : AdvancedSettingsDialogFragment.StateChangesListener {
        override fun onSelected(name: String, maxTokens: String, endSeparator: String, prefix: String) {
            model = name
            preferences?.setModel(name)
            preferences?.setMaxTokens(maxTokens.toInt())
            preferences?.setEndSeparator(endSeparator)
            preferences?.setPrefix(prefix)
            tileTextModel?.updateSubtitle(model)
        }

        override fun onFormError(name: String, maxTokens: String, endSeparator: String, prefix: String) {
            if (name == "") Toast.makeText(this@SettingsActivity, getString(R.string.model_error_empty), Toast.LENGTH_SHORT).show()
            else if (name.contains("gpt-4")) Toast.makeText(this@SettingsActivity, "Error, GPT4 support maximum of 8192 tokens", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this@SettingsActivity, "Error, more than 2048 tokens is not supported", Toast.LENGTH_SHORT).show()
            val advancedSettingsDialogFragment: AdvancedSettingsDialogFragment = AdvancedSettingsDialogFragment.newInstance(name, chatId)
            advancedSettingsDialogFragment.setStateChangedListener(this)
            advancedSettingsDialogFragment.show(supportFragmentManager.beginTransaction(), "ModelDialog")
        }
    }

    private var languageChangedListener: LanguageSelectorDialogFragment.StateChangesListener = object : LanguageSelectorDialogFragment.StateChangesListener {
        override fun onSelected(name: String) {
            preferences?.setLanguage(name)
            language = name
            tileVoiceLanguage?.updateSubtitle(Locale.forLanguageTag(name).displayLanguage)
        }

        override fun onFormError(name: String) {
            Toast.makeText(this@SettingsActivity, getString(R.string.language_error_empty), Toast.LENGTH_SHORT).show()
            val languageSelectorDialogFragment: LanguageSelectorDialogFragment = LanguageSelectorDialogFragment.newInstance(name, chatId)
            languageSelectorDialogFragment.setStateChangedListener(this)
            languageSelectorDialogFragment.show(supportFragmentManager.beginTransaction(), "LanguageSelectorDialog")
        }
    }

    private var resolutionChangedListener: SelectResolutionFragment.StateChangesListener = object : SelectResolutionFragment.StateChangesListener {
        override fun onSelected(name: String) {
            preferences?.setResolution(name)
            resolution = name
            tileImageResolution?.updateSubtitle(name)
        }

        override fun onFormError(name: String) {
            Toast.makeText(this@SettingsActivity, getString(R.string.image_resolution_error_empty), Toast.LENGTH_SHORT).show()
            val resolutionSelectorDialogFragment: SelectResolutionFragment = SelectResolutionFragment.newInstance(name, chatId)
            resolutionSelectorDialogFragment.setStateChangedListener(this)
            resolutionSelectorDialogFragment.show(supportFragmentManager.beginTransaction(), "ResolutionSelectorDialog")
        }
    }

    private var promptChangedListener: ActivationPromptDialogFragment.StateChangesListener =
        ActivationPromptDialogFragment.StateChangesListener { prompt ->
            activationPrompt = prompt
            preferences?.setPrompt(prompt)
        }

    private var systemChangedListener: SystemMessageDialogFragment.StateChangesListener =
        SystemMessageDialogFragment.StateChangesListener { prompt ->
            systemMessage = prompt
            preferences?.setSystemMessage(prompt)
        }

    private var voiceSelectorListener: VoiceSelectorDialogFragment.OnVoiceSelectedListener =
        VoiceSelectorDialogFragment.OnVoiceSelectedListener { voice ->
            this@SettingsActivity.voice = voice

            tileVoice?.updateSubtitle(voice)
        }

    private var customizeAssistantDialogListener: CustomizeAssistantDialog.CustomizeAssistantDialogListener = object : CustomizeAssistantDialog.CustomizeAssistantDialogListener {
        override fun onEdit(assistantName: String, avatarType: String, avatarId: String) {
            preferences?.setAssistantName(assistantName)
            preferences?.setAvatarType(avatarType)
            preferences?.setAvatarId(avatarId)
        }

        override fun onError(assistantName: String, avatarType: String, avatarId: String, error: String, dialog: CustomizeAssistantDialog) {
            Toast.makeText(this@SettingsActivity, error, Toast.LENGTH_SHORT).show()
            dialog.show(supportFragmentManager.beginTransaction(), "CustomizeAssistantDialog")
        }

        override fun onCancel() { /* unused */ }
    }

    private var settingsListener: SettingsListener = object : SettingsListener {
        override fun onSuccess(settings: String) {
            super.onSuccess(settings)
        }

        override fun onError(state: String, message: String) {
            super.onError(state, message)
        }
    }

    private var syncListener: SyncListener = object : SyncListener {
        override fun onSuccess() {
            super.onSuccess()
        }

        override fun onError(state: String, message: String) {
            super.onError(state, message)
        }
    }

    private var accountSyncListener: AccountSyncListener = object : AccountSyncListener {
        override fun onAuthFinished(name: String, email: String, isDev: Boolean, token: String) {
            Thread {
                Thread.sleep(500)
                runOnUiThread {
                    if (isDev) {
                        preferences?.setDebugMode(true)
                        if (areFragmentsInitialized) {
                            tileDebugTestAds?.setEnabled(true)
                        }

                        testAdsReady = true
                    } else {
                        preferences?.setDebugMode(false)
                        if (areFragmentsInitialized) {
                            tileDebugTestAds?.setEnabled(false)
                        }

                        preferences?.setDebugTestAds(false)
                        testAdsReady = false
                    }
                }
            }.start()
        }

        override fun onAuthFailed(state: String, message: String) {
            runOnUiThread {
                preferences?.setDebugMode(false)
                if (areFragmentsInitialized) {
                    tileDebugTestAds?.setEnabled(false)
                }

                preferences?.setDebugTestAds(false)
                testAdsReady = false
            }
        }

        override fun onSignedOut() {
            runOnUiThread {
                preferences?.setDebugMode(false)
                if (areFragmentsInitialized) {
                    tileDebugTestAds?.setEnabled(false)
                }

                preferences?.setDebugTestAds(false)
                testAdsReady = false
                restartActivity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnBack = findViewById(R.id.btn_back)
        root = findViewById(R.id.root)
        textGlobal = findViewById(R.id.text_global)
        threadLoading = findViewById(R.id.thread_loading)
        btnRemoveAds = findViewById(R.id.btn_remove_ads)

        threadLoading?.visibility = View.VISIBLE
        btnRemoveAds?.visibility = View.GONE

        val extras: Bundle? = intent.extras

        if (extras != null) {
            chatId = extras.getString("chatId", "")

            if (chatId == "") {
                tileClearChat?.setEnabled(false)
                textGlobal?.visibility = TextView.VISIBLE
            } else {
                textGlobal?.visibility = TextView.GONE
            }
        } else {
            tileClearChat?.setEnabled(false)
            textGlobal?.visibility = TextView.VISIBLE
        }

        preferences = Preferences.getPreferences(this, chatId)
        apiEndpointPreferences = ApiEndpointPreferences.getApiEndpointPreferences(this)
        apiEndpoint = apiEndpointPreferences?.getApiEndpoint(this, preferences?.getApiEndpointId()!!)

        model = preferences?.getModel() ?: "gpt-3.5-turbo"
        activationPrompt = preferences?.getPrompt() ?: ""
        systemMessage = preferences?.getSystemMessage() ?: ""
        language = preferences?.getLanguage() ?: "en"
        resolution = preferences?.getResolution() ?: "256x256"
        voice = if (preferences?.getTtsEngine() == "google") preferences?.getVoice() ?: "" else preferences?.getOpenAIVoice() ?: ""

        host = apiEndpoint?.host ?: ""
        ttsEngine = preferences?.getTtsEngine() ?: "google"

        initTeslasoftID()

        reloadAmoled()

        teslasoftIDClient = TeslasoftIDClient(this, "B7:9F:CB:D0:5C:69:1D:C7:DD:5C:36:50:64:1E:9B:32:00:CA:11:41:47:ED:F1:D9:64:86:2A:CA:49:CD:65:25", "d07985975904997990790c2e5088372a", "org.teslasoft.assistant", settingsListener, syncListener)

        val t1 = Thread {
            androidId = DeviceInfoProvider.getAndroidId(this@SettingsActivity)
            createFragments1()
            createFragments2()
            createFragments3()
            createFragments4()
            createFragments5()
        }

        val t2 = Thread {
            installationId = DeviceInfoProvider.getInstallationId(this@SettingsActivity)
            createFragments6()
            createFragments7()
        }

        t1.start()
        t2.start()

        t1.join()


        Thread {
            t2.join()
            val fragmentTransaction = placeFragments()

            runOnUiThread {
                val t = Thread {
                    runOnUiThread {
                        fragmentTransaction.commit()
                    }
                }

                t.start()
                t.join()

                Thread {
                    Thread.sleep(100)
                    areFragmentsInitialized = true
                }.start()

                initAds()
                initializeLogic()

                Handler(Looper.getMainLooper()).postDelayed({
                    val fadeOut: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_out_tab)
                    threadLoading?.startAnimation(fadeOut)

                    fadeOut.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) { /* UNUSED */ }
                        override fun onAnimationEnd(animation: Animation) {
                            runOnUiThread {
                                threadLoading?.visibility = View.GONE
                                threadLoading?.elevation = 0.0f
                            }
                        }

                        override fun onAnimationRepeat(animation: Animation) { /* UNUSED */ }
                    })
                }, 50)
            }
        }.start()
    }

    private fun createFragments1() {
        val t1 = Thread {
            tileAccountFragment = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_account_title),
                null,
                getString(R.string.tile_account_subtitle),
                null,
                R.drawable.ic_user,
                false,
                chatId,
                getString(R.string.tile_account_desc)
            )

            tileAssistant = TileFragment.newInstance(
                isDefaultAssistantApp(this@SettingsActivity),
                false,
                getString(R.string.tile_assistant_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_assistant,
                false,
                chatId,
                getString(R.string.tile_assistant_desc)
            )

            tileApiKey = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_api_endpoint_title),
                null,
                host,
                null,
                R.drawable.ic_key,
                false,
                chatId,
                getString(R.string.tile_api_endpoint_desc)
            )

            tileAutoSend = TileFragment.newInstance(
                preferences?.autoSend()!!,
                true,
                getString(R.string.tile_autosend_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_send,
                false,
                chatId,
                getString(R.string.tile_autosend_desc)
            )

            tileVoice = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_tts_voice_title),
                null,
                voice,
                null,
                R.drawable.ic_voice,
                false,
                chatId,
                getString(R.string.tile_tts_voice_desc)
            )
        }

        t1.start()
        t1.join()
    }

    private fun createFragments2() {
        val t2 = Thread {
            tileVoiceLanguage = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_voice_lang_title),
                null,
                Locale.forLanguageTag(preferences?.getLanguage()!!).displayLanguage,
                null,
                R.drawable.ic_language,
                false,
                chatId,
                getString(R.string.tile_voice_lang_desc)
            )

            tileImageModel = TileFragment.newInstance(
                preferences?.getDalleVersion() == "3",
                true,
                getString(R.string.tile_dalle_3),
                null,
                getString(R.string.on),
                getString(R.string.tile_dalle_2),
                R.drawable.ic_image,
                false,
                chatId,
                getString(R.string.tile_dalle_desc)
            )

            tileImageResolution = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_image_resolution_title),
                null,
                preferences?.getResolution()!!,
                null,
                R.drawable.ic_image,
                false,
                chatId,
                getString(R.string.tile_image_resolution_desc)
            )

            tileTTS = TileFragment.newInstance(
                preferences?.getTtsEngine() == "openai",
                true,
                getString(R.string.tile_openai_tts),
                null,
                getString(R.string.on),
                getString(R.string.tile_google_tts),
                R.drawable.ic_tts,
                false,
                chatId,
                getString(R.string.tile_tts_desc)
            )

            tileSTT = TileFragment.newInstance(
                preferences?.getAudioModel() == "whisper",
                true,
                getString(R.string.tile_whisper_stt),
                null,getString(R.string.on),
                getString(R.string.tile_google_stt),
                R.drawable.ic_microphone,
                false,
                chatId,
                getString(R.string.tile_stt_desc)
            )
        }

        t2.start()
        t2.join()
    }

    private fun createFragments3() {
        val t3 = Thread {
            tileSilentMode = TileFragment.newInstance(
                preferences?.getSilence() == true,
                true,
                getString(R.string.tile_silent_mode_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_mute,
                preferences?.getNotSilence() == true,
                chatId,
                getString(R.string.tile_silent_mode_desc)
            )

            tileAlwaysSpeak = TileFragment.newInstance(
                preferences?.getNotSilence() == true,
                true,
                getString(R.string.tile_always_speak_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_volume_up,
                preferences?.getSilence() == true,
                chatId,
                getString(R.string.tile_always_speak_desc)
            )

            tileTextModel = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_text_model_title),
                null,
                model,
                null,
                R.drawable.chatgpt_icon,
                false,
                chatId,
                getString(R.string.tile_text_generation_model_desc)
            )

            tileActivationMessage = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_activation_prompt_title),
                null,
                getString(R.string.label_tap_to_set),
                null,
                R.drawable.ic_play,
                false,
                chatId,
                getString(R.string.tile_activation_prompt_desc)
            )

            tileSystemMessage = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_system_message_title),
                null,
                getString(R.string.label_tap_to_set),
                null,
                R.drawable.ic_play,
                false,
                chatId,
                getString(R.string.tile_system_message_desc)
            )
        }

        t3.start()
        t3.join()
    }

    private fun createFragments4() {
        val t4 = Thread {
            tileLangDetect = TileFragment.newInstance(
                preferences?.getAutoLangDetect() == true,
                true,
                getString(R.string.tile_ale_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_language,
                false,
                chatId,
                getString(R.string.tile_ale_desc)
            )

            tileChatLayout = TileFragment.newInstance(
                preferences?.getLayout() == "classic",
                true,
                getString(R.string.tile_classic_layout_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_chat,
                false,
                chatId,
                getString(R.string.tile_classic_layout_desc)
            )

            tileFunctionCalling = TileFragment.newInstance(
                preferences?.getFunctionCalling() == true,
                true,
                "Function calling",
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_experiment,
                false,
                chatId,
                "This feature allows you to enable function calling. Please note that this feature is experimental and unstable."
            )

            tileSlashCommands = TileFragment.newInstance(
                preferences?.getImagineCommand() == true,
                true,
                getString(R.string.tile_sh_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_experiment,
                false,
                chatId,
                getString(R.string.tile_sh_desc)
            )

            tileDesktopMode = TileFragment.newInstance(
                preferences?.getDesktopMode() == true,
                true,
                getString(R.string.tile_desktop_mode_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_desktop,
                false,
                chatId,
                getString(R.string.tile_desktop_mode_desc)
            )
        }

        t4.start()
        t4.join()
    }

    private fun createFragments5() {
        val t5 = Thread {
            tileNewLook = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_new_ui_title),
                null,
                getString(R.string.on),
                null,
                R.drawable.ic_experiment,
                true,
                chatId,
                getString(R.string.tile_new_ui_desc)
            )

            tileAboutApp = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_about_app_title),
                null,
                getString(R.string.tile_about_app_subtitle),
                null,
                R.drawable.ic_info,
                false,
                chatId,
                getString(R.string.tile_about_app_desc)
            )

            tileClearChat = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_clear_chat_title),
                null,
                getString(R.string.tile_clear_chat_subtitle),
                null,
                R.drawable.ic_close,
                chatId == "",
                chatId,
                getString(R.string.tile_clear_chat_desc)
            )

            tileDocumentation = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_documentation_title),
                null,
                getString(R.string.tile_documentation_subtitle),
                null,
                R.drawable.ic_book,
                false,
                chatId,
                getString(R.string.tile_documentation_desc)
            )

            tileAmoledMode = TileFragment.newInstance(
                preferences?.getAmoledPitchBlack() == true && isDarkThemeEnabled(),
                true,
                getString(R.string.tile_amoled_mode_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_experiment,
                !isDarkThemeEnabled(),
                chatId,
                getString(R.string.tile_amoled_mode_desc)
            )
        }

        t5.start()
        t5.join()
    }

    private fun createFragments6() {
        val t6 = Thread {
            tileLockAssistantWindow = TileFragment.newInstance(
                preferences?.getLockAssistantWindow() == true,
                true,
                getString(R.string.tile_las_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_lock,
                false,
                chatId,
                getString(R.string.tile_las_desc)
            )

            tileCustomize = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_assistant_customize_title),
                null,
                getString(R.string.tile_assistant_customize_title),
                null,
                R.drawable.ic_experiment,
                false,
                chatId,
                getString(R.string.tile_assistant_customize_desc)
            )

            tileDeleteData = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_delete_data_title),
                null,
                getString(R.string.tile_delete_data_subtitle),
                null,
                R.drawable.ic_delete,
                false,
                chatId,
                getString(R.string.tile_delete_data_desc)
            )

            val IID = if (installationId == "00000000-0000-0000-0000-000000000000" || installationId == "") "<Not assigned>" else installationId

            val usageEnabled: Boolean = getSharedPreferences("consent", MODE_PRIVATE).getBoolean("usage", false)

            tileSendDiagnosticData = TileFragment.newInstance(
                usageEnabled,
                false,
                getString(R.string.tile_uad_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_send,
                false,
                chatId,
                "This feature allows you to manage diagnostics data.\nInstallation ID: $IID\nAndroid ID: $androidId"
            )

            tileGetNewInstallationId = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_assign_iid_title),
                null,
                getString(R.string.tile_assign_iid_title),
                null,
                R.drawable.ic_privacy,
                false,
                chatId,
                getString(R.string.tile_assign_iid_desc)
            )

            tileRevokeAuthorization = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_revoke_authorization_title),
                null,
                if (installationId == "00000000-0000-0000-0000-000000000000" || installationId == "") "Authorization revoked" else "Revoke authorization",
                null,
                R.drawable.ic_close,
                installationId == "00000000-0000-0000-0000-000000000000" || installationId == "",
                chatId,
                getString(R.string.tile_revoke_authorization_desc)
            )
        }

        t6.start()
        t6.join()
    }

    private fun createFragments7() {
        val t7 = Thread {


            val logView = if (installationId == "00000000-0000-0000-0000-000000000000" || installationId == "") "Authorization revoked" else "Tap to view"

            tileCrashLog = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_crash_log_title),
                null,
                logView,
                logView,
                R.drawable.ic_bug,
                installationId == "00000000-0000-0000-0000-000000000000" || installationId == "",
                chatId,
                getString(R.string.tile_crash_log_desc)
            )

            tileAdsLog = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_ads_log_title),
                null,
                logView,
                logView,
                R.drawable.ic_bug,
                installationId == "00000000-0000-0000-0000-000000000000" || installationId == "",
                chatId,
                getString(R.string.tile_ads_log_desc)
            )

            tileEventLog = TileFragment.newInstance(
                false,
                false,
                getString(R.string.tile_events_log_title),
                null,
                logView,
                logView,
                R.drawable.ic_bug,
                installationId == "00000000-0000-0000-0000-000000000000" || installationId == "",
                chatId,
                getString(R.string.tile_events_log_desc)
            )

            tileDebugTestAds = TileFragment.newInstance(
                preferences?.getDebugTestAds()!!,
                true,
                "debug.test.ads",
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_bug,
                !testAdsReady,
                chatId,
                "[DEVS ONLY] This feature allows you to enable test ads. Use this feature while development process to avoid ads policy violations. Available only for Teslasoft ID accounts with dev permissions. Ads ID: $adId"
            )

            tileChatsAutoSave = TileFragment.newInstance(
                preferences?.getChatsAutosave() == true,
                true,
                getString(R.string.tile_autosave_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_experiment,
                false,
                chatId,
                getString(R.string.tile_autosave_desc)
            )

            tileShowChatErrors = TileFragment.newInstance(
                preferences?.showChatErrors() == true,
                true,
                getString(R.string.tile_show_chat_errors_title),
                null,
                getString(R.string.on),
                getString(R.string.off),
                R.drawable.ic_experiment,
                false,
                chatId,
                getString(R.string.tile_show_chat_errors_desc)
            )
        }

        t7.start()
        t7.join()
    }

    private fun placeFragments() : FragmentTransaction {
        val operation = supportFragmentManager.beginTransaction().replace(R.id.tile_account, tileAccountFragment!!)
            .replace(R.id.tile_assistant, tileAssistant!!)
            .replace(R.id.tile_api, tileApiKey!!)
            .replace(R.id.tile_autosend, tileAutoSend!!)
            .replace(R.id.tile_voice, tileVoice!!)
            .replace(R.id.tile_voice_language, tileVoiceLanguage!!)
            .replace(R.id.tile_image_model, tileImageModel!!)
            .replace(R.id.tile_image_resolution, tileImageResolution!!)
            .replace(R.id.tile_tts, tileTTS!!)
            .replace(R.id.tile_stt, tileSTT!!)
            .replace(R.id.tile_silent_mode, tileSilentMode!!)
            .replace(R.id.tile_always_speak, tileAlwaysSpeak!!)
            .replace(R.id.tile_text_model, tileTextModel!!)
            .replace(R.id.tile_activation_prompt, tileActivationMessage!!)
            .replace(R.id.tile_system_message, tileSystemMessage!!)
            .replace(R.id.tile_auto_language_detection, tileLangDetect!!)
            .replace(R.id.tile_chat_layout, tileChatLayout!!)
            .replace(R.id.tile_function_calling, tileFunctionCalling!!)
            .replace(R.id.tile_slash_commands, tileSlashCommands!!)
            .replace(R.id.tile_desktop_mode, tileDesktopMode!!)
            .replace(R.id.tile_new_look, tileNewLook!!)
            .replace(R.id.tile_amoled_mode, tileAmoledMode!!)
            .replace(R.id.tile_lock_assistant, tileLockAssistantWindow!!)
            .replace(R.id.tile_customize, tileCustomize!!)
            .replace(R.id.tile_chats_autosave, tileChatsAutoSave!!)
            .replace(R.id.tile_about_app, tileAboutApp!!)
            .replace(R.id.tile_clear_chat, tileClearChat!!)
            .replace(R.id.tile_documentation, tileDocumentation!!)
            .replace(R.id.tile_delete_data, tileDeleteData!!)
            .replace(R.id.tile_send_diagnostic_data, tileSendDiagnosticData!!)
            .replace(R.id.tile_revoke_authorization, tileRevokeAuthorization!!)
            .replace(R.id.tile_assign_new_id, tileGetNewInstallationId!!)
            .replace(R.id.tile_crash_log, tileCrashLog!!)
            .replace(R.id.tile_ads_log, tileAdsLog!!)
            .replace(R.id.tile_event_log, tileEventLog!!)
            .replace(R.id.tile_debug_test_ads, tileDebugTestAds!!)
            .replace(R.id.tile_show_chat_errors, tileShowChatErrors!!)

        return operation
    }

    private fun initTeslasoftID() {
        teslasoftIDCircledButton = supportFragmentManager.findFragmentById(R.id.teslasoft_id_btn) as TeslasoftIDCircledButton
        teslasoftIDCircledButton?.setAccountSyncListener(accountSyncListener)
    }

    private fun initAds() {
        val crearEventoHilo: Thread = object : Thread() {
            @SuppressLint("HardwareIds")
            override fun run() {
                val info: AdvertisingIdClient.Info?

                adId = try {
                    info = AdvertisingIdClient.getAdvertisingIdInfo(this@SettingsActivity)
                    info.id.toString()
                } catch (e: IOException) {
                    e.printStackTrace()
                    "<Google Play Services error>"
                } catch (e : GooglePlayServicesNotAvailableException) {
                    e.printStackTrace()
                    "<Google Play Services not found>"
                } catch (e : IllegalStateException) {
                    e.printStackTrace()
                    "<IllegalStateException: ${e.message}>"
                } catch (e : GooglePlayServicesRepairableException) {
                    e.printStackTrace()
                    "<Google Play Services error>"
                }

                tileDebugTestAds?.setFunctionDesc("[DEVS ONLY] This feature allows you to enable test ads. Use this feature while development process to avoid ads policy violations. Available only for Teslasoft ID accounts with dev permissions. Ads ID: $adId\nAndroid ID: $androidId")
            }
        }
        crearEventoHilo.start()

        Thread {
            while (!areFragmentsInitialized) {
                Thread.sleep(100)
            }

            runOnUiThread {
                if (testAdsReady) {
                    tileDebugTestAds?.setEnabled(true)
                }
            }
        }.start()

        ad = findViewById(R.id.ad)

        btnRemoveAds?.setOnClickListener {
            val intent = Intent(this, RemoveAdsActivity::class.java).putExtra("chatId", chatId)
            startActivity(intent)
        }

        if (preferences?.getAdsEnabled()!!) {
            btnRemoveAds?.visibility = View.GONE
            // btnRemoveAds?.visibility = View.VISIBLE
            MobileAds.initialize(this) { /* unused */ }

            Logger.log(this, "ads", "AdMob", "info", "Ads initialized")

            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(TestDevicesAds.TEST_DEVICES)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            val adView = AdView(this)
            adView.setAdSize(AdSize.LARGE_BANNER)
            adView.adUnitId =
                if (preferences?.getDebugTestAds()!!) getString(R.string.ad_banner_unit_id_test) else getString(
                    R.string.ad_banner_unit_id
                )

            ad?.addView(adView)

            val adRequest: AdRequest = AdRequest.Builder().build()

            adView.loadAd(adRequest)

            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad?.visibility = View.GONE
                    Logger.log(this@SettingsActivity, "ads", "AdMob", "error", "Ad failed to load: ${error.message}")
                }

                override fun onAdLoaded() {
                    ad?.visibility = View.VISIBLE
                    Logger.log(this@SettingsActivity, "ads", "AdMob", "info", "Ad loaded successfully")
                }
            }
        } else {
            btnRemoveAds?.visibility = View.GONE
            ad?.visibility = View.GONE
            Logger.log(this, "ads", "AdMob", "info", "Ads initialization skipped: Ads are disabled")
        }
    }

    private var apiEndpointActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val apiEndpointId = data?.getStringExtra("apiEndpointId")

            if (apiEndpointId != null) {
                apiEndpoint = apiEndpointPreferences?.getApiEndpoint(this, apiEndpointId)
                host = apiEndpoint?.host ?: ""
                preferences?.setApiEndpointId(apiEndpointId)
                tileApiKey?.updateSubtitle(host)
            }
        }
    }

    private fun initializeLogic() {
        btnBack?.setOnClickListener {
            finish()
        }

        tileAccountFragment?.setOnTileClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse("https://platform.openai.com/account")
            startActivity(intent)
        }

        tileApiKey?.setOnTileClickListener {
            apiEndpointActivityResultLauncher.launch(Intent(this, ApiEndpointsListActivity::class.java))
        }

        tileAutoSend?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setAutoSend(true)
            } else {
                preferences?.setAutoSend(false)
            }
        }}

        tileAssistant?.setOnTileClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        tileVoice?.setOnTileClickListener {
            val voiceSelectorDialogFragment: VoiceSelectorDialogFragment = VoiceSelectorDialogFragment.newInstance(if (ttsEngine == "google") preferences?.getVoice() ?: "" else preferences?.getOpenAIVoice() ?: "", chatId, ttsEngine)
            voiceSelectorDialogFragment.setVoiceSelectedListener(voiceSelectorListener)
            voiceSelectorDialogFragment.show(supportFragmentManager.beginTransaction(), "VoiceSelectorDialogFragment")
        }

        tileVoiceLanguage?.setOnTileClickListener {
            val languageSelectorDialogFragment: LanguageSelectorDialogFragment = LanguageSelectorDialogFragment.newInstance(language, chatId)
            languageSelectorDialogFragment.setStateChangedListener(languageChangedListener)
            languageSelectorDialogFragment.show(supportFragmentManager.beginTransaction(), "LanguageSelectorDialog")
        }

        tileImageModel?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setDalleVersion("3")
            } else {
                preferences?.setDalleVersion("2")
            }
        }}

        tileImageResolution?.setOnTileClickListener {
            val resolutionSelectorDialogFragment: SelectResolutionFragment = SelectResolutionFragment.newInstance(resolution, chatId)
            resolutionSelectorDialogFragment.setStateChangedListener(resolutionChangedListener)
            resolutionSelectorDialogFragment.show(supportFragmentManager.beginTransaction(), "ResolutionSelectorDialog")
        }

        tileTTS?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setTtsEngine("openai")
                ttsEngine = "openai"
            } else {
                preferences?.setTtsEngine("google")
                ttsEngine = "google"
            }

            voice = if (!ischecked) preferences?.getVoice() ?: "" else preferences?.getOpenAIVoice() ?: ""
            tileVoice?.updateSubtitle(voice)
        }}

        tileSTT?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setAudioModel("whisper")
            } else {
                preferences?.setAudioModel("google")
            }
        }}

        tileSilentMode?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setSilence(true)
                preferences?.setNotSilence(false)
                tileAlwaysSpeak?.setChecked(false)
                tileAlwaysSpeak?.setEnabled(false)
            } else {
                preferences?.setSilence(false)
                tileAlwaysSpeak?.setEnabled(true)
            }
        }}

        tileAlwaysSpeak?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setNotSilence(true)
                preferences?.setSilence(false)
                tileSilentMode?.setChecked(false)
                tileSilentMode?.setEnabled(false)
            } else {
                preferences?.setNotSilence(false)
                tileSilentMode?.setEnabled(true)
            }
        }}

        tileTextModel?.setOnTileClickListener {
            val advancedSettingsDialogFragment: AdvancedSettingsDialogFragment = AdvancedSettingsDialogFragment.newInstance(model, chatId)
            advancedSettingsDialogFragment.setStateChangedListener(modelChangedListener)
            advancedSettingsDialogFragment.show(supportFragmentManager.beginTransaction(), "AdvancedSettingsDialog")
        }

        tileActivationMessage?.setOnTileClickListener {
            val promptDialog: ActivationPromptDialogFragment = ActivationPromptDialogFragment.newInstance(activationPrompt)
            promptDialog.setStateChangedListener(promptChangedListener)
            promptDialog.show(supportFragmentManager.beginTransaction(), "PromptDialog")
        }

        tileSystemMessage?.setOnTileClickListener {
            val promptDialog: SystemMessageDialogFragment = SystemMessageDialogFragment.newInstance(systemMessage)
            promptDialog.setStateChangedListener(systemChangedListener)
            promptDialog.show(supportFragmentManager.beginTransaction(), "SystemMessageDialog")
        }

        tileLangDetect?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setAutoLangDetect(true)
            } else {
                preferences?.setAutoLangDetect(false)
            }
        }}

        tileChatLayout?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setLayout("classic")
            } else {
                preferences?.setLayout("bubbles")
            }
        }}

        tileFunctionCalling?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setFunctionCalling(true)
            } else {
                preferences?.setFunctionCalling(false)
            }
        }}

        tileSlashCommands?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setImagineCommand(true)
            } else {
                preferences?.setImagineCommand(false)
            }
        }}

        tileDesktopMode?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setDesktopMode(true)
            } else {
                preferences?.setDesktopMode(false)
            }
        }}

        tileNewLook?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setExperimentalUI(true)
            } else {
                preferences?.setExperimentalUI(false)
            }
        }}

        tileAmoledMode?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setAmoledPitchBlack(true)
            } else {
                preferences?.setAmoledPitchBlack(false)
            }

            restartActivity()
        }}

        tileLockAssistantWindow?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setLockAssistantWindow(true)
            } else {
                preferences?.setLockAssistantWindow(false)
            }
        }}

        tileChatsAutoSave?.setOnCheckedChangeListener { ischecked -> run {
            if (ischecked) {
                preferences?.setChatsAutosave(true)
            } else {
                preferences?.setChatsAutosave(false)
            }
        }}

        tileAboutApp?.setOnTileClickListener {
            startActivity(Intent(this, AboutActivity::class.java).putExtra("chatId", chatId))
        }

        tileClearChat?.setOnTileClickListener {
            MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                .setTitle(R.string.label_clear_chat)
                .setMessage(R.string.msg_clear_chat)
                .setPositiveButton(R.string.yes) { _, _ ->
                    run {
                        ChatPreferences.getChatPreferences().clearChat(this, chatId)
                        Toast.makeText(this, getString(R.string.submsg_chat_cleared), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.no) { _, _ -> }
                .show()
        }

        tileDocumentation?.setOnTileClickListener {
            startActivity(Intent(this, DocumentationActivity::class.java).putExtra("chatId", chatId))
        }

        tileDeleteData?.setOnTileClickListener {
            MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                .setTitle(R.string.label_delete_data)
                .setMessage(R.string.msg_delete_data)
                .setPositiveButton(R.string.yes) { _, _ ->
                    run {
                        Logger.deleteAllLogs(this)
                        // TODO: Send deletion request when API will be ready
                        Toast.makeText(this, getString(R.string.submsg_data_deletion), Toast.LENGTH_SHORT).show()
                        resetDeviceId()
                    }
                }
                .setNegativeButton(R.string.no) { _, _ -> }
                .show()
        }

        tileSendDiagnosticData?.setOnTileClickListener {
            if (getSharedPreferences("consent", MODE_PRIVATE).getBoolean("usage", false)) {
                MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                    .setTitle(R.string.label_uad)
                    .setMessage(R.string.msg_uad)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        run {
                            getSharedPreferences("consent", MODE_PRIVATE).edit().putBoolean("usage", false).apply()
                            tileSendDiagnosticData?.setChecked(false)
                            restartActivity()
                        }
                    }
                    .setNegativeButton(R.string.no) { _, _ -> }
                    .show()
            } else {
                MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                    .setTitle(R.string.label_uad_optin)
                    .setMessage(R.string.mgs_uad_optin)
                    .setPositiveButton(R.string.btn_agree_and_enable) { _, _ ->
                        run {
                            getSharedPreferences("consent", MODE_PRIVATE).edit().putBoolean("usage", true).apply()
                            DeviceInfoProvider.assignInstallationId(this)
                            tileSendDiagnosticData?.setChecked(true)
                            restartActivity()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()
            }
        }

        tileRevokeAuthorization?.setOnTileClickListener {
            MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                .setTitle(R.string.label_revoke_authorization)
                .setMessage("Are you sure you want to revoke authorization? After you revoke your authorization this app will stop collecting data and delete data from their servers. This action will prevent this app from writing logs (even locally). Installation ID will be removed. Once you enable usage and diagnostics this setting will be reset. This option may prevent you from bug reporting. Would you like to continue?")
                .setPositiveButton(R.string.yes) { _, _ ->
                    run {
                        DeviceInfoProvider.revokeAuthorization(this)
                        restartActivity()
                    }
                }
                .setNegativeButton(R.string.no) { _, _ -> }
                .show()
        }

        tileGetNewInstallationId?.setOnTileClickListener {
            MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                .setTitle(R.string.label_iid_assign)
                .setMessage(R.string.msg_iid_assign)
                .setPositiveButton(R.string.btn_iid_assign) { _, _ ->
                    run {
                        DeviceInfoProvider.resetInstallationId(this)
                        restartActivity()
                    }
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
        }

        tileCrashLog?.setOnTileClickListener {
            startActivity(Intent(this, LogsActivity::class.java).putExtra("type", "crash").putExtra("chatId", chatId))
        }

        tileAdsLog?.setOnTileClickListener {
            startActivity(Intent(this, LogsActivity::class.java).putExtra("type", "ads").putExtra("chatId", chatId))
        }

        tileEventLog?.setOnTileClickListener {
            startActivity(Intent(this, LogsActivity::class.java).putExtra("type", "event").putExtra("chatId", chatId))
        }

        tileDebugTestAds?.setOnCheckedChangeListener { ischecked ->
            if (ischecked) {
                preferences?.setDebugTestAds(true)
            } else {
                preferences?.setDebugTestAds(false)
            }

            restartActivity()
        }

        tileShowChatErrors?.setOnCheckedChangeListener { ischecked ->
            if (ischecked) {
                preferences?.setShowChatErrors(true)
            } else {
                preferences?.setShowChatErrors(false)
            }
        }

        tileCustomize?.setOnTileClickListener {
            val customizeAssistantDialogFragment: CustomizeAssistantDialog = CustomizeAssistantDialog.newInstance(chatId, preferences?.getAssistantName() ?: "SpeakGPT", preferences?.getAvatarType() ?: "builtin", preferences?.getAvatarId() ?: "gpt")
            customizeAssistantDialogFragment.setCustomizeAssistantDialogListener(customizeAssistantDialogListener)
            customizeAssistantDialogFragment.show(supportFragmentManager.beginTransaction(), "CustomizeAssistantDialog")
        }
    }

    private fun restartActivity() {
        runOnUiThread {
            threadLoading?.visibility = View.VISIBLE
            threadLoading?.elevation = 100.0f
            val fadeIn: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            threadLoading?.startAnimation(fadeIn)

            fadeIn.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) { /* UNUSED */ }
                override fun onAnimationEnd(animation: Animation) {
                    runOnUiThread {
                        recreate()
                    }
                }

                override fun onAnimationRepeat(animation: Animation) { /* UNUSED */ }
            })
        }
    }

    private fun resetDeviceId() {
        MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
            .setTitle(R.string.label_iid_reset)
            .setMessage(R.string.msg_iid_reset)
            .setPositiveButton(R.string.btn_reset) { _, _ ->
                run {
                    DeviceInfoProvider.resetInstallationId(this)
                    restartActivity()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    private fun isDarkThemeEnabled(): Boolean {
        return when (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    private fun reloadAmoled() {
        if (isDarkThemeEnabled() && preferences?.getAmoledPitchBlack() == true) {
            if (android.os.Build.VERSION.SDK_INT <= 34) {
                window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.amoled_window_background, theme)
                window.statusBarColor = ResourcesCompat.getColor(resources, R.color.amoled_window_background, theme)
            }
            window.setBackgroundDrawableResource(R.color.amoled_window_background)
            root?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.amoled_window_background, theme))
            btnBack?.setBackgroundResource(R.drawable.btn_accent_icon_large_amoled)
            threadLoading?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.amoled_window_background, theme))
        } else {
            if (android.os.Build.VERSION.SDK_INT <= 34) {
                window.navigationBarColor = SurfaceColors.SURFACE_0.getColor(this)
                window.statusBarColor = SurfaceColors.SURFACE_0.getColor(this)
            }
            window.setBackgroundDrawableResource(R.color.window_background)
            root?.setBackgroundColor(SurfaceColors.SURFACE_0.getColor(this))
            btnBack?.background = getDisabledDrawable(ResourcesCompat.getDrawable(resources, R.drawable.btn_accent_icon_large, theme)!!)
            threadLoading?.setBackgroundColor(SurfaceColors.SURFACE_0.getColor(this))
        }
    }

    private fun isDefaultAssistantApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        } else {
            // For older versions, use the Settings API to check
            val defaultAssistPackage = Settings.Secure.getString(
                context.contentResolver,
                "voice_interaction_service"
            )

            val myPackage = context.packageName
            defaultAssistPackage.contains(myPackage)
        }
    }

    override fun onResume() {
        super.onResume()

        if (areFragmentsInitialized) {
            tileAssistant?.setChecked(isDefaultAssistantApp(this))
        }

        // Reset preferences singleton
        Preferences.getPreferences(this, chatId)
    }

    private fun getDisabledDrawable(drawable: Drawable) : Drawable {
        DrawableCompat.setTint(DrawableCompat.wrap(drawable), getDisabledColor())
        return drawable
    }

    private fun getDisabledColor() : Int {
        return if (isDarkThemeEnabled() && preferences?.getAmoledPitchBlack() == true) {
            ResourcesCompat.getColor(resources, R.color.accent_50, theme)
        } else {
            SurfaceColors.SURFACE_5.getColor(this)
        }
    }
}

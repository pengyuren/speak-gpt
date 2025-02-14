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

package org.teslasoft.assistant.ui.adapters

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.elevation.SurfaceColors
import io.noties.markwon.Markwon
import org.teslasoft.assistant.R
import org.teslasoft.assistant.preferences.ChatPreferences
import org.teslasoft.assistant.preferences.Preferences
import org.teslasoft.assistant.ui.activities.ImageBrowserActivity
import org.teslasoft.assistant.ui.fragments.dialogs.EditMessageDialogFragment
import java.io.File
import java.io.FileInputStream
import java.util.Base64
import java.util.Collections

abstract class AbstractChatAdapter(data: ArrayList<HashMap<String, Any>>?, context: FragmentActivity?, protected open val preferences: Preferences) : BaseAdapter(), EditMessageDialogFragment.StateChangesListener {
    protected val dataArray: ArrayList<HashMap<String, Any>>? = data
    protected val mContext: FragmentActivity? = context
    override fun getCount(): Int {
        return dataArray!!.size
    }
    override fun getItem(position: Int): Any {
        return dataArray!![position]
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    protected var ui: ConstraintLayout? = null
    protected var icon: ImageView? = null
    protected var message: TextView? = null
    protected var dalleImage: ImageView? = null
    protected var btnCopy: ImageButton? = null
    protected var btnEdit: ImageButton? = null
    private var btnRetry: ImageButton? = null
    private var dalleImageStringList = ArrayList<String>(Collections.nCopies(count+1, ""))
    private var imageStringList = ArrayList<String>(Collections.nCopies(count+1, ""))

    private var onUpdateListener: OnUpdateListener? = null

    fun setOnUpdateListener(listener: OnUpdateListener) {
        onUpdateListener = listener
    }

    private fun editMessage(position: Int, message: String) {
        dataArray?.get(position)?.set("message", message)
        onUpdateListener?.onMessageEdited()
    }

    private fun deleteMessage(position: Int) {
        dataArray?.removeAt(position)
    }

    override fun onEdit(prompt: String, position: Int) {
        editMessage(position, prompt)
        notifyDataSetChanged()

        if (preferences.getChatId() !== "") {
            val chatPreferences = ChatPreferences.getChatPreferences()
            chatPreferences.editMessage(mContext ?: return, preferences.getChatId(), position, prompt)
        }
    }

    override fun onDelete(position: Int) {
        deleteMessage(position)
        notifyDataSetChanged()

        if (preferences.getChatId() !== "") {
            val chatPreferences = ChatPreferences.getChatPreferences()
            chatPreferences.deleteMessage(mContext ?: return, preferences.getChatId(), position)
        }

        onUpdateListener?.onMessageDeleted()
    }

    private fun openEditDialog(position: Int) {
        val dialog = EditMessageDialogFragment.newInstance(dataArray?.get(position)?.get("message").toString(), position)
        dialog.setStateChangedListener(this)
        dialog.show(mContext?.supportFragmentManager ?: return, "EditMessageDialogFragment")
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        if (mContext == null) return convertView!!

        ui = convertView?.findViewById(R.id.ui)
        btnRetry = convertView?.findViewById(R.id.btn_retry)

        if (dataArray!!.isNotEmpty() && position == dataArray.size - 1 && dataArray[position]["isBot"] == true) {
            btnRetry?.visibility = View.VISIBLE

            btnRetry?.setOnClickListener {
                onUpdateListener?.onRetryClick()
            }
        } else {
            btnRetry?.visibility = View.GONE
        }

        ui?.setOnLongClickListener {
            openEditDialog(position)
            true
        }

        btnEdit?.setOnClickListener {
            openEditDialog(position)
        }

        btnCopy?.setImageResource(R.drawable.ic_copy)
        btnCopy?.setOnClickListener {
            val clipboard: ClipboardManager = mContext.getSystemService(FragmentActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("response", dataArray[position]["message"].toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(mContext, mContext.getString(R.string.label_copy), Toast.LENGTH_SHORT).show()
        }

        if (dataArray[position]["message"].toString().contains("data:image")) {
            dalleImage?.visibility = View.VISIBLE
            message?.visibility = View.GONE
            btnCopy?.visibility = View.GONE

            val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(convertDpToPixel(16f, mContext).toInt()))
            Glide.with(mContext).load(Uri.parse(dataArray[position]["message"].toString())).apply(requestOptions).into(dalleImage!!)

            dalleImage?.setOnClickListener {
                val sharedPreferences: SharedPreferences = mContext.getSharedPreferences("tmp", Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putString("tmp", dataArray[position]["message"].toString())
                editor.apply()
                val intent = Intent(mContext, ImageBrowserActivity::class.java).setAction(Intent.ACTION_VIEW)
                intent.putExtra("tmp", "1")
                mContext.startActivity(intent)
            }
        } else if (dataArray[position]["message"].toString().contains("~file:")) {
            dalleImage?.visibility = View.VISIBLE
            message?.visibility = View.GONE
            btnCopy?.visibility = View.GONE


            val contents: String = dataArray[position]["message"].toString()
            val fileName: String = contents.replace("~file:", "")
            try {
                val fullPath = mContext.getExternalFilesDir("images")?.absolutePath + "/" + fileName + ".png"

                while (dalleImageStringList.size < count+1) {
                    dalleImageStringList.add("")
                }

                if (dalleImageStringList[position] == "") {
                    mContext.contentResolver?.openFileDescriptor(
                        Uri.fromFile(
                            File(fullPath)
                        ), "r"
                    )?.use {
                        FileInputStream(it.fileDescriptor).use {
                            val c: ByteArray = it.readBytes()
                            dalleImageStringList[position] = "data:image/png;base64," + Base64.getEncoder().encodeToString(c)

                            val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(convertDpToPixel(16f, mContext ).toInt()))
                            Glide.with(mContext).load(Uri.parse(dalleImageStringList[position])).apply(requestOptions).into(dalleImage!!)

                            dalleImage?.setOnClickListener {
                                val sharedPreferences: SharedPreferences = mContext.getSharedPreferences("tmp", Context.MODE_PRIVATE) ?: return@setOnClickListener
                                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                                editor.putString("tmp", dalleImageStringList[position])
                                editor.apply()
                                val intent = Intent(mContext, ImageBrowserActivity::class.java).setAction(Intent.ACTION_VIEW)
                                intent.putExtra("tmp", "1")
                                mContext.startActivity(intent)
                            }
                        }
                    }
                } else {
                    val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(convertDpToPixel(16f, mContext).toInt()))
                    Glide.with(mContext).load(Uri.parse(dalleImageStringList[position])).apply(requestOptions).into(dalleImage!!)

                    dalleImage?.setOnClickListener {
                        val sharedPreferences: SharedPreferences = mContext.getSharedPreferences("tmp", Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putString("tmp", dalleImageStringList[position])
                        editor.apply()
                        val intent = Intent(mContext, ImageBrowserActivity::class.java).setAction(Intent.ACTION_VIEW)
                        intent.putExtra("tmp", "1")
                        mContext.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dalleImage?.visibility = View.GONE
                message?.visibility = View.VISIBLE
                btnCopy?.visibility = View.VISIBLE
                message?.text = "<IMAGE NOT FOUND\n" +
                        "Stacktrace: ${e.stackTraceToString()}>"
            }
        } else {
            if (dataArray[position]["isBot"] == true) {
                val src = dataArray[position]["message"].toString()
                val markwon: Markwon = Markwon.create(mContext)
                markwon.setMarkdown(message!!, src)
            } else {
                message?.text = dataArray[position]["message"].toString()
            }

            if (dataArray[position]["isBot"] == false && dataArray[position]["image"] !== null) {
                dalleImage?.visibility = View.VISIBLE

                try {
                    val imageType = dataArray[position]["imageType"] ?: "png"
                    val mimeType = if (imageType == "png") "image/png" else "image/jpeg"
                    val fullPath = mContext.getExternalFilesDir("images")?.absolutePath + "/" + dataArray[position]["image"] + "." + imageType

                    while (imageStringList.size < count+1) {
                        imageStringList.add("")
                    }

                    if (imageStringList[position] == "") {
                        mContext.contentResolver?.openFileDescriptor(
                            Uri.fromFile(
                                File(fullPath)
                            ), "r"
                        )?.use { it ->
                            FileInputStream(it.fileDescriptor).use {
                                val c: ByteArray = it.readBytes()
                                imageStringList[position] = "data:$mimeType;base64," + Base64.getEncoder().encodeToString(c)

                                val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(convertDpToPixel(16f, mContext).toInt()))
                                Glide.with(mContext).load(Uri.parse(imageStringList[position])).apply(requestOptions).into(dalleImage!!)

                                dalleImage?.setOnClickListener {
                                    val sharedPreferences: SharedPreferences = mContext.getSharedPreferences("tmp", Context.MODE_PRIVATE)
                                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                                    editor.putString("tmp", imageStringList[position])
                                    editor.apply()
                                    val intent = Intent(mContext, ImageBrowserActivity::class.java).setAction(Intent.ACTION_VIEW)
                                    intent.putExtra("tmp", "1")
                                    mContext.startActivity(intent)
                                }
                            }
                        }
                    } else {
                        val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(convertDpToPixel(16f, mContext).toInt()))
                        Glide.with(mContext).load(Uri.parse(imageStringList[position])).apply(requestOptions).into(dalleImage!!)

                        dalleImage?.setOnClickListener {
                            val sharedPreferences: SharedPreferences? = mContext.getSharedPreferences("tmp", Context.MODE_PRIVATE)
                            val editor: SharedPreferences.Editor = sharedPreferences?.edit()!!
                            editor.putString("tmp", imageStringList[position])
                            editor.apply()
                            val intent = Intent(mContext, ImageBrowserActivity::class.java).setAction(Intent.ACTION_VIEW)
                            intent.putExtra("tmp", "1")
                            mContext.startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    dalleImage?.visibility = View.GONE
                    message?.visibility = View.VISIBLE
                    btnCopy?.visibility = View.VISIBLE
                    message?.text = "${message?.text}\n<IMAGE NOT FOUND: ${dataArray[position]["image"]}.${dataArray[position]["imageType"]}\nStacktrace: ${e.stackTraceToString()}>"
                }
            } else {
                dalleImage?.visibility = View.GONE
            }

            message?.visibility = View.VISIBLE
        }

        return convertView!!
    }

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
    }

    protected fun getSurfaceColor(context: Context): Int {
        return if (isDarkThemeEnabled() && preferences.getAmoledPitchBlack()) {
            ResourcesCompat.getColor(context.resources, R.color.amoled_accent_50, null)
        } else {
            SurfaceColors.SURFACE_1.getColor(context)
        }
    }

    protected fun getSurface2Color(context: Context): Int {
        return if (isDarkThemeEnabled() && preferences.getAmoledPitchBlack()) {
            ResourcesCompat.getColor(context.resources, R.color.amoled_window_background, null)
        } else {
            SurfaceColors.SURFACE_0.getColor(context)
        }
    }

    protected fun isDarkThemeEnabled(): Boolean {
        return when (mContext?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    interface OnUpdateListener {
        fun onRetryClick()
        fun onMessageEdited()
        fun onMessageDeleted()
    }
}

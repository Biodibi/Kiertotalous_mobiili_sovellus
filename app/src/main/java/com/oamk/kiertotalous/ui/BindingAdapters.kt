package com.oamk.kiertotalous.ui

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.storage.StorageReference
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.model.FileInfo
import timber.log.Timber
import android.view.*

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("customHeight")
    fun setLayoutHeight(view: View, height: Float) {
        view.layoutParams = view.layoutParams.apply {
            this.height = height.toInt()
        }
    }

    @JvmStatic
    @BindingAdapter("customWidth")
    fun setLayoutWidth(view: View, width: Float) {
        view.layoutParams = view.layoutParams.apply {
            this.width = width.toInt()
        }
    }

    @JvmStatic
    @BindingAdapter("localFile")
    fun fetchLocalFile(imageView: ImageView, fileInfo: FileInfo?) {
        imageView.setImageBitmap(null)
        fileInfo?.file?.let { file ->
            val progressDrawable = CircularProgressDrawable(imageView.context).apply {
                strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3F, imageView.context.resources.displayMetrics)
                centerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12F, imageView.context.resources.displayMetrics)
                colorFilter = BlendModeColorFilter(imageView.context.getColor(R.color.green), BlendMode.SRC_ATOP)
            }
            GlideApp.with(imageView.context)
                .load(file)
                .fitCenter()
                .placeholder(progressDrawable)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        Timber.e(e)
                        progressDrawable.stop()
                        return false
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                })
                .into(imageView)
        }
    }

    @JvmStatic
    @BindingAdapter("storageReference")
    fun fetchStorageReference(imageView: ImageView, storageReference: StorageReference?) {
        imageView.setImageBitmap(null)
        storageReference?.let {
            val progressDrawable = CircularProgressDrawable(imageView.context).apply {
                strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3F, imageView.context.resources.displayMetrics)
                centerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12F, imageView.context.resources.displayMetrics)
                colorFilter = BlendModeColorFilter(imageView.context.getColor(R.color.green), BlendMode.SRC_ATOP)
            }
            progressDrawable.start()
            GlideApp.with(imageView.context)
                .load(storageReference)
                .fitCenter()
                .placeholder(progressDrawable)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        Timber.e(e)
                        progressDrawable.stop()
                        return false
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                })
                .into(imageView)
        }
    }
}
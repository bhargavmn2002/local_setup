package com.signox.dashboard.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.signox.dashboard.BuildConfig
import com.signox.dashboard.R
import com.signox.dashboard.data.model.Media

class MediaPreviewFragment : Fragment() {
    
    private var media: Media? = null
    
    companion object {
        private const val ARG_MEDIA = "media"
        
        fun newInstance(media: Media): MediaPreviewFragment {
            return MediaPreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MEDIA, media)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        media = arguments?.getParcelable(ARG_MEDIA)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_preview, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val btnClose = view.findViewById<ImageButton>(R.id.btn_close)
        val tvMediaName = view.findViewById<TextView>(R.id.tv_media_name)
        val tvMediaInfo = view.findViewById<TextView>(R.id.tv_media_info)
        val ivImage = view.findViewById<ImageView>(R.id.iv_image)
        
        media?.let { setupPreview(it, tvMediaName, tvMediaInfo, ivImage) }
        
        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun setupPreview(
        media: Media,
        tvMediaName: TextView,
        tvMediaInfo: TextView,
        ivImage: ImageView
    ) {
        val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/")
        
        tvMediaName.text = media.originalName ?: media.name
        tvMediaInfo.text = buildString {
            append(if (media.isImage) "Image" else "Video")
            append(" • ${media.formattedSize}")
            if (media.width != null && media.height != null) {
                append(" • ${media.width}x${media.height}")
            }
            if (media.formattedDuration != null) {
                append(" • ${media.formattedDuration}")
            }
        }
        
        if (media.isImage) {
            // Show image
            ivImage.visibility = View.VISIBLE
            
            val imageUrl = when {
                media.optimizedUrl != null -> baseUrl + media.optimizedUrl
                media.previewUrl != null -> baseUrl + media.previewUrl
                else -> baseUrl + media.url
            }
            
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(ivImage)
                
        } else if (media.isVideo) {
            // For videos, show a placeholder for now
            // TODO: Implement video playback with ExoPlayer
            ivImage.visibility = View.VISIBLE
            ivImage.setImageResource(android.R.drawable.ic_media_play)
            Toast.makeText(requireContext(), "Video playback coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}

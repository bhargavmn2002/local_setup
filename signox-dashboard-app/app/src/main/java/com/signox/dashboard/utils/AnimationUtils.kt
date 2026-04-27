package com.signox.dashboard.utils

import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.recyclerview.widget.RecyclerView
import com.signox.dashboard.R

object AnimationUtils {
    
    /**
     * Animate a view with fade in effect
     */
    fun fadeIn(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.postDelayed({
            view.animate()
                .alpha(1f)
                .setDuration(600)
                .start()
        }, delay)
    }
    
    /**
     * Animate a view with slide up effect
     */
    fun slideUp(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationY = 100f
        view.postDelayed({
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()
        }, delay)
    }
    
    /**
     * Animate a view with slide from left
     */
    fun slideInLeft(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationX = -200f
        view.postDelayed({
            view.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(500)
                .start()
        }, delay)
    }
    
    /**
     * Animate a view with slide from right
     */
    fun slideInRight(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationX = 200f
        view.postDelayed({
            view.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(500)
                .start()
        }, delay)
    }
    
    /**
     * Animate a view with scale effect
     */
    fun scaleIn(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.postDelayed({
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .start()
        }, delay)
    }
    
    /**
     * Animate a view with bounce effect
     */
    fun bounceIn(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.postDelayed({
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(android.view.animation.BounceInterpolator())
                .start()
        }, delay)
    }
    
    /**
     * Animate multiple views in sequence
     */
    fun animateSequence(views: List<View>, animationType: AnimationType = AnimationType.SLIDE_UP, delayBetween: Long = 100) {
        views.forEachIndexed { index, view ->
            val delay = index * delayBetween
            when (animationType) {
                AnimationType.FADE_IN -> fadeIn(view, delay)
                AnimationType.SLIDE_UP -> slideUp(view, delay)
                AnimationType.SLIDE_LEFT -> slideInLeft(view, delay)
                AnimationType.SLIDE_RIGHT -> slideInRight(view, delay)
                AnimationType.SCALE_IN -> scaleIn(view, delay)
                AnimationType.BOUNCE_IN -> bounceIn(view, delay)
            }
        }
    }
    
    /**
     * Apply layout animation to RecyclerView
     */
    fun applyRecyclerViewAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = LayoutAnimationController(
            AnimationUtils.loadAnimation(context, R.anim.slide_up)
        )
        controller.delay = 0.1f
        controller.order = LayoutAnimationController.ORDER_NORMAL
        recyclerView.layoutAnimation = controller
    }
    
    /**
     * Pulse animation for attention
     */
    fun pulse(view: View) {
        view.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
    
    /**
     * Shake animation for errors
     */
    fun shake(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.shake)
        view.startAnimation(animation)
    }
    
    enum class AnimationType {
        FADE_IN,
        SLIDE_UP,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        SCALE_IN,
        BOUNCE_IN
    }
}

package com.signox.offlinemanager.ui.layout

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.signox.offlinemanager.R
import com.signox.offlinemanager.data.model.LayoutZone

class LayoutCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var layoutWidth = 1920
    private var layoutHeight = 1080
    private var zones = listOf<LayoutZone>()
    private var selectedZone: LayoutZone? = null
    
    private var isCreatingZone = false
    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    
    private var onZoneCreatedListener: ((Int, Int, Int, Int) -> Unit)? = null
    private var onZoneSelectedListener: ((LayoutZone?) -> Unit)? = null
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        // Zone paint
        paint.color = context.getColor(R.color.primary)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        
        // Text paint
        textPaint.color = context.getColor(R.color.text_primary)
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER
        
        // Selected zone paint
        selectedPaint.color = context.getColor(R.color.accent)
        selectedPaint.style = Paint.Style.STROKE
        selectedPaint.strokeWidth = 6f
        selectedPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    
    fun setLayoutDimensions(width: Int, height: Int) {
        layoutWidth = width
        layoutHeight = height
        invalidate()
    }
    
    fun setZones(zones: List<LayoutZone>) {
        this.zones = zones
        invalidate()
    }
    
    fun selectZone(zone: LayoutZone?) {
        selectedZone = zone
        invalidate()
    }
    
    fun enterZoneCreationMode() {
        isCreatingZone = true
    }
    
    fun setOnZoneCreatedListener(listener: (Int, Int, Int, Int) -> Unit) {
        onZoneCreatedListener = listener
    }
    
    fun setOnZoneSelectedListener(listener: (LayoutZone?) -> Unit) {
        onZoneSelectedListener = listener
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val canvasWidth = width.toFloat()
        val canvasHeight = height.toFloat()
        
        // Calculate scale to fit layout in view
        val scaleX = canvasWidth / layoutWidth
        val scaleY = canvasHeight / layoutHeight
        val scale = minOf(scaleX, scaleY)
        
        val scaledWidth = layoutWidth * scale
        val scaledHeight = layoutHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2
        val offsetY = (canvasHeight - scaledHeight) / 2
        
        // Draw layout background
        val layoutRect = RectF(offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(layoutRect, paint)
        
        // Draw layout border
        paint.style = Paint.Style.STROKE
        paint.color = context.getColor(R.color.text_secondary)
        paint.strokeWidth = 2f
        canvas.drawRect(layoutRect, paint)
        
        // Draw zones
        zones.forEach { zone ->
            val zoneRect = RectF(
                offsetX + zone.x * scale,
                offsetY + zone.y * scale,
                offsetX + (zone.x + zone.width) * scale,
                offsetY + (zone.y + zone.height) * scale
            )
            
            // Draw zone background
            paint.style = Paint.Style.FILL
            paint.color = context.getColor(R.color.primary)
            paint.alpha = 50
            canvas.drawRect(zoneRect, paint)
            
            // Draw zone border
            paint.style = Paint.Style.STROKE
            paint.color = context.getColor(R.color.primary)
            paint.alpha = 255
            paint.strokeWidth = 4f
            canvas.drawRect(zoneRect, paint)
            
            // Draw selected zone highlight
            if (zone == selectedZone) {
                canvas.drawRect(zoneRect, selectedPaint)
            }
            
            // Draw zone name
            val centerX = zoneRect.centerX()
            val centerY = zoneRect.centerY()
            canvas.drawText(zone.name, centerX, centerY, textPaint)
        }
        
        // Draw zone being created
        if (isCreatingZone && startX != 0f && startY != 0f) {
            val rect = RectF(
                minOf(startX, currentX),
                minOf(startY, currentY),
                maxOf(startX, currentX),
                maxOf(startY, currentY)
            )
            
            paint.style = Paint.Style.STROKE
            paint.color = context.getColor(R.color.accent)
            paint.strokeWidth = 4f
            paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            canvas.drawRect(rect, paint)
            paint.pathEffect = null
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isCreatingZone) {
                    startX = event.x
                    startY = event.y
                    currentX = event.x
                    currentY = event.y
                } else {
                    // Check if touching a zone
                    val touchedZone = findZoneAtPoint(event.x, event.y)
                    selectedZone = touchedZone
                    onZoneSelectedListener?.invoke(touchedZone)
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isCreatingZone) {
                    currentX = event.x
                    currentY = event.y
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (isCreatingZone && startX != 0f && startY != 0f) {
                    createZoneFromTouch()
                    isCreatingZone = false
                    startX = 0f
                    startY = 0f
                    currentX = 0f
                    currentY = 0f
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun findZoneAtPoint(x: Float, y: Float): LayoutZone? {
        val canvasWidth = width.toFloat()
        val canvasHeight = height.toFloat()
        val scaleX = canvasWidth / layoutWidth
        val scaleY = canvasHeight / layoutHeight
        val scale = minOf(scaleX, scaleY)
        val offsetX = (canvasWidth - layoutWidth * scale) / 2
        val offsetY = (canvasHeight - layoutHeight * scale) / 2
        
        return zones.find { zone ->
            val zoneLeft = offsetX + zone.x * scale
            val zoneTop = offsetY + zone.y * scale
            val zoneRight = zoneLeft + zone.width * scale
            val zoneBottom = zoneTop + zone.height * scale
            
            x >= zoneLeft && x <= zoneRight && y >= zoneTop && y <= zoneBottom
        }
    }
    
    private fun createZoneFromTouch() {
        val canvasWidth = width.toFloat()
        val canvasHeight = height.toFloat()
        val scaleX = canvasWidth / layoutWidth
        val scaleY = canvasHeight / layoutHeight
        val scale = minOf(scaleX, scaleY)
        val offsetX = (canvasWidth - layoutWidth * scale) / 2
        val offsetY = (canvasHeight - layoutHeight * scale) / 2
        
        // Convert touch coordinates to layout coordinates
        val left = ((minOf(startX, currentX) - offsetX) / scale).toInt().coerceAtLeast(0)
        val top = ((minOf(startY, currentY) - offsetY) / scale).toInt().coerceAtLeast(0)
        val right = ((maxOf(startX, currentX) - offsetX) / scale).toInt().coerceAtMost(layoutWidth)
        val bottom = ((maxOf(startY, currentY) - offsetY) / scale).toInt().coerceAtMost(layoutHeight)
        
        val width = right - left
        val height = bottom - top
        
        // Only create zone if it has minimum size
        if (width > 50 && height > 50) {
            onZoneCreatedListener?.invoke(left, top, width, height)
        }
    }
}
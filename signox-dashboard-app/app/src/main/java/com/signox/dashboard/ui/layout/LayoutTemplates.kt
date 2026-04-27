package com.signox.dashboard.ui.layout

import android.graphics.Color
import com.signox.dashboard.R
import com.signox.dashboard.data.model.LayoutTemplate
import com.signox.dashboard.data.model.TemplateSectionConfig

object LayoutTemplates {
    
    fun getAllTemplates(): List<LayoutTemplate> {
        return listOf(
            getFullScreenTemplate(),
            getSinglePaneTemplate(),
            getThreePanesTemplate(),
            getTwoThreadsVerticalTemplate(),
            getTwoThreadsHorizontalTemplate(),
            getFourSquaresTemplate(),
            getSplitHorizontalTemplate(),
            getSplitVerticalTemplate(),
            getThreeZoneTemplate(),
            getPortraitThreeBottomTemplate(),
            getLShapeRightTemplate(),
            getLShapeLeftTemplate(),
            getSplit2HorizontalTemplate(),
            getSplit3HorizontalTemplate(),
            getSplit2VerticalTemplate(),
            getSplit3VerticalTemplate(),
            getMain2SidebarTemplate(),
            getTopBar2MainTemplate(),
            getSixGridTemplate(),
            getNineGridTemplate()
        )
    }
    
    private fun getFullScreenTemplate() = LayoutTemplate(
        id = "full_screen",
        name = "Full Screen",
        description = "Single full-screen zone for maximum impact",
        thumbnail = R.drawable.ic_launcher, // TODO: Add template thumbnails
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Main Content",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 100f
            )
        )
    )
    
    private fun getSplitHorizontalTemplate() = LayoutTemplate(
        id = "split_horizontal",
        name = "Split Horizontal",
        description = "Two zones side by side",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Left Zone",
                order = 0,
                x = 0f,
                y = 0f,
                width = 50f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Right Zone",
                order = 1,
                x = 50f,
                y = 0f,
                width = 50f,
                height = 100f
            )
        )
    )
    
    private fun getSplitVerticalTemplate() = LayoutTemplate(
        id = "split_vertical",
        name = "Split Vertical",
        description = "Two zones stacked vertically",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top Zone",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Bottom Zone",
                order = 1,
                x = 0f,
                y = 50f,
                width = 100f,
                height = 50f
            )
        )
    )
    
    private fun getThreeZoneTemplate() = LayoutTemplate(
        id = "three_zone",
        name = "Three Zone",
        description = "Main content with two side panels",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Left Panel",
                order = 0,
                x = 0f,
                y = 0f,
                width = 20f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Main Content",
                order = 1,
                x = 20f,
                y = 0f,
                width = 60f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Right Panel",
                order = 2,
                x = 80f,
                y = 0f,
                width = 20f,
                height = 100f
            )
        )
    )
    
    private fun getFourZoneTemplate() = LayoutTemplate(
        id = "four_zone",
        name = "Four Zone Grid",
        description = "2x2 grid layout",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top Left",
                order = 0,
                x = 0f,
                y = 0f,
                width = 50f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Top Right",
                order = 1,
                x = 50f,
                y = 0f,
                width = 50f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Bottom Left",
                order = 2,
                x = 0f,
                y = 50f,
                width = 50f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Bottom Right",
                order = 3,
                x = 50f,
                y = 50f,
                width = 50f,
                height = 50f
            )
        )
    )
    
    private fun getSixZoneTemplate() = LayoutTemplate(
        id = "six_zone",
        name = "Six Zone Grid",
        description = "3x2 grid layout",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Zone 1",
                order = 0,
                x = 0f,
                y = 0f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 2",
                order = 1,
                x = 33.33f,
                y = 0f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 3",
                order = 2,
                x = 66.66f,
                y = 0f,
                width = 33.34f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 4",
                order = 3,
                x = 0f,
                y = 50f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 5",
                order = 4,
                x = 33.33f,
                y = 50f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 6",
                order = 5,
                x = 66.66f,
                y = 50f,
                width = 33.34f,
                height = 50f
            )
        )
    )
    
    // Additional templates based on your screenshot
    
    private fun getSinglePaneTemplate() = LayoutTemplate(
        id = "single_pane",
        name = "Single Pane",
        description = "One full screen zone",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Main",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 100f
            )
        )
    )
    
    private fun getThreePanesTemplate() = LayoutTemplate(
        id = "three_panes",
        name = "Three Panes",
        description = "Three equal horizontal zones",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Left",
                order = 0,
                x = 0f,
                y = 0f,
                width = 33.33f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Center",
                order = 1,
                x = 33.33f,
                y = 0f,
                width = 33.33f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Right",
                order = 2,
                x = 66.66f,
                y = 0f,
                width = 33.34f,
                height = 100f
            )
        )
    )
    
    private fun getTwoThreadsVerticalTemplate() = LayoutTemplate(
        id = "two_threads_vertical",
        name = "Two Threads (Vertical)",
        description = "Two vertical zones stacked",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Bottom",
                order = 1,
                x = 0f,
                y = 50f,
                width = 100f,
                height = 50f
            )
        )
    )
    
    private fun getTwoThreadsHorizontalTemplate() = LayoutTemplate(
        id = "two_threads_horizontal",
        name = "Two Threads (Horizontal)",
        description = "Two horizontal zones side by side",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Left",
                order = 0,
                x = 0f,
                y = 0f,
                width = 50f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Right",
                order = 1,
                x = 50f,
                y = 0f,
                width = 50f,
                height = 100f
            )
        )
    )
    
    private fun getFourSquaresTemplate() = LayoutTemplate(
        id = "four_squares",
        name = "Four Squares",
        description = "2x2 grid layout",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top Left",
                order = 0,
                x = 0f,
                y = 0f,
                width = 50f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Top Right",
                order = 1,
                x = 50f,
                y = 0f,
                width = 50f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Bottom Left",
                order = 2,
                x = 0f,
                y = 50f,
                width = 50f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Bottom Right",
                order = 3,
                x = 50f,
                y = 50f,
                width = 50f,
                height = 50f
            )
        )
    )
    
    private fun getPortraitThreeBottomTemplate() = LayoutTemplate(
        id = "portrait_three_bottom",
        name = "Portrait Three Bottom",
        description = "Three horizontal zones at bottom",
        thumbnail = R.drawable.ic_launcher,
        width = 1080,
        height = 1920,
        orientation = "PORTRAIT",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Middle",
                order = 1,
                x = 0f,
                y = 33.33f,
                width = 100f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Bottom",
                order = 2,
                x = 0f,
                y = 66.66f,
                width = 100f,
                height = 33.34f
            )
        )
    )
    
    private fun getLShapeRightTemplate() = LayoutTemplate(
        id = "l_shape_right",
        name = "L-Shape (Right)",
        description = "L-shaped layout with sidebar on right",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Main",
                order = 0,
                x = 0f,
                y = 0f,
                width = 66.67f,
                height = 66.67f
            ),
            TemplateSectionConfig(
                name = "Top Right",
                order = 1,
                x = 66.67f,
                y = 0f,
                width = 33.33f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Bottom Left",
                order = 2,
                x = 0f,
                y = 66.67f,
                width = 66.67f,
                height = 33.33f
            )
        )
    )
    
    private fun getLShapeLeftTemplate() = LayoutTemplate(
        id = "l_shape_left",
        name = "L-Shape (Left)",
        description = "L-shaped layout with sidebar on left",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top Left",
                order = 0,
                x = 0f,
                y = 0f,
                width = 33.33f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Main",
                order = 1,
                x = 33.33f,
                y = 0f,
                width = 66.67f,
                height = 66.67f
            ),
            TemplateSectionConfig(
                name = "Bottom Right",
                order = 2,
                x = 33.33f,
                y = 66.67f,
                width = 66.67f,
                height = 33.33f
            )
        )
    )
    
    private fun getSplit2HorizontalTemplate() = LayoutTemplate(
        id = "split_2_horizontal",
        name = "Split 2 Horizontal",
        description = "Two equal horizontal zones",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Left",
                order = 0,
                x = 0f,
                y = 0f,
                width = 50f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Right",
                order = 1,
                x = 50f,
                y = 0f,
                width = 50f,
                height = 100f
            )
        )
    )
    
    private fun getSplit3HorizontalTemplate() = LayoutTemplate(
        id = "split_3_horizontal",
        name = "Split 3 Horizontal",
        description = "Three equal horizontal zones",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Left",
                order = 0,
                x = 0f,
                y = 0f,
                width = 33.33f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Center",
                order = 1,
                x = 33.33f,
                y = 0f,
                width = 33.33f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Right",
                order = 2,
                x = 66.66f,
                y = 0f,
                width = 33.34f,
                height = 100f
            )
        )
    )
    
    private fun getSplit2VerticalTemplate() = LayoutTemplate(
        id = "split_2_vertical",
        name = "Split 2 Vertical",
        description = "Two equal vertical zones",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Bottom",
                order = 1,
                x = 0f,
                y = 50f,
                width = 100f,
                height = 50f
            )
        )
    )
    
    private fun getSplit3VerticalTemplate() = LayoutTemplate(
        id = "split_3_vertical",
        name = "Split 3 Vertical",
        description = "Three equal vertical zones",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Middle",
                order = 1,
                x = 0f,
                y = 33.33f,
                width = 100f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Bottom",
                order = 2,
                x = 0f,
                y = 66.66f,
                width = 100f,
                height = 33.34f
            )
        )
    )
    
    private fun getMain2SidebarTemplate() = LayoutTemplate(
        id = "main_2_sidebar",
        name = "Main + 2 Sidebar",
        description = "Large main area with two sidebars",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Left Sidebar",
                order = 0,
                x = 0f,
                y = 0f,
                width = 20f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Main",
                order = 1,
                x = 20f,
                y = 0f,
                width = 60f,
                height = 100f
            ),
            TemplateSectionConfig(
                name = "Right Sidebar",
                order = 2,
                x = 80f,
                y = 0f,
                width = 20f,
                height = 100f
            )
        )
    )
    
    private fun getTopBar2MainTemplate() = LayoutTemplate(
        id = "top_bar_2_main",
        name = "Top Bar + 2 Main",
        description = "Top bar with two main content areas",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Top Bar",
                order = 0,
                x = 0f,
                y = 0f,
                width = 100f,
                height = 20f
            ),
            TemplateSectionConfig(
                name = "Main Left",
                order = 1,
                x = 0f,
                y = 20f,
                width = 50f,
                height = 80f
            ),
            TemplateSectionConfig(
                name = "Main Right",
                order = 2,
                x = 50f,
                y = 20f,
                width = 50f,
                height = 80f
            )
        )
    )
    
    private fun getSixGridTemplate() = LayoutTemplate(
        id = "six_grid",
        name = "Six Grid",
        description = "3x2 grid layout",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Zone 1",
                order = 0,
                x = 0f,
                y = 0f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 2",
                order = 1,
                x = 33.33f,
                y = 0f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 3",
                order = 2,
                x = 66.66f,
                y = 0f,
                width = 33.34f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 4",
                order = 3,
                x = 0f,
                y = 50f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 5",
                order = 4,
                x = 33.33f,
                y = 50f,
                width = 33.33f,
                height = 50f
            ),
            TemplateSectionConfig(
                name = "Zone 6",
                order = 5,
                x = 66.66f,
                y = 50f,
                width = 33.34f,
                height = 50f
            )
        )
    )
    
    private fun getNineGridTemplate() = LayoutTemplate(
        id = "nine_grid",
        name = "Nine Grid",
        description = "3x3 grid layout",
        thumbnail = R.drawable.ic_launcher,
        width = 1920,
        height = 1080,
        orientation = "LANDSCAPE",
        sections = listOf(
            TemplateSectionConfig(
                name = "Zone 1",
                order = 0,
                x = 0f,
                y = 0f,
                width = 33.33f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Zone 2",
                order = 1,
                x = 33.33f,
                y = 0f,
                width = 33.33f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Zone 3",
                order = 2,
                x = 66.66f,
                y = 0f,
                width = 33.34f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Zone 4",
                order = 3,
                x = 0f,
                y = 33.33f,
                width = 33.33f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Zone 5",
                order = 4,
                x = 33.33f,
                y = 33.33f,
                width = 33.33f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Zone 6",
                order = 5,
                x = 66.66f,
                y = 33.33f,
                width = 33.34f,
                height = 33.33f
            ),
            TemplateSectionConfig(
                name = "Zone 7",
                order = 6,
                x = 0f,
                y = 66.66f,
                width = 33.33f,
                height = 33.34f
            ),
            TemplateSectionConfig(
                name = "Zone 8",
                order = 7,
                x = 33.33f,
                y = 66.66f,
                width = 33.33f,
                height = 33.34f
            ),
            TemplateSectionConfig(
                name = "Zone 9",
                order = 8,
                x = 66.66f,
                y = 66.66f,
                width = 33.34f,
                height = 33.34f
            )
        )
    )
}

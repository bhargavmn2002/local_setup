package com.signox.offlinemanager.ui.layout

data class LayoutTemplate(
    val id: String,
    val name: String,
    val description: String,
    val width: Int = 1920,
    val height: Int = 1080,
    val sections: List<LayoutSection>
)

data class LayoutSection(
    val name: String,
    val order: Int,
    val x: Float, // Percentage (0-100)
    val y: Float, // Percentage (0-100)
    val width: Float, // Percentage (0-100)
    val height: Float, // Percentage (0-100)
    val type: SectionType = SectionType.MEDIA
)

enum class SectionType {
    MEDIA, TEXT
}

object LayoutTemplates {
    val templates = listOf(
        LayoutTemplate(
            id = "single-pane",
            name = "Single Pane",
            description = "Full screen single zone layout",
            sections = listOf(
                LayoutSection("Section 1", 0, 0f, 0f, 100f, 100f)
            )
        ),
        LayoutTemplate(
            id = "split-2-horiz",
            name = "Split 2 Horizontal",
            description = "Two equal horizontal sections",
            sections = listOf(
                LayoutSection("Section 1", 0, 0f, 0f, 50f, 100f),
                LayoutSection("Section 2", 1, 50f, 0f, 50f, 100f)
            )
        ),
        LayoutTemplate(
            id = "split-2-vert",
            name = "Split 2 Vertical",
            description = "Two equal vertical sections",
            sections = listOf(
                LayoutSection("Section 1", 0, 0f, 0f, 100f, 50f),
                LayoutSection("Section 2", 1, 0f, 50f, 100f, 50f)
            )
        ),
        LayoutTemplate(
            id = "split-3-horiz",
            name = "Split 3 Horizontal",
            description = "Three equal horizontal sections",
            sections = listOf(
                LayoutSection("Section 1", 0, 0f, 0f, 33.33f, 100f),
                LayoutSection("Section 2", 1, 33.33f, 0f, 33.33f, 100f),
                LayoutSection("Section 3", 2, 66.66f, 0f, 33.34f, 100f)
            )
        ),
        LayoutTemplate(
            id = "four-grid",
            name = "Four Grid",
            description = "Four equal sections in a 2x2 grid",
            sections = listOf(
                LayoutSection("Section 1", 0, 0f, 0f, 50f, 50f),
                LayoutSection("Section 2", 1, 50f, 0f, 50f, 50f),
                LayoutSection("Section 3", 2, 0f, 50f, 50f, 50f),
                LayoutSection("Section 4", 3, 50f, 50f, 50f, 50f)
            )
        ),
        LayoutTemplate(
            id = "six-grid",
            name = "Six Grid",
            description = "Six equal sections in 3x2 grid",
            sections = listOf(
                LayoutSection("Section 1", 0, 0f, 0f, 33.33f, 50f),
                LayoutSection("Section 2", 1, 33.33f, 0f, 33.33f, 50f),
                LayoutSection("Section 3", 2, 66.66f, 0f, 33.34f, 50f),
                LayoutSection("Section 4", 3, 0f, 50f, 33.33f, 50f),
                LayoutSection("Section 5", 4, 33.33f, 50f, 33.33f, 50f),
                LayoutSection("Section 6", 5, 66.66f, 50f, 33.34f, 50f)
            )
        ),
        LayoutTemplate(
            id = "nine-grid",
            name = "Nine Grid",
            description = "Nine equal sections in 3x3 grid",
            sections = listOf(
                LayoutSection("Section 1", 0, 0f, 0f, 33.33f, 33.33f),
                LayoutSection("Section 2", 1, 33.33f, 0f, 33.33f, 33.33f),
                LayoutSection("Section 3", 2, 66.66f, 0f, 33.34f, 33.33f),
                LayoutSection("Section 4", 3, 0f, 33.33f, 33.33f, 33.33f),
                LayoutSection("Section 5", 4, 33.33f, 33.33f, 33.33f, 33.33f),
                LayoutSection("Section 6", 5, 66.66f, 33.33f, 33.34f, 33.33f),
                LayoutSection("Section 7", 6, 0f, 66.66f, 33.33f, 33.34f),
                LayoutSection("Section 8", 7, 33.33f, 66.66f, 33.33f, 33.34f),
                LayoutSection("Section 9", 8, 66.66f, 66.66f, 33.34f, 33.34f)
            )
        ),
        LayoutTemplate(
            id = "main-sidebar",
            name = "Main + Side",
            description = "Large main area with sidebar",
            sections = listOf(
                LayoutSection("Main Content", 0, 0f, 0f, 75f, 100f),
                LayoutSection("Sidebar", 1, 75f, 0f, 25f, 100f)
            )
        ),
        LayoutTemplate(
            id = "main-top",
            name = "Main + Top",
            description = "Top banner with main content area",
            sections = listOf(
                LayoutSection("Top Banner", 0, 0f, 0f, 100f, 20f),
                LayoutSection("Main Content", 1, 0f, 20f, 100f, 80f)
            )
        ),
        LayoutTemplate(
            id = "main-with-scroll-bottom",
            name = "Main + Bottom Scroll",
            description = "Main content area with bottom scrolling text",
            sections = listOf(
                LayoutSection("Main Content", 0, 0f, 0f, 100f, 85f, SectionType.MEDIA),
                LayoutSection("Bottom Scroll Text", 1, 0f, 85f, 100f, 15f, SectionType.TEXT)
            )
        ),
        LayoutTemplate(
            id = "dual-with-scroll",
            name = "Dual + Bottom Scroll",
            description = "Two content areas with bottom scrolling text",
            sections = listOf(
                LayoutSection("Content Left", 0, 0f, 0f, 50f, 85f, SectionType.MEDIA),
                LayoutSection("Content Right", 1, 50f, 0f, 50f, 85f, SectionType.MEDIA),
                LayoutSection("Bottom Scroll Text", 2, 0f, 85f, 100f, 15f, SectionType.TEXT)
            )
        )
    )
}
package com.synfusion.snipnote.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.synfusion.snipnote.domain.model.CategoryIcons
import com.synfusion.snipnote.domain.model.NoteType

object SnipIcons {

    fun category(iconKey: String): ImageVector = when (iconKey) {
        CategoryIcons.PROMPT -> Icons.Filled.AutoAwesome
        CategoryIcons.CODE -> Icons.Filled.Code
        CategoryIcons.WRITING -> Icons.Filled.Edit
        CategoryIcons.STUDY -> Icons.Filled.School
        CategoryIcons.BUSINESS -> Icons.Filled.BusinessCenter
        CategoryIcons.LINKS -> Icons.Filled.Link
        CategoryIcons.PERSONAL -> Icons.Filled.Person
        CategoryIcons.STAR -> Icons.Filled.Star
        CategoryIcons.LIGHTBULB -> Icons.Filled.Lightbulb
        CategoryIcons.MUSIC -> Icons.Filled.MusicNote
        CategoryIcons.RECIPE -> Icons.Filled.Restaurant
        else -> Icons.Filled.Folder
    }

    fun type(type: NoteType): ImageVector = when (type) {
        NoteType.PROMPT -> Icons.Filled.AutoAwesome
        NoteType.NOTE -> Icons.Outlined.StickyNote2
        NoteType.CODE -> Icons.Outlined.Terminal
        NoteType.LINK -> Icons.Outlined.Link
    }

    val quickCapture: ImageVector = Icons.Filled.Bolt
    val note: ImageVector = Icons.Outlined.Description
}

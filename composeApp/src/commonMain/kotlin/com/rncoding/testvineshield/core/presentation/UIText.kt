package com.rncoding.testvineshield.core.presentation
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


sealed interface UIText {
    data class DynamicString(val value: String): UIText
    class StringResourceId(
        val id: StringResource,
        val args: Array<Any> = arrayOf()
    ): UIText

    @Composable
    fun asString(): String{
        return when(this){
            is DynamicString -> value
            is StringResourceId -> stringResource(resource = id, formatArgs = args)
        }
    }
}
package com.example.otter.model

/**
 * Represents a function item in the top bar.
 * @param type The type of the function.
 * @param isSelected Whether this function is currently selected.
 */
data class FunctionItem(
    val type: FunctionType,
    var isSelected: Boolean = false
)

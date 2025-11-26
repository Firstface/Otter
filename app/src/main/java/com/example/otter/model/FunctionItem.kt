package com.example.otter.model

/**
 * Represents a function item in the top bar.
 * @param name The name of the function.
 * @param isSelected Whether this function is currently selected.
 */
data class FunctionItem(
    val name: String,
    var isSelected: Boolean = false
)

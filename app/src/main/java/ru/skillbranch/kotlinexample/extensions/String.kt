package ru.skillbranch.kotlinexample.extensions

import java.lang.IllegalArgumentException

fun String.fullNameToPair(): Pair<String, String?> {
    return split(" ")
        .filter { it.isNotBlank() }
        .run {
            when (size) {
                1 -> first() to null
                2 -> first() to last()
                else -> throw IllegalArgumentException(
                    "Fullname must contain only " +
                            "first name and last name, current split result ${this@fullNameToPair}"
                )
            }
        }
}
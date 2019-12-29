package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    return when {
        isEmpty() -> this
        predicate(last()) -> dropLast(1)
        else -> dropLast(1).dropLastUntil(predicate)
    }
}
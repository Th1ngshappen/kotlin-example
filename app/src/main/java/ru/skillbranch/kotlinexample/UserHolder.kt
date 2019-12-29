package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.fullNameToPair

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user ->
                require(!userExists(user)) { "A user with this email already exists" }
                map[user.login] = user
            }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        return User.makeUser(fullName, phone = rawPhone)
            .also { user ->
                require(!userExists(user)) { "A user with this phone already exists" }
                map[user.login] = user
            }
    }

    fun loginUser(login: String, password: String): String? {
        val user = map[login.trim()] ?: map[User.clearPhone(login)]
        return user?.let {
            if (it.checkPassword(password)) it.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) {
        val user = map[login.trim()] ?: map[User.clearPhone(login)]
        user?.setupAccessCode(login)
    }

    private fun userExists(user: User) = map.containsKey(user.login)

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun importUsers(list: List<String>): List<User> {
        val userList = mutableListOf<User>()
        list.forEach { inputString ->
            inputString
                .split(";")
                .map { if (it.isBlank()) null else it }.also {
                    val (firstName, lastName) = it[0]!!.fullNameToPair()
                    val salt = it[2]!!.dropLast(33)
                    val pswHash = it[2]!!.takeLast(32)
                    val user = User(firstName, lastName, it[1] , salt, pswHash, it[3])
                    userList.add(user)
                    map[user.login] = user
                }
        }
        return userList
    }
}
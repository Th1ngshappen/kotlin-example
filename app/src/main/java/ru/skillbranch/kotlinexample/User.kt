package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.fullNameToPair
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.let { clearPhone(value) }
        }

    private var _login: String? = null
    internal var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    //private val salt: String by lazy {
    //    // любые генераторы случайных чисел не считаются лёгкими операциями
    //    ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    //}

    private lateinit var salt: String
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        salt = getSomeSalt()
        passwordHash = encrypt(password)
    }

    // for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        salt = getSomeSalt()
        setupAccessCode(rawPhone)
    }

    // for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        saltStr: String,
        pwdHash: String,
        rawPhone: String?
    ) : this(
        firstName,
        lastName,
        rawPhone = rawPhone,
        email = email,
        meta = mapOf("src" to "csv")
    ) {
        println("Secondary csv constructor")
        salt = saltStr
        passwordHash = pwdHash
    }

    internal fun setupAccessCode(rawPhone: String) {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sentAccessCodeToUser(rawPhone, code)
    }

    init {
        println("first init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        phone = rawPhone

        with(phone) {
            require(this.isNullOrBlank() || isPhoneValid(this)) {
                "Enter a valid phone number starting with a + and containing 11 digits"
            }
        }

        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()

        println(userInfo)
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    private fun getSomeSalt() = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()

    // никогда не используйте хэширование по простому алгоритму, здесь для лучшего шифрования добавлена "соль"
    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sentAccessCodeToUser(phone: String, code: String) {
        println("...sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun isPhoneValid(rawPhone: String) =
            Regex("\\+[\\d]{11}").matches(rawPhone)

        fun clearPhone(rawPhone: String) = rawPhone.replace("[^+\\d]".toRegex(), "")

        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }
    }
}
package com.tripfair.app.data.auth

/** Input rules for accounts, shared by the screens (to show errors) and the repository (to match accounts). */
object AuthValidation {
    const val PASSWORD_MIN_LENGTH = 8
    const val CODE_LENGTH = 6

    private val Email = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    /** Vietnamese mobile number: 10 digits starting with 0. */
    private val Phone = Regex("^0\\d{9}$")

    fun isEmail(input: String): Boolean = Email.matches(input.trim())

    fun isPhone(input: String): Boolean = normalizePhone(input)?.let(Phone::matches) == true

    /** Accounts are found by email or phone number, so either is accepted. */
    fun isContact(input: String): Boolean = isEmail(input) || isPhone(input)

    /**
     * One form per contact so "Abc@Gmail.com " and "abc@gmail.com", or "+84 901 234 567" and
     * "0901234567", find the same account.
     */
    fun normalizeContact(input: String): String =
        if ('@' in input) input.trim().lowercase() else normalizePhone(input) ?: input.trim()

    /** At least 8 characters with both letters and digits ("Tối thiểu 8 ký tự, gồm chữ và số"). */
    fun isPassword(input: String): Boolean =
        input.length >= PASSWORD_MIN_LENGTH && input.any(Char::isLetter) && input.any(Char::isDigit)

    fun isCode(input: String): Boolean = input.length == CODE_LENGTH && input.all(Char::isDigit)

    /** Drops spaces, dots, dashes and brackets, and turns +84 into 0; null if anything else is left. */
    private fun normalizePhone(input: String): String? {
        val compact = input.filterNot { it.isWhitespace() || it in ".-()" }
        val local = if (compact.startsWith("+84")) "0" + compact.removePrefix("+84") else compact
        return local.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
    }
}

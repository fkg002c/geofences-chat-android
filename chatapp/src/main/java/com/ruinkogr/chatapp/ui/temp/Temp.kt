package com.ruinkogr.chatapp.ui.temp

import com.ezylang.evalex.Expression
import java.util.Stack


fun main() {
    listOf(
        "3+5/2+2",
        " 3/2 ",
        " 3+5 / 2 ",
        "5",
        "-24",
        "*5",
        "2-4",
        "5\\",
        "-24/",
    ).forEach { s ->
        println(" expr: \"$s\" parsed to: ${parse(s)},  alternate result: ${parseExt(s)}")
    }
}

/**

Given a string s which represents an expression, evaluate this expression and return its value.

The integer division should truncate toward zero.

You may assume that the given expression is always valid. All intermediate results will be in the range of [-231, 231 - 1].

Note: You are not allowed to use any built-in function which evaluates strings as mathematical expressions, such as eval().



Example 1:

Input: s = "3+5/2+2"
Output: 7
Example 2:2*2

Input: s = " 3/2 "
Output: 1
Example 3:

Input: s = " 3+5 / 2 "
Output: 5

 */

fun parse(s: String): Int {
    val stack = Stack<Int>()
    var currentNumber = 0
    var operator = '+'

    val cleanStr = s.replace(" ", "")

    cleanStr.forEachIndexed { i, char ->
        if (char.isDigit()) {
            currentNumber = currentNumber * 10 + (char - '0')
        }

        if (!char.isDigit() || i == cleanStr.lastIndex) {
            when (operator) {
                '+' -> stack.push(currentNumber)
                '-' -> stack.push(-currentNumber)
                '*' -> stack.push(stack.pop() * currentNumber)
                '/' -> {
                    stack.push(stack.pop() / currentNumber)
                }
            }
            operator = char
            currentNumber = 0
        }
    }

    return stack.sum()
}

fun parseExt(s: String): String {
    try {
        return Expression(s).evaluate().numberValue.toString()
    } catch (e: Exception) {
        return "error: ${e.message}"
    }
}
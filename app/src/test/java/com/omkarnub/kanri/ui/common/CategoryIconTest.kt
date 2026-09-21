package com.omkarnub.kanri.ui.common

import com.omkarnub.kanri.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryIconTest {

    @Test
    fun testCategoryIconMapping_standardCategories() {
        assertEquals(R.drawable.ic_category_food, getCategoryIconRes("Food & Dining"))
        assertEquals(R.drawable.ic_category_groceries, getCategoryIconRes("Groceries"))
        assertEquals(R.drawable.ic_category_shopping, getCategoryIconRes("Shopping"))
        assertEquals(R.drawable.ic_category_transport, getCategoryIconRes("Transport"))
        assertEquals(R.drawable.ic_category_bills, getCategoryIconRes("Bills & Utilities"))
        assertEquals(R.drawable.ic_category_salary, getCategoryIconRes("Salary & Income"))
        assertEquals(R.drawable.ic_category_health, getCategoryIconRes("Health & Medical"))
        assertEquals(R.drawable.ic_category_cash, getCategoryIconRes("Cash & ATM"))
        assertEquals(R.drawable.ic_category_entertainment, getCategoryIconRes("Entertainment"))
        assertEquals(R.drawable.ic_category_education, getCategoryIconRes("Education"))
        assertEquals(R.drawable.ic_category_investment, getCategoryIconRes("Investments"))
        assertEquals(R.drawable.ic_category_other, getCategoryIconRes("Random Unmatched Category"))
    }

    @Test
    fun testCategoryIconMapping_byIconName() {
        assertEquals(R.drawable.ic_category_food, getCategoryIconRes(null, "restaurant"))
        assertEquals(R.drawable.ic_category_groceries, getCategoryIconRes(null, "shopping_cart"))
        assertEquals(R.drawable.ic_category_shopping, getCategoryIconRes(null, "shopping_bag"))
        assertEquals(R.drawable.ic_category_transport, getCategoryIconRes(null, "directions_car"))
        assertEquals(R.drawable.ic_category_bills, getCategoryIconRes(null, "receipt"))
        assertEquals(R.drawable.ic_category_salary, getCategoryIconRes(null, "payments"))
        assertEquals(R.drawable.ic_category_health, getCategoryIconRes(null, "local_hospital"))
        assertEquals(R.drawable.ic_category_cash, getCategoryIconRes(null, "local_atm"))
    }

    @Test
    fun testCategoryIconMapping_nullSafety() {
        assertEquals(R.drawable.ic_category_other, getCategoryIconRes(null, null))
        assertEquals(R.drawable.ic_category_other, getCategoryIconRes("", ""))
    }
}

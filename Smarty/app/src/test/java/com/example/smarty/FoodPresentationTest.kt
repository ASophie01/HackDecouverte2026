package com.example.smarty

import com.example.smarty.model.FridgeItem
import com.example.smarty.ui.*
import java.time.LocalDate

class FoodPresentationTest {
    @org.junit.Test
    fun matchesFoodAndExpiryPresentation() {
    var checks = 0
    fun verify(ok: Boolean) { check(ok); checks++ }
    verify(foodKind("Milk") == FoodKind.DAIRY)
    verify(foodKind("Lait d'amande") == FoodKind.DAIRY)
    verify(foodKind("Œufs") == FoodKind.EGGS)
    verify(foodKind("ÉPINARDS") == FoodKind.VEGETABLE)
    verify(foodKind("Eggplant") == FoodKind.VEGETABLE)
    verify(foodKind("Strawberries") == FoodKind.FRUIT)
    verify(foodKind("Fromage") == FoodKind.DAIRY)
    verify(foodKind("Pain") == FoodKind.BREAD)
    verify(foodKind("Saumon") == FoodKind.FISH)
    verify(foodKind("Poulet") == FoodKind.MEAT)
    verify(foodKind("Mystery ingredient") == FoodKind.PANTRY)
    verify(foodKind("", "Dairy") == FoodKind.DAIRY)
    val today = LocalDate.of(2026, 11, 14)
    fun item(date: String) = FridgeItem(expiryDate=date)
    verify(isUseSoon(item("2026-11-14"), today))
    verify(isUseSoon(item("2026-11-17"), today))
    verify(!isUseSoon(item("2026-11-13"), today))
    verify(!isUseSoon(item("2026-11-18"), today))
    verify(!isUseSoon(item("bad-date"), today))
    verify(expiryLabel(item("2026-11-14"), today) == "Today")
    verify(expiryLabel(item("2026-11-15"), today) == "Tomorrow")
    verify(expiryLabel(item("2026-11-17"), today) == "In 3 days")
    verify(expiryLabel(item("2026-11-13"), today) == "Past entered date")
    verify(expiryLabel(item("bad-date"), today) == "Check date")
    verify(amountLabel(1.0) == "1")
    verify(amountLabel(0.25) == "0.25")
    verify(amountLabel(Double.NaN) == "—")
    check(checks == 25)
}

}

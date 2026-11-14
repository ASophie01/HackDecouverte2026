package com.example.smarty.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.smarty.R
import com.example.smarty.model.FridgeItem
import com.example.smarty.ui.*
import com.example.smarty.ui.theme.*
import java.time.LocalDate

@Composable
fun FoodIllustration(name: String, category: String = "", modifier: Modifier = Modifier) {
    val resource = when (foodKind(name, category)) {
        FoodKind.DAIRY -> R.drawable.food_dairy
        FoodKind.EGGS -> R.drawable.food_eggs
        FoodKind.VEGETABLE -> R.drawable.food_vegetable
        FoodKind.FRUIT -> R.drawable.food_fruit
        FoodKind.BREAD -> R.drawable.food_bread
        FoodKind.MEAT -> R.drawable.food_meat
        FoodKind.FISH -> R.drawable.food_fish
        FoodKind.PANTRY -> R.drawable.food_pantry
    }
    Image(painterResource(resource), contentDescription = null, modifier = modifier.size(56.dp))
}

@Composable
fun MealIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.food_meal), contentDescription = null, modifier = modifier.size(140.dp))
}

@Composable
fun SmartyBrand(modifier: Modifier = Modifier, large: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(R.drawable.smarty_fridge), contentDescription = null, modifier = Modifier.size(if (large) 58.dp else 42.dp))
        Text("Smarty", style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium)
    }
}

/** Small original line icons, avoiding a large icon dependency. */
@Composable
fun AppGlyph(name: String, label: String? = null, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.size(23.dp).semantics { if (label != null) contentDescription = label }) {
        val scale = size.minDimension / 24f
        fun point(x: Float, y: Float) = Offset(x * scale, y * scale)
        fun line(x1: Float,y1: Float,x2: Float,y2: Float) = drawLine(tint,point(x1,y1),point(x2,y2),1.8f * scale)
        fun rect(x: Float,y: Float,w: Float,h: Float,r: Float = 2f) = drawRoundRect(tint,point(x,y),Size(w*scale,h*scale),CornerRadius(r*scale),style=Stroke(1.7f*scale))
        when(name) {
            "fridge" -> { rect(5f,2f,14f,20f);line(5f,9f,19f,9f);line(8f,5f,8f,7f);line(8f,12f,8f,17f) }
            "plus" -> { line(12f,4f,12f,20f);line(4f,12f,20f,12f) }
            "chevron" -> {line(7f,9f,12f,14f);line(12f,14f,17f,9f)}
            "back" -> {line(15f,5f,8f,12f);line(8f,12f,15f,19f)}
            "more" -> {for(y in listOf(5f,12f,19f))drawCircle(tint,1.4f*scale,point(12f,y))}
            "check" -> {line(4f,12f,9f,17f);line(9f,17f,20f,5f)}
            "clock" -> {drawCircle(tint,9f*scale,point(12f,12f),style=Stroke(1.7f*scale));line(12f,6f,12f,12f);line(12f,12f,16f,14f)}
            "scan" -> {
                for(x in listOf(7f,10f,14f,17f)) line(x,7f,x,17f)
                line(3f,8f,3f,3f);line(3f,3f,8f,3f);line(16f,3f,21f,3f);line(21f,3f,21f,8f)
                line(3f,16f,3f,21f);line(3f,21f,8f,21f);line(16f,21f,21f,21f);line(21f,21f,21f,16f)
            }
            "settings" -> {
                drawCircle(tint,7f*scale,point(12f,12f),style=Stroke(1.7f*scale))
                drawCircle(tint,2.5f*scale,point(12f,12f),style=Stroke(1.7f*scale))
                line(12f,1f,12f,5f);line(12f,19f,12f,23f);line(1f,12f,5f,12f);line(19f,12f,23f,12f)
                line(4f,4f,7f,7f);line(17f,17f,20f,20f);line(4f,20f,7f,17f);line(17f,7f,20f,4f)
            }
            "recipe" -> {
                rect(5f,10f,14f,8f);line(7f,21f,17f,21f)
                drawCircle(tint,4f*scale,point(6f,8f),style=Stroke(1.7f*scale))
                drawCircle(tint,5f*scale,point(12f,6f),style=Stroke(1.7f*scale))
                drawCircle(tint,4f*scale,point(18f,8f),style=Stroke(1.7f*scale))
            }
            "people" -> {
                drawCircle(tint,3f*scale,point(9f,6f),style=Stroke(1.7f*scale))
                drawCircle(tint,2.5f*scale,point(18f,7f),style=Stroke(1.7f*scale))
                rect(3f,12f,12f,9f,4f);line(18f,13f,21f,16f);line(21f,16f,21f,21f)
            }
            "sparkle" -> {
                val path=Path().apply {moveTo(12f*scale,2f*scale);lineTo(15f*scale,9f*scale);lineTo(22f*scale,12f*scale);lineTo(15f*scale,15f*scale);lineTo(12f*scale,22f*scale);lineTo(9f*scale,15f*scale);lineTo(2f*scale,12f*scale);lineTo(9f*scale,9f*scale);close()};drawPath(path,tint)
            }
            else -> {rect(4f,5f,16f,15f);line(8f,2f,8f,8f);line(16f,2f,16f,8f);line(4f,10f,20f,10f)}
        }
    }
}

@Composable
fun ExpiryBadge(item: FridgeItem) {
    val date = itemDate(item)
    val today = LocalDate.now()
    val past = date?.isBefore(today) == true
    val background = when { past -> MaterialTheme.colorScheme.errorContainer; date == today -> Peach; isUseSoon(item, today) -> AmberSoft; else -> PaleSage }
    Surface(color = background, shape = CircleShape) {
        Text(expiryLabel(item, today), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium, color = if(past) MaterialTheme.colorScheme.onErrorContainer else Forest)
    }
}

@Composable
fun HintCard(title: String, text: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = PaleSage, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        }
    }
}

@Composable
fun MessageBanner(message: String, isError: Boolean = true) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (isError) MaterialTheme.colorScheme.errorContainer else PaleSage) {
        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium,
            color = if(isError) MaterialTheme.colorScheme.onErrorContainer else Forest)
    }
}

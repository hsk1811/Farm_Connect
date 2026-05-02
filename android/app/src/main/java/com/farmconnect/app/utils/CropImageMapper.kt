package com.farmconnect.app.utils

import com.farmconnect.app.R

/**
 * Utility object to get crop-specific images based on crop type
 */
object CropImageMapper {
    
    /**
     * Returns the appropriate drawable resource ID for a given crop type
     * @param cropType The crop type string (case-insensitive)
     * @return Drawable resource ID for the crop image
     */
    fun getCropImage(cropType: String?): Int {
        if (cropType == null) return R.drawable.crop_default
        
        return when (cropType.lowercase().trim()) {
            // Grains
            "rice", "paddy", "basmati", "non-basmati" -> R.drawable.crop_rice
            "wheat", "gehun" -> R.drawable.crop_wheat
            "corn", "maize", "makka" -> R.drawable.crop_corn
            
            // Cash Crops
            "cotton", "kapas" -> R.drawable.crop_cotton
            "sugarcane", "ganna" -> R.drawable.crop_sugarcane
            
            // Pulses/Lentils
            "pulses", "dal", "lentils", "chickpea", "pigeon pea", "tur", "moong", "urad", 
            "rajma", "chana", "gram", "beans" -> R.drawable.crop_pulses
            
            // Vegetables
            "vegetable", "vegetables", "tomato", "potato", "onion", "cabbage", "cauliflower",
            "carrot", "peas", "spinach", "brinjal", "eggplant", "cucumber", "pumpkin",
            "lady finger", "okra", "capsicum", "pepper", "bhindi", "aloo", "pyaz", "tamatar" -> R.drawable.crop_vegetables
            
            // Default for unknown crops
            else -> R.drawable.crop_default
        }
    }
    
    /**
     * Returns a crop emoji icon for additional visual representation
     */
    fun getCropEmoji(cropType: String?): String {
        if (cropType == null) return "🌾"
        
        return when (cropType.lowercase().trim()) {
            "rice", "paddy", "basmati", "non-basmati" -> "🌾"
            "wheat", "gehun" -> "🌾"
            "corn", "maize", "makka" -> "🌽"
            "cotton", "kapas" -> "🌼"
            "sugarcane", "ganna" -> "🎋"
            "pulses", "dal", "lentils", "chickpea", "pigeon pea", "tur", "moong", "urad", 
            "rajma", "chana", "gram", "beans" -> "🫘"
            "tomato", "tamatar" -> "🍅"
            "potato", "aloo" -> "🥔"
            "onion", "pyaz" -> "🧅"
            "carrot" -> "🥕"
            "cabbage", "cauliflower" -> "🥬"
            else -> "🌱"
        }
    }
}

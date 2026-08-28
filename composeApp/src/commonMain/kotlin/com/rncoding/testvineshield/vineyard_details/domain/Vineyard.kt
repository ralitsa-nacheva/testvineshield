package com.rncoding.testvineshield.vineyard_details.domain

data class Vineyard(
    val id: Int,
    val name: String,
    val location: String, //geolocation coordinates
    val area: Int, //acres or m2?
    // do I need id of user?
)

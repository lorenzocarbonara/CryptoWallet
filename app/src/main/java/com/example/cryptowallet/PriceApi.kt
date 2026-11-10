package com.example.cryptowallet

import retrofit2.http.GET

interface PriceApi {
    @GET("prices")
    suspend fun getPrices():
            Map<String, Double>
}
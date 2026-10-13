package com.myblbl.repository

import com.myblbl.model.series.AllSeriesFilterModel
import com.myblbl.repository.remote.AllSeriesRepository as NetworkAllSeriesRepository

typealias AllSeriesPage = com.myblbl.repository.remote.AllSeriesPage

class AllSeriesRepository(
    private val delegate: NetworkAllSeriesRepository
) {
    suspend fun getAllSeries(
        type: Int,
        page: Int,
        filters: List<AllSeriesFilterModel> = emptyList()
    ) = delegate.getAllSeries(type, page, filters)
}

package com.ae.movies.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.ae.movies.viewmodel.TmdbTestViewModel

val viewModelModule = module {
    viewModel { TmdbTestViewModel(get()) }
}
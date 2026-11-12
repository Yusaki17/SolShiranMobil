package pe.edu.upeu.solshiran.di

import org.koin.dsl.module
import pe.edu.upeu.solshiran.domain.repository.SolRepository
import pe.edu.upeu.solshiran.data.repository.SolRepositoryImpl
import pe.edu.upeu.solshiran.presentation.AppViewModel

val appModule = module {
    single<SolRepository> { SolRepositoryImpl(get(),get(),get()) }
    factory { AppViewModel(get()) }
}

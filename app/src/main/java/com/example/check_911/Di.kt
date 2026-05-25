package com.example.check_911

import android.app.Application
import androidx.room.Room
import com.example.check_911.data.db.MainDb
import com.example.check_911.data.db.repository.AppUrlRepository
import com.example.check_911.data.db.repository.AuthorizationRepository
import com.example.check_911.data.db.repository.InstructionRepository
import com.example.check_911.data.db.repository.ResultsSurveyRepository
import com.example.check_911.data.db.repository.SurveyRepository
import com.example.check_911.data.db.repository.TaskRepository
import com.example.check_911.data.db.repository.UsersRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val networkModule = module {
    single {
        NetWorkProvider.provideRetrofit(
            link = NetWorkProvider.BASE_URL
        )
    }
    single {
        get<Retrofit>().create(ApiServiceData::class.java)
    }
}

val dbModule = module {
    single {
        Room.databaseBuilder(
            get<Application>(),
            MainDb::class.java,
            "survey_db"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<MainDb>().surveyDao() }  // Добавляем DAO
    single {
        get<MainDb>().daoIpAddress() // Регистрация DaoIpAddress
    }
    single { get<MainDb>().daoUsers() }
    single { get<MainDb>().resultsSurveyDao() }
    single { get<MainDb>().taskDao() }
    single { get<MainDb>().taskResultDao() }
    single { get<MainDb>().instructionDao() }
}

val repositoryModule = module {
    single { SurveyRepository(get(), get()) } // Использует ApiServiceData и SurveyDao
    single { ResultsSurveyRepository(get()) }
    single { AppUrlRepository(get()) } // Для работы с URL API
    single { AuthorizationRepository(get()) }
    single { UsersRepository(get(), get()) }
    single { TaskRepository(get(), get(), get()) }
    single { InstructionRepository(get(), get()) }
}

val viewModelModule = module {
    viewModel { AuthorizationViewModel(get()) }
    viewModel { SurveyViewModel(get()) }
    viewModel { ResultsSurveyViewModel(get()) }
    viewModel { UsersViewModel(get(), get(), get()) }
    viewModel { SelectionViewModel(get()) }
    viewModel { IpAddressViewModel(get(), get()) }
    viewModel { TaskViewModel(get()) }
    viewModel { InstructionViewModel(get()) }
    viewModel { TaskDetailsViewModel(get(), get()) }
}




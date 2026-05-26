package com.example.check_911.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.check_911.data.db.dao.DaoIpAddress
import com.example.check_911.data.db.dao.DaoUsers
import com.example.check_911.data.db.dao.InstructionDao
import com.example.check_911.data.db.dao.InstructionResultDao
import com.example.check_911.data.db.dao.InstructionTaskDao
import com.example.check_911.data.db.dao.InstructionTaskResultDao
import com.example.check_911.data.db.dao.ResultsSurveyDao
import com.example.check_911.data.db.dao.SurveyDao
import com.example.check_911.data.db.dao.TaskDao
import com.example.check_911.data.db.dao.TaskResultDao
import com.example.check_911.data.db.entity.CategoryQuestionsEntity
import com.example.check_911.data.db.entity.IpAddressEntity
import com.example.check_911.data.db.entity.InstructionCategoryEntity
import com.example.check_911.data.db.entity.InstructionDetailEntity
import com.example.check_911.data.db.entity.InstructionEntity
import com.example.check_911.data.db.entity.InstructionAnswerEntity
import com.example.check_911.data.db.entity.InstructionResultEntity
import com.example.check_911.data.db.entity.InstructionTaskEntity
import com.example.check_911.data.db.entity.InstructionTaskResultEntity
import com.example.check_911.data.db.entity.OptionForQuestionsEntity
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.data.db.entity.SurveyEntity
import com.example.check_911.data.db.entity.SurveyResultEntity
import com.example.check_911.data.db.entity.TaskEntity
import com.example.check_911.data.db.entity.TaskResultEntity
import com.example.check_911.data.db.entity.UsersEntity

@Database(
    entities = [
        SurveyEntity::class,
        CategoryQuestionsEntity::class,
        QuestionEntity::class,
        OptionForQuestionsEntity::class,
        IpAddressEntity::class,
        UsersEntity::class,
        SurveyResultEntity::class,
        SurveyAnswerEntity::class,
        TaskEntity::class,
        TaskResultEntity::class,
        InstructionEntity::class,
        InstructionCategoryEntity::class,
        InstructionDetailEntity::class,
        InstructionResultEntity::class,
        InstructionAnswerEntity::class,
        InstructionTaskEntity::class,
        InstructionTaskResultEntity::class
    ],
    version = 29,
    exportSchema = false
)
abstract class MainDb : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun resultsSurveyDao(): ResultsSurveyDao
    abstract fun daoIpAddress(): DaoIpAddress
    abstract fun daoUsers(): DaoUsers

    abstract fun taskDao(): TaskDao
    abstract fun taskResultDao(): TaskResultDao
    abstract fun instructionDao(): InstructionDao
    abstract fun instructionResultDao(): InstructionResultDao
    abstract fun instructionTaskDao(): InstructionTaskDao
    abstract fun instructionTaskResultDao(): InstructionTaskResultDao
}

package com.fleeksoft.tmaattendancelocal

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.*
import kotlinx.android.parcel.Parcelize


@Database(entities = [PersonModel::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }


        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app-db").fallbackToDestructiveMigration()
                    .build()
        }

    }

    abstract fun employeeDao(): PersonDao

}


@Dao
interface PersonDao {
    @Query("SELECT * FROM person")
    suspend fun getAll(): List<PersonModel>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(  obj: PersonModel):Long

    @Query("SELECT * FROM person where personId =:id LIMIT 1")
    suspend fun getById(id: Long): PersonModel?

    @Query("DELETE FROM person")
    suspend fun deleteAll();

    @Update
    suspend fun update(obj: PersonModel): Int
}

@Keep
@Parcelize
@Entity(tableName = "person")
open class PersonModel(
        @PrimaryKey(autoGenerate = true)
        var personId: Long = 0L,
        var personName: String = "",
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
        var personFaceTemplate: ByteArray
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is PersonModel) {
            return other.personId == this.personId
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = personId.hashCode()
        result = 31 * result + personName.hashCode()
        return result
    }


}
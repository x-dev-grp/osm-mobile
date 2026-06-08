package com.xdev.osm_mobile.database

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.xdev.osm_mobile.database.dao.MainDao
import com.xdev.osm_mobile.database.entities.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory//this one is for sqlcipher
import java.security.SecureRandom

@Database(
    entities = [
        LotEntity::class,
        OfEntity::class,
        ArticleEntity::class,
        StockEntity::class,
        BomEntity::class,
        BomLineEntity::class,
        ExpeditionEntity::class,
        MovementEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mainDao(): MainDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_PASSPHRASE_PREF = "db_passphrase"

        init {
            try {
                System.loadLibrary("sqlcipher")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `movements` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `articleId` TEXT NOT NULL,
                        `articleName` TEXT,
                        `quantity` INTEGER NOT NULL,
                        `typeMouvement` TEXT NOT NULL,
                        `motif` TEXT,
                        `dateMouvement` TEXT
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)//Crée une clé AES sécurisée dans Android Keystore.Cette clé sert à chiffrer les SharedPreferences.
                val securePrefs = EncryptedSharedPreferences.create(//Crée des préférences sécurisées.
                    "db_secure_prefs",
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                val passphrase = securePrefs.getString(DB_PASSPHRASE_PREF, null)
                    ?: run {
                        val key = ByteArray(32)
                        SecureRandom().nextBytes(key)
                        val newPassphrase = Base64.encodeToString(key, Base64.NO_WRAP)
                        securePrefs.edit().putString(DB_PASSPHRASE_PREF, newPassphrase).apply()
                        newPassphrase
                    }

                val factory = SupportOpenHelperFactory(passphrase.toByteArray())//Crée moteur SQLCipher avec clé.
                android.util.Log.d("DATABASE_KEY", "La clé est : $passphrase")
                val instance = Room.databaseBuilder(//Crée DB Room
                    appContext,
                    AppDatabase::class.java,
                    "osm_mobile_db"
                )//this ligne
                  //.openHelperFactory(factory)
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
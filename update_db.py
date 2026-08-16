import re

# Update AiModelEntity.kt
path1 = 'app/src/main/java/com/example/engine/db/AiModelEntity.kt'
with open(path1, 'r') as f:
    content1 = f.read()

new_entity = """package com.example.engine.db

import androidx.room.Entity

@Entity(tableName = "ai_models", primaryKeys = ["providerId", "modelId"])
data class AiModelEntity(
    val providerId: String,
    val modelId: String,
    val fetchedAt: Long = System.currentTimeMillis(),
    val inputType: String = "TEXT", // TEXT, AUDIO, IMAGE, MULTIMODAL
    val outputType: String = "TEXT", // TEXT, AUDIO, EMBEDDING, UNSUPPORTED
    val description: String? = null
)"""
with open(path1, 'w') as f:
    f.write(new_entity)

# Update AppDatabase.kt
path2 = 'app/src/main/java/com/example/engine/db/AppDatabase.kt'
with open(path2, 'r') as f:
    content2 = f.read()

content2 = content2.replace('version = 8', 'version = 9')

migration = """
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_models ADD COLUMN description TEXT DEFAULT NULL")
            }
        }

        @Volatile"""
content2 = content2.replace('        @Volatile', migration)

content2 = content2.replace('addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)', 'addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)')

with open(path2, 'w') as f:
    f.write(content2)

# Fix AiManagerViewModel.kt since modelName isn't there
path3 = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path3, 'r') as f:
    content3 = f.read()

content3 = content3.replace('modelName = fileName,', '')

with open(path3, 'w') as f:
    f.write(content3)


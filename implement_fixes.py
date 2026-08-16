import re

# 1. Update ModelRatingEntity.kt
with open('app/src/main/java/com/example/engine/db/ModelRatingEntity.kt', 'r') as f:
    content = f.read()

content = content.replace('@PrimaryKey val id: String,', '@PrimaryKey val messageId: String,')

with open('app/src/main/java/com/example/engine/db/ModelRatingEntity.kt', 'w') as f:
    f.write(content)

# 2. Update AiManagerViewModel.kt
vm_path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(vm_path, 'r') as f:
    content = f.read()

replacement = """    fun rateModel(providerId: String, modelName: String, isPositive: Boolean, messageId: String) {
        viewModelScope.launch {
            modelRatingDao.insertRating(
                com.example.engine.db.ModelRatingEntity(
                    messageId = messageId,
                    modelName = modelName,
                    providerId = providerId,
                    isPositive = isPositive,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }"""
content = re.sub(r'    fun rateModel.*?timestamp = System\.currentTimeMillis\(\)\n                \)\n            \)\n        \}\n    \}', replacement, content, flags=re.DOTALL)

with open(vm_path, 'w') as f:
    f.write(content)

# 3. Update ChatScreen.kt
chat_path = 'app/src/main/java/com/example/ui/chat/ChatScreen.kt'
with open(chat_path, 'r') as f:
    content = f.read()

# Fix rateModel calls
content = content.replace('aiViewModel.rateModel(message.providerId, message.modelName, true)', 'aiViewModel.rateModel(message.providerId, message.modelName, true, message.id)')
content = content.replace('aiViewModel.rateModel(message.providerId, message.modelName, false)', 'aiViewModel.rateModel(message.providerId, message.modelName, false, message.id)')

# Fix Select Model fallback
fallback_replacement = """                                    val parts = selectedModel.split("/", limit = 2)
                                    var currentProvider = parts.getOrNull(0)
                                    var currentModel = parts.getOrNull(1) ?: selectedModel
                                    
                                    if (currentProvider == "Select Model" || currentProvider == null) {
                                        currentProvider = "google_ai_studio"
                                        currentModel = "gemini-1.5-pro-latest"
                                    }

                                    val generatingMessage = ChatMessage(text = "Thinking...", role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)"""
content = content.replace("""                                    val parts = selectedModel.split("/", limit = 2)
                                    val currentProvider = parts.getOrNull(0)
                                    val currentModel = parts.getOrNull(1) ?: selectedModel

                                    val generatingMessage = ChatMessage(text = "Thinking...", role = MessageRole.AI, modelName = currentModel, providerId = currentProvider)""", fallback_replacement)

with open(chat_path, 'w') as f:
    f.write(content)

# 4. Update AppDatabase.kt
db_path = 'app/src/main/java/com/example/engine/db/AppDatabase.kt'
with open(db_path, 'r') as f:
    content = f.read()

# Bump version to 8
content = content.replace('version = 7', 'version = 8')

# Remove destructive migration and add custom migrations
migration_imports = """import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase"""

if "import androidx.room.migration.Migration" not in content:
    content = content.replace('import androidx.sqlite.db.SupportSQLiteDatabase', migration_imports)

migrations = """
    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_models ADD COLUMN inputType TEXT NOT NULL DEFAULT 'TEXT'")
                db.execSQL("ALTER TABLE ai_models ADD COLUMN outputType TEXT NOT NULL DEFAULT 'TEXT'")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN modelName TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN providerId TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS `model_ratings` (`id` TEXT NOT NULL, `modelName` TEXT NOT NULL, `providerId` TEXT NOT NULL, `isPositive` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `model_ratings`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `model_ratings` (`messageId` TEXT NOT NULL, `modelName` TEXT NOT NULL, `providerId` TEXT NOT NULL, `isPositive` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`messageId`))")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnivian_database"
                )
                .addCallback(DatabaseCallback())
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }"""

content = re.sub(r'    companion object \{.*?    \}', migrations, content, flags=re.DOTALL)

with open(db_path, 'w') as f:
    f.write(content)

print("Fixes applied successfully.")

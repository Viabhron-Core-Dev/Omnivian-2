import re

with open('app/src/main/java/com/example/engine/db/AppDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace("entities = [ChatMessageEntity::class], version = 1", "entities = [ChatMessageEntity::class, WorkspaceConfigEntity::class], version = 2")
content = content.replace("abstract class AppDatabase : RoomDatabase() {", "abstract class AppDatabase : RoomDatabase() {\n    abstract fun workspaceConfigDao(): WorkspaceConfigDao")
content = content.replace(".build()", ".fallbackToDestructiveMigration()\n                .build()")

with open('app/src/main/java/com/example/engine/db/AppDatabase.kt', 'w') as f:
    f.write(content)

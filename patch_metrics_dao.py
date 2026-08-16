import re

with open('app/src/main/java/com/example/engine/db/AiManagerDaos.kt', 'r') as f:
    content = f.read()

new_methods = """    @Query("SELECT SUM(estimatedCost) FROM request_logs")
    fun getTotalEstimatedCost(): Flow<Double?>
    
    @Query("SELECT SUM(tokensUsed) FROM token_usage WHERE timestamp >= :since")
    fun getTokensUsedSince(since: Long): Flow<Int?>
    
    @Query("SELECT COUNT(*) FROM request_logs WHERE timestamp >= :since")
    fun getRequestCountSince(since: Long): Flow<Int>"""

content = content.replace("    @Query(\"SELECT SUM(estimatedCost) FROM request_logs\")\n    fun getTotalEstimatedCost(): Flow<Double?>", new_methods)

with open('app/src/main/java/com/example/engine/db/AiManagerDaos.kt', 'w') as f:
    f.write(content)

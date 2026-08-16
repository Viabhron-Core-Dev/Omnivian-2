import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'r') as f:
    content = f.read()

replacement = """
    private val aiModelDao = db.aiModelDao()
    private val modelRatingDao = db.modelRatingDao()
"""
content = content.replace('    private val aiModelDao = db.aiModelDao()', replacement)

replacement_2 = """
    val totalCost = metricsDao.getTotalEstimatedCost()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        
    val modelRatings = modelRatingDao.getRatingStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
"""
content = content.replace('    val totalCost = metricsDao.getTotalEstimatedCost()\n        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)', replacement_2)

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'w') as f:
    f.write(content)

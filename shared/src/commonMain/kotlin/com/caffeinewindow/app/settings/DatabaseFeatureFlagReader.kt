package com.caffeinewindow.app.settings

import com.caffeinewindow.app.db.FeatureFlagDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DatabaseFeatureFlagReader(
  private val featureFlagDao: FeatureFlagDao,
) : FeatureFlagReader {
  override suspend fun isEnabled(flag: FeatureFlag): Boolean {
    return featureFlagDao.isEnabled(flag.key) ?: false
  }

  override fun observeEnabledFlags(): Flow<Set<FeatureFlag>> {
    return featureFlagDao.observeAll().map { entities ->
      entities
        .filter { it.enabled }
        .mapNotNull { entity -> FeatureFlag.entries.find { it.key == entity.key } }
        .toSet()
    }
  }
}

package com.caffeinewindow.app.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Named feature flags. Verification and granting live in the private Pro repo.
 * The public app only reads flag values that were set elsewhere.
 */
enum class FeatureFlag(val key: String) {
  PRO_WEARABLE_SIGNAL("pro_wearable_signal"),
}

interface FeatureFlagReader {
  suspend fun isEnabled(flag: FeatureFlag): Boolean
  fun observeEnabledFlags(): Flow<Set<FeatureFlag>>
}

/**
 * Free-tier reader: all flags off. Pro repo replaces this at build time in the private package.
 */
class FreeTierFeatureFlagReader : FeatureFlagReader {
  override suspend fun isEnabled(flag: FeatureFlag): Boolean = false

  override fun observeEnabledFlags(): Flow<Set<FeatureFlag>> {
    return flowOf(emptySet())
  }
}

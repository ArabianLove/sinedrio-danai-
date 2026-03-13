package com.sinedrio.danai.senate

/**
 * Represents the owner's delegation authority over the Senate.
 *
 * Only tasks submitted through a valid [SenateDelegate] are accepted by the
 * [Senate].  This enforces that agents are "delegated 100% by the owner".
 *
 * @param ownerName  Display name of the owner who created this delegate.
 * @param token      A secret string that the Senate uses to verify authority.
 */
data class SenateDelegate(
    val ownerName: String,
    val token: String
) {
    /**
     * Returns `true` when the delegate's token is non-blank, indicating that
     * the delegation is active and the Senate should accept submitted tasks.
     */
    val isActive: Boolean
        get() = token.isNotBlank()
}

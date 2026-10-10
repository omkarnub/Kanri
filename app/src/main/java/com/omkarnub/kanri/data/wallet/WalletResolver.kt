package com.omkarnub.kanri.data.wallet

data class ResolvedWallet(
    val wallet: String,
    val transferToWallet: String? = null
)

object WalletResolver {

    const val WALLET_CASH = "CASH"
    const val WALLET_ONLINE = "ONLINE"
    const val WALLET_NONE = "NONE"

    /**
     * Centralized resolver determining the wallet and transfer_to_wallet for any transaction.
     */
    fun resolve(
        sourceType: String,
        type: String,
        userChoice: String? = null,
        atmMode: AtmWithdrawalMode = AtmWithdrawalMode.SPENDING,
        isRefund: Boolean = false
    ): ResolvedWallet {
        // 1. Explicit user choice from manual entry / edit dialogs
        if (!userChoice.isNullOrBlank()) {
            return when (userChoice.uppercase().trim()) {
                WALLET_CASH -> ResolvedWallet(wallet = WALLET_CASH, transferToWallet = null)
                WALLET_ONLINE -> ResolvedWallet(wallet = WALLET_ONLINE, transferToWallet = null)
                WALLET_NONE -> ResolvedWallet(wallet = WALLET_NONE, transferToWallet = null)
                else -> ResolvedWallet(wallet = WALLET_ONLINE, transferToWallet = null)
            }
        }

        val normalizedSource = sourceType.uppercase().trim()
        val normalizedType = type.uppercase().trim()

        // 2. Manual Move money / transfer
        if (normalizedSource == "WALLET_TRANSFER") {
            return ResolvedWallet(wallet = WALLET_ONLINE, transferToWallet = WALLET_CASH)
        }

        // 3. ATM withdrawal (DEBIT)
        if (normalizedSource == "ATM" && normalizedType == "DEBIT") {
            return if (atmMode == AtmWithdrawalMode.TRANSFER) {
                ResolvedWallet(wallet = WALLET_ONLINE, transferToWallet = WALLET_CASH)
            } else {
                ResolvedWallet(wallet = WALLET_ONLINE, transferToWallet = null)
            }
        }

        // 4. Cash manual entry
        if (normalizedSource == WALLET_CASH) {
            return ResolvedWallet(wallet = WALLET_CASH, transferToWallet = null)
        }

        // 5. Refund / reversal credit -> ONLINE
        if (isRefund) {
            return ResolvedWallet(wallet = WALLET_ONLINE, transferToWallet = null)
        }

        // 6. UPI, CARD, BANK_TRANSFER, Notification listener, unknown -> ONLINE
        return ResolvedWallet(wallet = WALLET_ONLINE, transferToWallet = null)
    }
}

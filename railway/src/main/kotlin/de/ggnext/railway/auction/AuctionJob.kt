package de.ggnext.railway.auction

import de.ggnext.common.job.Job

class AuctionJob(
    private val auctionManager: AuctionManager,
) : Job {
    override val id = "auction-job"
    override val interval = 600
    override var lastRun: Long = 0L

    override suspend fun execute() {
        auctionManager.expireAuctions()
    }
}

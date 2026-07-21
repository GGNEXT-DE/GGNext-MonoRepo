## JobManager

The `JobManager` is the central scheduler for recurring background tasks. It lives in the common module so both Velocity and Paper plugins can register and run jobs through a single shared instance.

### Jobs

A `Job` is a simple recurring task with an `id`, an `interval` (in seconds), and an `execute()` function.

**Example**
```kotlin
class AuctionJob(
    private val auctionManager: AuctionManager,
) : Job {
    override val id = "auction-job"
    override val interval = 600
    override var lastRun: Long = System.currentTimeMillis()
    override suspend fun execute() {
        auctionManager.expireAuctions()
    }
}
```

Register it via `GGNextAPI.jobManager`:
```kotlin
GGNextAPI.jobManager.registerJob(AuctionJob(auctionManager))
```

### What not to do

Jobs can be registered **at any time**, even before or after `ggnext-core` has started its scheduling loop — the `JobManager` will pick them up on the next tick regardless of order.

```kotlin
// WRONG: manually looping/delaying a task yourself instead of using the JobManager
scope.launch {
    while (isActive) {
        auctionManager.expireAuctions()
        delay(600.seconds)
    }
}
```

Doing this bypasses centralized scheduling, error handling, and logging that `JobManager` already provides — always register a `Job` instead.
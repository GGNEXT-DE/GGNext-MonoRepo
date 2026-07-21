package de.ggnext.common.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

/**
 * Central scheduler for recurring [Job]s, shared across the common module
 * so both the Velocity proxy and Paper plugins can register and run jobs
 * through a single instance.
 *
 * The internal loop is tied to [scope]'s lifecycle (`while (isActive)`), not
 * to whether any jobs are currently registered. This means [startAll] can be
 * called before any jobs exist (e.g. during core plugin init) and jobs
 * registered later by other plugins will still be picked up on the next tick.
 *
 * @param scope the coroutine scope the scheduling loop runs in; the loop
 *   stops automatically when this scope is cancelled.
 * @param logger used to report job start and failure events.
 */
class JobManager(
    private val scope: CoroutineScope,
    private val logger: Logger,
) {
    /** All currently registered jobs, keyed by [Job.id]. */
    private val jobs = ConcurrentHashMap<String, Job>()

    /**
     * Registers a job to be run by the scheduler.
     * If a job with the same [Job.id] is already registered, it will be replaced.
     */
    fun registerJob(job: Job) {
        jobs[job.id] = job
    }

    /**
     * Starts the scheduling loop.
     *
     * Every 10 seconds, checks all registered jobs and executes those whose
     * interval has elapsed since their last run. A failing job is caught and
     * logged so it does not affect other jobs or stop the loop.
     */
    fun startAll() {
        scope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                jobs.forEach { (id, job) ->
                    if (currentTime >= job.lastRun + job.interval * 1000L) {
                        logger.info("Job $id started!")
                        runCatching { job.execute() }
                            .onFailure { logger.warning("Job $id failed!") }
                        job.lastRun = currentTime
                    }
                }
                delay(10.seconds)
            }
        }
    }
}

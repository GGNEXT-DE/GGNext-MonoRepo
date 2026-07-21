package de.ggnext.common.job

/**
 * A recurring task that can be scheduled and run by [JobManager].
 */
interface Job {
    /** Unique identifier for this job. */
    val id: String

    /** How often this job should run, in seconds. */
    val interval: Int

    /** Epoch millis of the last successful run; updated by [JobManager]. */
    var lastRun: Long

    /** Executes the job's logic. */
    suspend fun execute()
}

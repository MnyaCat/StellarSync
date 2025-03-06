package dev.mnyacat.stellar_sync_common.exception

import java.sql.SQLException

class MaxRetriesExceededException(
    actionName: String,
    maxRetries: Int,
    sqlException: SQLException
) : Exception("Action '$actionName' failed after $maxRetries retries. SQLException: ${sqlException.message}")

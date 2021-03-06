package com.mangotea.rely.tasks

import com.mangotea.rely.keeps.tasks.TaskState
import com.mangotea.rely.w
import kotlinx.coroutines.*

@Deprecated("Not recommended for use", level = DeprecationLevel.WARNING)
open class SimpleTask<T, R>(
    private val taskDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private var taraget: T?,
    private val task: suspend T.() -> R
) : BaseTask<R>() {

    @Suppress("UNCHECKED_CAST")
    override fun work(dispatcher: CoroutineDispatcher): SimpleTask<T, R> {
        GlobalScope.launch(dispatcher) {
            try {
                state = TaskState.EXECUTING
                job = async(taskDispatcher) {
                    runCatching {
                        taraget?.task()
                    }
                }
                val deferred = job as Deferred<Result<R>>
                val result = if (timeMillis > 0) withTimeout(timeMillis) { deferred.await() } else deferred.await()
                onDone?.let { it(result.getOrThrow()) }
            } catch (e: Throwable) {
                when {
                    isTimeout(e) -> if (onTimeout!=null){
                        onTimeout?.let { it() }
                    }else{
                        onFailed?.let { it(e) }
                    }
                    isCancel(e) -> {
                    }
                    else -> {
                        w(e, "SimpleTask Exception")
                        onFailed?.let { it(e) }
                    }
                }
            }
        }
        return this
    }

    override fun destory() {
        taraget = null
        super.destory()
    }

}



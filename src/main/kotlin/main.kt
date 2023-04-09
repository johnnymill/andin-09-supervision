package ru.netology.supervision

import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext

fun main() {
    println("Вопросы: Cancellation")
    println("   Вопрос №1")
    Cancellation.q1()
    println("   Вопрос №2")
    Cancellation.q2()

    println()
    println("Вопросы: Exception Handling")
    println("   Вопрос №1")
    ExceptionHandling.q1()
    println("   Вопрос №2")
    ExceptionHandling.q2()
    println("   Вопрос №3")
    ExceptionHandling.q3()
    println("   Вопрос №4")
    ExceptionHandling.q4()
    println("   Вопрос №5")
    ExceptionHandling.q5()
    println("   Вопрос №6")
    ExceptionHandling.q6()
    println("   Вопрос №7")
    ExceptionHandling.q7()
}

private object Cancellation {
    fun q1() = runBlocking {
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            launch {
                delay(500)
                /*
                 * Это сообщение никогда не будет выведено, т.к. родительская корутина
                 * получает сигнал отмены раньше, чем дочерняя что-то начинает печатать.
                 * При этом родительская транслирует сигнал отмены и всем своим дочерним
                 * корутинам, т.к. они все работают в одном scope.
                 */
                println("ok") // <--
            }
            launch {
                delay(500)
                // ... и это тоже, по тем же причинам
                println("ok")
            }
        }
        delay(100)
        job.cancelAndJoin()
        println(job)
    }

    fun q2() = runBlocking {
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            val child = launch {
                delay(500)
                /*
                 * Это сообщение выведено не будет из-за вызова child.cancel()
                 * раньше, чем дело дойдет до этого момента.
                 */
                println("ok") // <--
            }
            launch {
                delay(500)
                // ... а это напечатается, т.к. cancel() соседней корутины не отменяет родительскую
                println("ok")
            }
            delay(100)
            child.cancel()
        }
        delay(100)
        job.join()
        println(job)
    }
}

private object ExceptionHandling {
    fun q1() {
        with(CoroutineScope(EmptyCoroutineContext)) {
            try {
                launch {
                    throw Exception("something bad happened")
                }
            } catch (e: Exception) {
                /*
                 * Этот код выполнен не будет, т.к. выброшенное исключение моментально отменяет
                 * саму корутину, где произошло исключение, что ведет и к отмене родительской.
                 */
                e.printStackTrace() // <--
            }
        }
        Thread.sleep(1000)
    }

    fun q2() {
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            try {
                coroutineScope {
                    throw Exception("something bad happened")
                }
            } catch (e: Exception) {
                /*
                 * Этот код будет выполнен, т.к. выброшенное исключение отменяет
                 * только корутину, где возникло исключение, и не влияет на родительскую
                 * (текущую). Потому что они работают в разных scope.
                 */
                e.printStackTrace() // <--
            }
        }
        Thread.sleep(1000)
        println(job)
    }

    fun q3() {
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            try {
                supervisorScope {
                    throw Exception("something bad happened")
                }
            } catch (e: Exception) {
                /*
                 * Этот код будет выполнен, т.к. выброшенное исключение отменяет
                 * только корутину, где возникло исключение, и не влияет на родительскую
                 * (текущую). Потому что они работают в разных scope.
                 */
                e.printStackTrace() // <--
            }
        }
        Thread.sleep(1000)
        println(job)
    }

    fun q4() {
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            var subjob1: Job? = null
            var subjob2: Job? = null
            try {
                coroutineScope {
                    subjob1 = launch {
                        delay(500)
                        /*
                         * Это исключение не будет выброшено, т.к. эта корутина будет
                         * отменена раньше из-за отмены всех корутин в этом scope.
                         */
                        throw Exception("something bad happened") // <--
                    }
                    subjob2 = launch {
                        throw Exception("something bad happened")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println(subjob1)
                println(subjob2)
            }
        }
        Thread.sleep(1000)
        println(job)
    }

    fun q5() {
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            try {
                supervisorScope {
                    launch {
                        delay(500)
                        /*
                         * Этот код выполниться, т.к. "упавшие" корутины не влияют
                         * на своих соседей в одном supervisor scope, как и на родительскую.
                         */
                        throw Exception("something bad happened") // <--
                    }
                    launch {
                        throw Exception("something bad happened")
                    }
                }
            } catch (e: Exception) {
                /*
                 * Этот код не выполнится, т.к. сгенерированные исключения не выходят
                 * за пределы supervisor scope.
                 */
                e.printStackTrace() // <--
            }
        }
        Thread.sleep(1000)
        println(job)
    }

    fun q6() {
        var subjob: Job? = null
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            subjob = CoroutineScope(EmptyCoroutineContext).launch {
                launch {
                    delay(1000)
                    /*
                     * Это сообщение не будет выведено, т.к. родительская корутина "упала"
                     * и отменила все свои дочерние корутины.
                     */
                    println("ok") // <--
                }
                launch {
                    delay(500)
                    // ... и это тоже.
                    println("ok")
                }
                throw Exception("something bad happened")
            }
            println(subjob)
        }
        Thread.sleep(1000)
        println(job)
        println(subjob)
    }

    fun q7() {
        var subjob: Job? = null
        val job = CoroutineScope(EmptyCoroutineContext).launch {
            subjob = CoroutineScope(EmptyCoroutineContext + SupervisorJob()).launch {
                launch {
                    delay(1000)
                    /*
                     * Это сообщение не будет выведено, т.к. родительская корутина "упала"
                     * и отменила все свои дочерние корутины. SupervisorJob на это не влияет,
                     * т.к. предназначено нивелировать эффект упавших дочерних корутин, но не
                     * родительской.
                     */
                    println("ok") // <--
                }
                launch {
                    delay(500)
                    // ... и это тоже.
                    println("ok")
                }
                throw Exception("something bad happened")
            }
            println(subjob)
        }
        Thread.sleep(1000)
        println(job)
        println(subjob)
    }
}

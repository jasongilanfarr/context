package org.thisamericandream.context

import org.slf4j.MDC

import scala.collection.immutable.HashMap
import java.util

case class ContextValue[+V] private (private[context] val v: Option[V]) extends AnyVal

/** Context is a specialized "Thread Local Storage" that when used along with the appropriate
  * Schedulers/Executors/etc will propagate any Contextual Information across asynchronous boundaries.
  *
  * Use of this class directly isn't ideal, instead, use specific Context classes, e.g. CancelContext which
  * should build on these primitives.
  */
object Context {
  type ContextMap = Map[ContextKey[_], ContextValue[_]]
  private val tls = new ThreadLocal[ContextMap]() {
    override def initialValue(): ContextMap =
      HashMap.empty[ContextKey[_], ContextValue[_]]
  }

  /**
    * Set a given value for a key while running the given method. Intended for specific contexts)
    */
  def withContext[R, V](k: ContextKey[V], v: V)(f: () => R): R = {
    val previous = tls.get()
    tls.set(previous + (k -> ContextValue(Some(v))))
    try {
      f()
    } finally {
      tls.set(previous)
    }
  }

  /** Get the whole map. Intended for async boundaries */
  def get(): ContextMap = tls.get()

  /** Get a specific value. Intended for specific contexts */
  def get[V](k: ContextKey[V]): Option[V] = {
    tls.get().get(k).flatMap(_.v.asInstanceOf[Option[V]])
  }

  /** Run a given method with a specific context, restoring it after. Intended for async boundaries */
  def withContext[R](ctx: ContextMap, mdc: Option[util.Map[String, String]])(f: () => R): R = {
    val old = tls.get()
    val oldMdc = Option(MDC.getCopyOfContextMap)
    tls.set(ctx)
    mdc.fold(MDC.clear())(MDC.setContextMap)
    try {
      f()
    } finally {
      tls.set(old)
      oldMdc.fold(MDC.clear())(MDC.setContextMap)
    }
  }

  /** Run a given method with no context, restoring it after */
  def clearContext[R](f: () => R): R = {
    val old = tls.get()
    val oldMdc = Option(MDC.getCopyOfContextMap)
    MDC.clear()
    tls.remove()
    try {
      f()
    } finally {
      tls.set(old)
      oldMdc.fold(MDC.clear())(MDC.setContextMap)
    }
  }
}

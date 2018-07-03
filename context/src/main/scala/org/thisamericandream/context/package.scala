package org.thisamericandream

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits

package object context {
  /** Prefer this Execution Context over the scala global context */
  implicit lazy val global: ExecutionContext = ContextPropagatingExecutionContext(Implicits.global)
}

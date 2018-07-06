package org.thisamericandream.context

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpec}

trait Spec extends WordSpec with Matchers with OptionValues with ScalaFutures
